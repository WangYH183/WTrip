package com.baguetteui.btrip

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {

    fun install(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val logFile = File(appContext.filesDir, "crash.log")
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
                val header = "\n\n===== CRASH $time (thread=${thread.name}) =====\n"
                logFile.appendText(header)
                logFile.appendText(throwable.stackTraceToString())
                logFile.appendText("\n")
            }

            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun read(context: Context): String? {
        val f = File(context.filesDir, "crash.log")
        return if (f.exists()) f.readText() else null
    }

    fun clear(context: Context) {
        val f = File(context.filesDir, "crash.log")
        if (f.exists()) f.delete()
    }
}