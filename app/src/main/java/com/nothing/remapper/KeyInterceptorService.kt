package com.nothing.remapper

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * KeyInterceptorService
 *
 * Runs as an AccessibilityService and intercepts hardware key events.
 * It catches the "unknown" keycode emitted by the Nothing Essential Button 
 * (once the default Nothing app is disabled via ADB) and executes the custom action.
 */
class KeyInterceptorService : AccessibilityService() {

    companion object {
        @Volatile var isRunning = false
        private const val TAG = "KeyInterceptor"
    }

    private var torchEnabled = false
    private lateinit var prefs: SharedPreferences
    private var lastTrigger = 0L

    // ──────────────────────────────────────────────────────────────────────────
    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        Log.d(TAG, "Accessibility Service Connected")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    // ──────────────────────────────────────────────────────────────────────────
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used, but required to override
    }

    override fun onInterrupt() {
        // Not used, but required to override
    }

    // ──────────────────────────────────────────────────────────────────────────
    override fun onKeyEvent(event: KeyEvent): Boolean {
        // We only care about DOWN events to trigger actions
        if (event.action != KeyEvent.ACTION_DOWN) {
            // Still need to consume the UP event if it's our key, otherwise let it pass
            val customKey = prefs.getInt(MainActivity.KEY_KEYCODE, 0)
            return (event.keyCode == customKey)
        }

        val keyCode = event.keyCode
        Log.d(TAG, "Key detected: $keyCode")

        val customKey = prefs.getInt(MainActivity.KEY_KEYCODE, 0)
        
        // If a custom key is set and matches, intercept it
        if (keyCode == customKey) {
            
            // Debounce: ignore rapid repeat events within 500 ms
            val now = System.currentTimeMillis()
            if (now - lastTrigger > 500) {
                lastTrigger = now
                executeAction()
            }
            return true // We consumed this event!
        }

        return super.onKeyEvent(event)
    }



    // ──────────────────────────────────────────────────────────────────────────
    private fun executeAction() {
        when (prefs.getString(MainActivity.KEY_ACTION, MainActivity.ACTION_FLASHLIGHT)) {
            MainActivity.ACTION_FLASHLIGHT -> toggleFlashlight()
            MainActivity.ACTION_CAMERA     -> openCamera()
            MainActivity.ACTION_SCREENSHOT -> takeScreenshot()
            MainActivity.ACTION_CYCLE_RINGER -> cycleRingerMode()
            MainActivity.ACTION_CALL       -> openDialer()
            MainActivity.ACTION_NOTHING    -> { /* intentionally empty */ }
        }

        // Haptic feedback if enabled
        if (prefs.getBoolean(MainActivity.KEY_VIBRATE, true)) {
            vibrate(50)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    private fun toggleFlashlight() {
        try {
            val cm       = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cm.cameraIdList.firstOrNull() ?: return
            torchEnabled = !torchEnabled
            cm.setTorchMode(cameraId, torchEnabled)
        } catch (e: Exception) {
            Log.e(TAG, "Flashlight error: ${e.message}")
        }
    }

    private fun openCamera() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        runCatching { startActivity(intent) }
    }

    private fun takeScreenshot() {
        // In an AccessibilityService, we can take a screenshot natively!
        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
    }

    private fun cycleRingerMode() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val currentMode = audioManager.ringerMode
            
            val nextMode = when (currentMode) {
                android.media.AudioManager.RINGER_MODE_NORMAL -> android.media.AudioManager.RINGER_MODE_VIBRATE
                android.media.AudioManager.RINGER_MODE_VIBRATE -> android.media.AudioManager.RINGER_MODE_SILENT
                else -> android.media.AudioManager.RINGER_MODE_NORMAL
            }
            audioManager.ringerMode = nextMode
        } catch (e: Exception) {
            Log.e(TAG, "Audio manager error: ${e.message}")
        }
    }

    private fun openDialer() {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        runCatching { startActivity(intent) }
    }

    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibrate error: ${e.message}")
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    override fun onUnbind(intent: Intent?): Boolean {
        isRunning = false
        return super.onUnbind(intent)
    }
}
