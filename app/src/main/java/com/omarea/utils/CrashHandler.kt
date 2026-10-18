package com.omarea.utils

import android.content.Context
import java.io.File

class CrashHandler : Thread.UncaughtExceptionHandler {
    private lateinit var logFile: File
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        logFile = File(context.filesDir, "crash/error.log")
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, error: Throwable) {
        try {
            CrashLogWriter.write(logFile, thread, error)
        } catch (_: Throwable) {
            // Logging must never prevent Android's normal crash handling.
        } finally {
            val handler = defaultHandler
            if (handler != null) {
                handler.uncaughtException(thread, error)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
    }
}
