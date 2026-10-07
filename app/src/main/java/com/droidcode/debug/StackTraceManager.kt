package com.droidcode.debug

import android.content.Context
import android.os.Build
import android.util.Log
import com.droidcode.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diagnostic utility for capturing, persisting, and viewing app stack traces
 * and system debug reports.
 */
object StackTraceManager {
    private const val TAG = "StackTraceManager"
    private const val DEBUG_DIR = "debug_logs"
    private const val LATEST_FILE = "latest_stacktrace.txt"
    private const val HISTORY_FILE = "stacktrace_history.log"
    private const val MAX_SAVED_TRACES = 25

    private var appContext: Context? = null
    private val inMemoryTraces = mutableListOf<String>()
    private var defaultUncaughtHandler: Thread.UncaughtExceptionHandler? = null
    private var isInitialized = false

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        appContext = context.applicationContext
        isInitialized = true

        loadPersistedTraces()
        installUncaughtExceptionHandler()
    }

    private fun installUncaughtExceptionHandler() {
        if (defaultUncaughtHandler != null) return
        defaultUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                recordException(
                    throwable = throwable,
                    tag = "UNCAUGHT_CRASH",
                    customMessage = "Fatal crash on thread: ${thread.name} (ID ${thread.id})",
                    isCrash = true
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record uncaught crash", e)
            } finally {
                defaultUncaughtHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    /**
     * Records an exception into the diagnostic log and persists it to disk.
     */
    @Synchronized
    fun recordException(
        throwable: Throwable,
        tag: String = "ERROR",
        customMessage: String? = null,
        isCrash: Boolean = false
    ): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTraceString = sw.toString().trim()

        val report = buildString {
            appendLine("================================================================================")
            appendLine("DROIDCODE DIAGNOSTIC STACK TRACE")
            appendLine("================================================================================")
            appendLine("Timestamp   : $timestamp")
            appendLine("Severity    : ${if (isCrash) "CRITICAL (Crash)" else "ERROR (Caught)"}")
            appendLine("Tag         : $tag")
            if (!customMessage.isNullOrBlank()) {
                appendLine("Message     : $customMessage")
            }
            appendLine("Thread      : ${Thread.currentThread().name} (ID: ${Thread.currentThread().id})")
            appendLine()
            appendLine("--- APPLICATION INFORMATION ---")
            appendLine("Package     : ${BuildConfig.APPLICATION_ID}")
            appendLine("Version     : ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
            appendLine("Build Type  : ${BuildConfig.BUILD_TYPE}")
            appendLine()
            appendLine("--- DEVICE & ENVIRONMENT ---")
            appendLine("Brand/Model : ${Build.BRAND} ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Android OS  : Android ${Build.VERSION.RELEASE} (API Level ${Build.VERSION.SDK_INT})")
            appendLine("ABIs        : ${Build.SUPPORTED_ABIS.joinToString(", ")}")
            appendLine("Fingerprint : ${Build.FINGERPRINT}")
            appendLine()
            appendLine("--- MEMORY STATUS ---")
            val runtime = Runtime.getRuntime()
            val maxMemoryMb = runtime.maxMemory() / (1024 * 1024)
            val totalMemoryMb = runtime.totalMemory() / (1024 * 1024)
            val freeMemoryMb = runtime.freeMemory() / (1024 * 1024)
            val usedMemoryMb = totalMemoryMb - freeMemoryMb
            appendLine("Heap Usage  : ${usedMemoryMb}MB used / ${totalMemoryMb}MB allocated (Max: ${maxMemoryMb}MB)")
            appendLine()
            appendLine("--- EXCEPTION SUMMARY ---")
            appendLine("Exception   : ${throwable.javaClass.name}")
            appendLine("Cause       : ${throwable.localizedMessage ?: "No message provided"}")
            appendLine()
            appendLine("--- STACK TRACE ---")
            appendLine(stackTraceString)
            appendLine("================================================================================")
        }

        inMemoryTraces.add(0, report)
        if (inMemoryTraces.size > MAX_SAVED_TRACES) {
            inMemoryTraces.removeAt(inMemoryTraces.lastIndex)
        }

        persistTrace(report)
        return report
    }

    /**
     * Manually triggers and records a sample test exception for debugging and verification.
     */
    fun recordTestException(): String {
        val testException = IllegalStateException("Manual diagnostic test stack trace generated from Settings.")
        return recordException(
            throwable = testException,
            tag = "MANUAL_TEST",
            customMessage = "User generated test diagnostic stack trace from DroidCode Settings"
        )
    }

    /**
     * Returns true if one or more stack traces have been recorded.
     */
    @Synchronized
    fun hasStackTraces(): Boolean = inMemoryTraces.isNotEmpty()

    /**
     * Returns the latest recorded stack trace, or a fresh system diagnostics snapshot if none recorded.
     */
    @Synchronized
    fun getLatestStackTrace(): String {
        return inMemoryTraces.firstOrNull() ?: getSystemDiagnostics()
    }

    /**
     * Returns all recorded stack traces in descending chronological order.
     */
    @Synchronized
    fun getAllStackTraces(): List<String> {
        return inMemoryTraces.toList()
    }

    /**
     * Generates a comprehensive device and application diagnostic snapshot.
     */
    fun getSystemDiagnostics(): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val runtime = Runtime.getRuntime()
        val maxMemoryMb = runtime.maxMemory() / (1024 * 1024)
        val totalMemoryMb = runtime.totalMemory() / (1024 * 1024)
        val freeMemoryMb = runtime.freeMemory() / (1024 * 1024)
        val usedMemoryMb = totalMemoryMb - freeMemoryMb

        return buildString {
            appendLine("================================================================================")
            appendLine("DROIDCODE SYSTEM DIAGNOSTICS REPORT")
            appendLine("================================================================================")
            appendLine("Timestamp   : $timestamp")
            appendLine("Status      : No crashes or unhandled exceptions recorded.")
            appendLine()
            appendLine("--- APPLICATION INFORMATION ---")
            appendLine("Package     : ${BuildConfig.APPLICATION_ID}")
            appendLine("Version     : ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
            appendLine("Build Type  : ${BuildConfig.BUILD_TYPE}")
            appendLine()
            appendLine("--- DEVICE HARDWARE & OS ---")
            appendLine("Brand/Model : ${Build.BRAND} ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Android OS  : Android ${Build.VERSION.RELEASE} (API Level ${Build.VERSION.SDK_INT})")
            appendLine("Hardware    : ${Build.HARDWARE} (${Build.BOARD})")
            appendLine("Processors  : ${runtime.availableProcessors()} cores")
            appendLine("ABIs        : ${Build.SUPPORTED_ABIS.joinToString(", ")}")
            appendLine("Fingerprint : ${Build.FINGERPRINT}")
            appendLine()
            appendLine("--- MEMORY & RUNTIME ---")
            appendLine("Heap Usage  : ${usedMemoryMb}MB used / ${totalMemoryMb}MB allocated (Max: ${maxMemoryMb}MB)")
            appendLine("Active Threads: ${Thread.activeCount()}")
            appendLine()
            appendLine("--- ENVIRONMENT ---")
            appendLine("Locale      : ${Locale.getDefault().toLanguageTag()}")
            appendLine("Timezone    : ${java.util.TimeZone.getDefault().id}")
            appendLine("================================================================================")
        }
    }

    /**
     * Clears all recorded stack traces in memory and on disk.
     */
    @Synchronized
    fun clearStackTraces() {
        inMemoryTraces.clear()
        try {
            val dir = getLogsDir()
            if (dir.exists()) {
                dir.deleteRecursively()
                dir.mkdirs()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed clearing persisted stack traces", e)
        }
    }

    private fun getLogsDir(): File {
        val root = appContext?.filesDir ?: File(System.getProperty("java.io.tmpdir", "."))
        val dir = File(root, DEBUG_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun persistTrace(report: String) {
        try {
            val dir = getLogsDir()
            val latestFile = File(dir, LATEST_FILE)
            latestFile.writeText(report)

            val historyFile = File(dir, HISTORY_FILE)
            historyFile.appendText(report + "\n\n")
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing stack trace to disk", e)
        }
    }

    private fun loadPersistedTraces() {
        try {
            val dir = getLogsDir()
            val latestFile = File(dir, LATEST_FILE)
            if (latestFile.exists() && latestFile.length() > 0) {
                val latest = latestFile.readText()
                if (latest.isNotBlank()) {
                    inMemoryTraces.add(latest)
                }
            }

            val historyFile = File(dir, HISTORY_FILE)
            if (historyFile.exists() && historyFile.length() > 0) {
                val history = historyFile.readText()
                val entries = history.split("================================================================================\n================================================================================")
                for (entry in entries) {
                    val trimmed = entry.trim()
                    if (trimmed.isNotBlank() && !inMemoryTraces.contains(trimmed)) {
                        inMemoryTraces.add(trimmed)
                        if (inMemoryTraces.size >= MAX_SAVED_TRACES) break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed loading persisted stack traces", e)
        }
    }
}
