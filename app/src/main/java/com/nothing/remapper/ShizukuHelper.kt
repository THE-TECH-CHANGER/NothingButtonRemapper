package com.nothing.remapper

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Helper class for all Shizuku-related operations.
 *
 * Handles checking availability, managing permissions, and executing
 * privileged shell commands (like pm disable-user) through the Shizuku service.
 */
object ShizukuHelper {

    private const val TAG = "ShizukuHelper"

    const val PACKAGE_ESSENTIAL_SPACE = "com.nothing.ntessentialspace"
    const val PACKAGE_ESSENTIAL_RECORDER = "com.nothing.ntessentialrecorder"
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    // ──────────────────────────────────────────────────────────────────────────
    // Availability & Permission
    // ──────────────────────────────────────────────────────────────────────────

    /** Returns true if the Shizuku service is currently running and reachable. */
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            Log.w(TAG, "pingBinder failed: ${e.message}")
            false
        }
    }

    /** Returns true if Shizuku permission has already been granted to this app. */
    fun isPermissionGranted(): Boolean {
        return try {
            if (!isShizukuAvailable()) return false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            Log.w(TAG, "checkSelfPermission failed: ${e.message}")
            false
        }
    }

    /** Request Shizuku permission from the user. */
    fun requestPermission(requestCode: Int) {
        try {
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
            Log.e(TAG, "requestPermission failed: ${e.message}")
        }
    }

    /** Check if the Shizuku app is installed on this device. */
    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Shell Command Execution
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Execute a shell command via Shizuku with elevated privileges.
     * Must be called from a coroutine — runs on Dispatchers.IO.
     *
     * @return Result containing stdout on success, or the exception on failure.
     */
    suspend fun executeCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val parts = command.split(" ").filter { it.isNotEmpty() }.toTypedArray()
            val process = Shizuku.newProcess(parts, null, null)

            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()

            val exitCode = process.waitFor()
            Log.d(TAG, "Command: $command | exit=$exitCode | out=$stdout | err=$stderr")

            if (exitCode == 0) {
                Result.success(stdout.trim())
            } else {
                Result.failure(Exception("Exit code $exitCode: ${stderr.ifBlank { stdout }}".trim()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "executeCommand failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Disable both Nothing Essential Space and Nothing Essential Recorder packages.
     * This frees up the hardware key so our AccessibilityService can intercept it.
     */
    suspend fun disableNothingPackages(): Result<String> {
        val result1 = executeCommand("pm disable-user --user 0 $PACKAGE_ESSENTIAL_SPACE")
        val result2 = executeCommand("pm disable-user --user 0 $PACKAGE_ESSENTIAL_RECORDER")

        // If both succeed, report success
        return if (result1.isSuccess && result2.isSuccess) {
            Result.success("Both packages disabled successfully!")
        } else {
            // Collect error messages
            val errors = buildString {
                result1.exceptionOrNull()?.let { append("Essential Space: ${it.message}\n") }
                result2.exceptionOrNull()?.let { append("Essential Recorder: ${it.message}") }
            }
            // If at least one succeeded, still consider partial success
            if (result1.isSuccess || result2.isSuccess) {
                Result.success("Partially done. $errors")
            } else {
                Result.failure(Exception(errors.ifBlank { "Failed to disable packages" }))
            }
        }
    }

    /**
     * Re-enable the Nothing Essential packages (restore default behavior).
     */
    suspend fun enableNothingPackages(): Result<String> {
        val result1 = executeCommand("pm enable --user 0 $PACKAGE_ESSENTIAL_SPACE")
        val result2 = executeCommand("pm enable --user 0 $PACKAGE_ESSENTIAL_RECORDER")

        return if (result1.isSuccess && result2.isSuccess) {
            Result.success("Default Nothing behavior restored!")
        } else {
            val errors = buildString {
                result1.exceptionOrNull()?.let { append("Essential Space: ${it.message}\n") }
                result2.exceptionOrNull()?.let { append("Essential Recorder: ${it.message}") }
            }
            Result.failure(Exception(errors.ifBlank { "Failed to re-enable packages" }))
        }
    }
}
