package com.lastgimbus.the.freebuddy.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "settings")

/** A physical headset set. Case serial is stable; MAC is used until MBB 1/7 arrives. */
data class HeadphonesIdentity(
    val protocolId: String,
    val address: String? = null,
    val caseSerial: String? = null,
)

/** User-selected appearance settings, persisted via DataStore Preferences. */
object ThemeStore {
    enum class ThemeMode { SYSTEM, LIGHT, DARK }

    private val KEY = stringPreferencesKey("theme_mode")
    private val SHOW_EMOJIS = booleanPreferencesKey("show_emojis")

    // Legacy global keys (pre-0.3.4). Kept only as migration sources for 5i.
    private val LEGACY_FORCE_HEADPHONES_COLOR = booleanPreferencesKey("force_headphones_color")
    private val LEGACY_HEADPHONES_COLOR = stringPreferencesKey("headphones_color")

    private val LAST_PROTOCOL = stringPreferencesKey("last_headphones_protocol")
    private val LAST_ADDRESS = stringPreferencesKey("last_headphones_address")
    private val LAST_SERIAL = stringPreferencesKey("last_headphones_case_serial")

    fun themeMode(context: Context): Flow<ThemeMode> =
        context.themeDataStore.data.map { prefs ->
            runCatching { ThemeMode.valueOf(prefs[KEY] ?: "") }.getOrDefault(ThemeMode.SYSTEM)
        }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.themeDataStore.edit { it[KEY] = mode.name }
    }

    fun showEmojis(context: Context): Flow<Boolean> =
        context.themeDataStore.data.map { prefs -> prefs[SHOW_EMOJIS] ?: true }

    suspend fun setShowEmojis(context: Context, show: Boolean) {
        context.themeDataStore.edit { it[SHOW_EMOJIS] = show }
    }

    /** Effective per-device setting: serial -> remembered MAC mapping -> MAC -> model -> legacy. */
    fun forceHeadphonesColor(context: Context, identity: HeadphonesIdentity): Flow<Boolean> =
        context.themeDataStore.data.map { prefs -> resolveForce(prefs, identity) }

    fun headphonesColor(context: Context, identity: HeadphonesIdentity): Flow<String> =
        context.themeDataStore.data.map { prefs -> resolveColor(prefs, identity) }

    suspend fun setForceHeadphonesColor(
        context: Context,
        identity: HeadphonesIdentity,
        force: Boolean,
    ) {
        context.themeDataStore.edit { prefs ->
            prefs[booleanPreferencesKey(forceColorPreferenceName(resolveWriteScope(prefs, identity)))] = force
            rememberLast(prefs, identity)
        }
    }

    suspend fun setHeadphonesColor(
        context: Context,
        identity: HeadphonesIdentity,
        color: String,
    ) {
        context.themeDataStore.edit { prefs ->
            prefs[stringPreferencesKey(headphonesColorPreferenceName(resolveWriteScope(prefs, identity)))] = color
            rememberLast(prefs, identity)
        }
    }

    /**
     * Saves MAC -> case serial and atomically migrates the effective MAC/model value to serial.
     * Calling this repeatedly is harmless and never overwrites an explicit serial setting.
     */
    suspend fun registerHeadphones(context: Context, identity: HeadphonesIdentity) {
        context.themeDataStore.edit { prefs ->
            val forceBefore = resolveForceOrNull(prefs, identity)
            val colorBefore = resolveColorOrNull(prefs, identity)
            val serial = identity.caseSerial.cleanId()
            val address = identity.address.cleanId()
            if (serial != null && address != null) {
                prefs[stringPreferencesKey(macSerialPreferenceName(identity.protocolId, address))] = serial
                val serialScope = serialScope(identity.protocolId, serial)
                val forceKey = booleanPreferencesKey(forceColorPreferenceName(serialScope))
                val colorKey = stringPreferencesKey(headphonesColorPreferenceName(serialScope))
                if (prefs[forceKey] == null && forceBefore != null) prefs[forceKey] = forceBefore
                if (prefs[colorKey] == null && colorBefore != null) prefs[colorKey] = colorBefore
            }
            rememberLast(prefs, identity)
        }
    }

    fun lastHeadphonesIdentity(context: Context): Flow<HeadphonesIdentity> =
        context.themeDataStore.data.map { prefs ->
            HeadphonesIdentity(
                protocolId = prefs[LAST_PROTOCOL] ?: "freebuds_5i",
                address = prefs[LAST_ADDRESS],
                caseSerial = prefs[LAST_SERIAL],
            )
        }

    private fun rememberLast(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        identity: HeadphonesIdentity,
    ) {
        prefs[LAST_PROTOCOL] = identity.protocolId
        val address = identity.address.cleanId()
        if (address != null) prefs[LAST_ADDRESS] = address else prefs.remove(LAST_ADDRESS)
        val serial = identity.caseSerial.cleanId() ?: address?.let {
            prefs[stringPreferencesKey(macSerialPreferenceName(identity.protocolId, it))]
        }
        if (serial != null) prefs[LAST_SERIAL] = serial else prefs.remove(LAST_SERIAL)
    }

    private fun resolveForce(prefs: Preferences, identity: HeadphonesIdentity): Boolean =
        resolveForceOrNull(prefs, identity) ?: false

    private fun resolveColor(prefs: Preferences, identity: HeadphonesIdentity): String =
        resolveColorOrNull(prefs, identity) ?: "BLACK"

    private fun resolveForceOrNull(prefs: Preferences, identity: HeadphonesIdentity): Boolean? {
        preferenceScopes(prefs, identity).forEach { scope ->
            prefs[booleanPreferencesKey(forceColorPreferenceName(scope))]?.let { return it }
        }
        return prefs[LEGACY_FORCE_HEADPHONES_COLOR].takeIf { identity.protocolId == "freebuds_5i" }
    }

    private fun resolveColorOrNull(prefs: Preferences, identity: HeadphonesIdentity): String? {
        preferenceScopes(prefs, identity).forEach { scope ->
            prefs[stringPreferencesKey(headphonesColorPreferenceName(scope))]?.let { return it }
        }
        return prefs[LEGACY_HEADPHONES_COLOR].takeIf { identity.protocolId == "freebuds_5i" }
    }

    private fun preferenceScopes(prefs: Preferences, identity: HeadphonesIdentity): List<String> = buildList {
        val address = identity.address.cleanId()
        val directSerial = identity.caseSerial.cleanId()
        val mappedSerial = address?.let {
            prefs[stringPreferencesKey(macSerialPreferenceName(identity.protocolId, it))]
        }
        (directSerial ?: mappedSerial)?.let { add(serialScope(identity.protocolId, it)) }
        address?.let { add(macScope(identity.protocolId, it)) }
        // Previous model-scoped implementation used the raw protocol id as scope.
        add(identity.protocolId.preferenceKeyPart())
        add(protocolScope(identity.protocolId))
    }.distinct()

    private fun resolveWriteScope(prefs: Preferences, identity: HeadphonesIdentity): String {
        val serial = identity.caseSerial.cleanId()
            ?: identity.address.cleanId()?.let {
                prefs[stringPreferencesKey(macSerialPreferenceName(identity.protocolId, it))]
            }
        return when {
            serial != null -> serialScope(identity.protocolId, serial)
            identity.address.cleanId() != null -> macScope(identity.protocolId, identity.address.cleanId()!!)
            else -> protocolScope(identity.protocolId)
        }
    }
}

internal fun protocolScope(protocolId: String): String =
    "${protocolId.preferenceKeyPart()}_model"

internal fun macScope(protocolId: String, address: String): String =
    "${protocolId.preferenceKeyPart()}_mac_${address.preferenceKeyPart()}"

internal fun serialScope(protocolId: String, serial: String): String =
    "${protocolId.preferenceKeyPart()}_serial_${serial.preferenceKeyPart()}"

internal fun forceColorPreferenceName(scope: String): String =
    "force_headphones_color_$scope"

internal fun headphonesColorPreferenceName(scope: String): String =
    "headphones_color_$scope"

internal fun macSerialPreferenceName(protocolId: String, address: String): String =
    "headphones_serial_${protocolId.preferenceKeyPart()}_${address.preferenceKeyPart()}"

private fun String?.cleanId(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

private fun String.preferenceKeyPart(): String =
    lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_')
