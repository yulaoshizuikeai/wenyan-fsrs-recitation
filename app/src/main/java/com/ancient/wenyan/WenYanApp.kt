package com.ancient.wenyan

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Classical Chinese Recitation Application Entry.
 * Initializes core resources, offline Room database, and global application state.
 */
@HiltAndroidApp
class WenYanApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("WenYanCrash", "Uncaught exception on thread ${thread.name}", throwable)
            try {
                val crashFile = java.io.File(filesDir, "crash.log")
                crashFile.writeText("Time: ${System.currentTimeMillis()}\nThread: ${thread.name}\n${throwable.stackTraceToString()}")
            } catch (_: Throwable) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        lateinit var instance: WenYanApp
            private set
    }
}
