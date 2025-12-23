package com.baguetteui.btrip

import android.app.Application

class BTripApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}