package io.github.razziell.freebuddynext

import android.app.Application
import io.github.razziell.freebuddynext.bluetooth.HeadphonesRepository

class FreeBuddyApplication : Application() {
    val headphonesRepository by lazy { HeadphonesRepository(this) }
    override fun onCreate() {
        super.onCreate()
        headphonesRepository.start()
    }
}
