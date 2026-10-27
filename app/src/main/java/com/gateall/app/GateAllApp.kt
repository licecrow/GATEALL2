package com.gateall.app

import android.app.Application

class GateAllApp : Application() {

    companion object {
        lateinit var instance: GateAllApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
