package io.github.razziell.freebuddynext.companion

import android.Manifest
import android.companion.CompanionDeviceManager
import android.companion.ObservingDevicePresenceRequest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import io.github.razziell.freebuddynext.bluetooth.BackgroundDiagnostics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class CompanionLink(val address: String, val id: Int?)

/** Consent lives in CDM; the preference records only the user's observation choice. */
internal object CompanionAccess {
    private val mutex = Mutex()
    private const val PREFS = "companion_presence"
    private const val ENABLED = "enabled_addresses"

    fun supported(context: Context): Boolean = Build.VERSION.SDK_INT >= 31 &&
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP) &&
        context.getSystemService(CompanionDeviceManager::class.java) != null

    fun hasPermission(context: Context): Boolean = Build.VERSION.SDK_INT < 31 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    fun enabled(context: Context): Set<String> = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getStringSet(ENABLED, emptySet()).orEmpty().mapNotNull(::companionAddress).toSet()

    private fun save(context: Context, addresses: Set<String>) {
        check(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(ENABLED, addresses.toSet()).commit()) { "Could not save companion settings" }
    }

    @Suppress("DEPRECATION")
    @RequiresApi(31)
    fun links(context: Context): List<CompanionLink> {
        val manager = context.getSystemService(CompanionDeviceManager::class.java)
        return if (Build.VERSION.SDK_INT >= 33) manager.myAssociations.mapNotNull {
            companionAddress(it.deviceMacAddress?.toString())?.let { address -> CompanionLink(address, it.id) }
        } else manager.associations.mapNotNull { companionAddress(it)?.let { address -> CompanionLink(address, null) } }
    }

    @Suppress("DEPRECATION")
    @RequiresApi(31)
    private fun observe(context: Context, link: CompanionLink) {
        val manager = context.getSystemService(CompanionDeviceManager::class.java)
        if (Build.VERSION.SDK_INT >= 36 && link.id != null) {
            manager.startObservingDevicePresence(ObservingDevicePresenceRequest.Builder()
                .setAssociationId(link.id).build())
        } else manager.startObservingDevicePresence(link.address)
    }

    suspend fun enable(context: Context, address: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(supported(context) && hasPermission(context))
            if (Build.VERSION.SDK_INT >= 31) {
                val key = requireNotNull(companionAddress(address))
                val link = links(context).firstOrNull { it.address == key }
                    ?: error("System association is missing")
                // Persist intent first: if the process exits between these operations,
                // restore() retries observing an already consented system association.
                save(context, enabled(context) + key)
                observe(context, link)
                BackgroundDiagnostics.record(context, "companion observing api=${Build.VERSION.SDK_INT}")
            }
        }
    }

    @Suppress("DEPRECATION")
    suspend fun remove(context: Context, address: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(supported(context))
            if (Build.VERSION.SDK_INT >= 31) {
                val key = requireNotNull(companionAddress(address))
                val manager = context.getSystemService(CompanionDeviceManager::class.java)
                // Disassociation also removes the system presence subscription. Do not
                // unpair Bluetooth or disconnect the UI's shared RFCOMM connection.
                links(context).filter { it.address == key }.forEach { link ->
                    if (Build.VERSION.SDK_INT >= 33 && link.id != null) manager.disassociate(link.id)
                    else manager.disassociate(link.address)
                }
                save(context, enabled(context) - key)
                BackgroundDiagnostics.record(context, "companion association removed")
            }
        }
    }

    suspend fun restore(context: Context) = withContext(Dispatchers.IO) {
        if (!supported(context) || !hasPermission(context) || Build.VERSION.SDK_INT < 31) return@withContext
        mutex.withLock {
            try {
                val desired = enabled(context)
                if (desired.isEmpty()) return@withLock
                val links = links(context)
                val valid = desired.intersect(links.map { it.address }.toSet())
                if (valid != desired) save(context, valid)
                links.filter { it.address in valid }.forEach { link ->
                    try {
                        observe(context, link)
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (error: Exception) {
                        BackgroundDiagnostics.record(context, "companion restore error=${error.javaClass.simpleName}")
                    }
                }
                BackgroundDiagnostics.record(context, "companion restore count=${valid.size}")
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                BackgroundDiagnostics.record(context, "companion restore error=${error.javaClass.simpleName}")
            }
        }
    }
}
