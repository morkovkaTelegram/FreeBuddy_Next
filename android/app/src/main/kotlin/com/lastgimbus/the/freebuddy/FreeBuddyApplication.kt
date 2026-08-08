package com.lastgimbus.the.freebuddy

import android.app.Application
import com.lastgimbus.the.freebuddy.bluetooth.HeadphonesRepository

class FreeBuddyApplication : Application() {
    val headphonesRepository by lazy { HeadphonesRepository(this) }
    override fun onCreate() {
        super.onCreate()
        headphonesRepository.start()
    }
}
