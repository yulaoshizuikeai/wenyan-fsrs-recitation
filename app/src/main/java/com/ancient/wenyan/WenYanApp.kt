package com.ancient.wenyan

import android.app.Application

/**
 * Classical Chinese Recitation Application Entry.
 * Initializes core resources, offline Room database, and global application state.
 */
class WenYanApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: WenYanApp
            private set
    }
}
