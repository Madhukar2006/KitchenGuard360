package com.kitchenguard360.app

import android.app.Application

class KitchenGuardApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: KitchenGuardApplication
            private set
    }
}
