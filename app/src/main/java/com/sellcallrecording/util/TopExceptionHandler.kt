package com.sellcallrecording.util

import android.content.Context
import android.os.Build
import android.os.Environment
import com.sellcallrecording.R
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date

class TopExceptionHandler(private val localPath: String?, private val context: Context) :
    Thread.UncaughtExceptionHandler {
    private val defaultUEH: Thread.UncaughtExceptionHandler =
        Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        val stringBuffSync: Writer = StringWriter()
        val printWriter = PrintWriter(stringBuffSync)
        e.printStackTrace(printWriter)
        val stacktrace = stringBuffSync.toString()
        printWriter.close()

        if (localPath != null) {
            writeToFile(stacktrace)
        }

        defaultUEH.uncaughtException(t, e)
    }

    private fun writeToFile(currentStacktrace: String) {
        try {
            val dateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
            val date = Date()
            val filename = "/" + dateFormat.format(date) + ".txt"

            val directory: File
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val path =
                    Environment.DIRECTORY_DOCUMENTS + File.separator + context.getString(R.string.app_name)
                directory = Environment.getExternalStoragePublicDirectory(path)
            } else {
                directory = File(
                    Environment.getExternalStorageDirectory(),
                    context.getString(R.string.app_name)
                )
            }

            if (!directory.exists()) {
                directory.mkdirs()
            }

            val file = File(directory, filename)
            if (directory.exists()) {
                file.createNewFile()
            }

            if (file.exists()) {
                val fileWriter = FileWriter(file)
                fileWriter.append(currentStacktrace)
                fileWriter.flush()
                fileWriter.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}