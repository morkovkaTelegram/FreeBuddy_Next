package io.github.razziell.freebuddynext

import android.app.Application
import io.github.razziell.freebuddynext.companion.CompanionAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import io.github.razziell.freebuddynext.bluetooth.HeadphonesRepository

class FreeBuddyApplication : Application() {
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val headphonesRepository by lazy { HeadphonesRepository(this) }
    override fun onCreate() {
        super.onCreate()
        headphonesRepository.start()
        backgroundScope.launch {
            io.github.razziell.freebuddynext.bluetooth.BackgroundDiagnostics.recordProcessStart(this@FreeBuddyApplication)
            CompanionAccess.restore(this@FreeBuddyApplication)
        }
    }
}
