package com.phoenix.phnx

import android.app.Application

class PhnxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PhnxPreferences.applyTheme(this)
    }
}
