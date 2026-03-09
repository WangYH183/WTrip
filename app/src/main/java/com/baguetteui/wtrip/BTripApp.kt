package com.baguetteui.wtrip

import android.app.Application

class WTripApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}