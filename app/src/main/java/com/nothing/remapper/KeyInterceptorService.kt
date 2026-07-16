package com.nothing.remapper

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * KeyInterceptorService
 *
 * Runs as an AccessibilityService and intercepts hardware key events.
 * Implements a gesture detection state machine that recognizes:
 *   - Single press, Double press, Triple press, Long press
 *
 * Context-aware: detects if a camera app is in the foreground and
 * uses camera-specific action overrides when configured.
 */
class KeyInterceptorService : AccessibilityService() {

    companion object {
        @Volatile var isRunning = false
        private const val TAG = "KeyInterceptor"

        // Timing constants
        private const val MULTI_PRESS_WINDOW_MS = 350L
        private const val LONG_PRESS_THRESHOLD_MS = 500L

        // Gesture keys for preferences
        const val GESTURE_SINGLE = "single_press"
        const val GESTURE_DOUBLE = "double_press"
        const val GESTURE_TRIPLE = "triple_press"
        const val GESTURE_LONG   = "long_press"

        // Action constants
        const val ACTION_FLASHLIGHT    = "flashlight"
        const val ACTION_CAMERA        = "camera"
        const val ACTION_SHUTTER       = "shutter"
        const val ACTION_SCREENSHOT    = "screenshot"
        const val ACTION_CYCLE_RINGER  = "cycle_ringer"
        const val ACTION_DIALER        = "dialer"
        const val ACTION_NOTHING       = "nothing"
        const val ACTION_DEFAULT       = "default"

        // Preference key pattern: "action_{gesture}" and "action_{gesture}_camera"
        fun actionKey(gesture: String) = "action_$gesture"
        fun cameraActionKey(gesture: String) = "action_${gesture}_camera"

        // Known camera package names
        private val CAMERA_PACKAGES = setOf(
            "com.nothing.camera",
            "com.google.android.GoogleCamera",
            "com.android.camera",
            "com.android.camera2",
            "com.sec.android.app.camera",
            "com.huawei.camera",
            "com.oneplus.camera",
            "com.oppo.camera",
            "org.codeaurora.snapcam",
            "com.motorola.camera3"
        )
    }

    private var torchEnabled = false
    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())

    // Gesture state machine
    private var pressCount = 0
    private var isKeyDown = false
    private var longPressTriggered = false
    private var currentForegroundPackage: String = ""

    // Runnables for timing
    private val multiPressRunnable = Runnable { onMultiPressWindowExpired() }
    private val longPressRunnable = Runnable { onLongPressTriggered() }

    // ──────────────────────────────────────────────────────────────────────────
    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        Log.d(TAG, "Accessibility Service Connected — Gesture engine active")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Context Awareness: track foreground app
    // ──────────────────────────────────────────────────────────────────────────
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()?.let { pkg ->
                if (pkg != currentForegroundPackage) {
                    currentForegroundPackage = pkg
                    Log.d(TAG, "Foreground app: $pkg")
                }
            }
        }
    }

    override fun onInterrupt() {
        // Required override
    }

    /** Check if a camera app is currently in the foreground */
    private fun isCameraInForeground(): Boolean {
        return currentForegroundPackage in CAMERA_PACKAGES ||
               currentForegroundPackage.contains("camera", ignoreCase = true)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Gesture Detection State Machine
    // ──────────────────────────────────────────────────────────────────────────
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!prefs.getBoolean(MainActivity.KEY_ENABLED, false)) {
            return super.onKeyEvent(event)
        }

        val customKey = prefs.getInt(MainActivity.KEY_KEYCODE, 0)
        if (event.keyCode != customKey) {
            return super.onKeyEvent(event)
        }

        when (event.action) {
            KeyEvent.ACTION_DOWN -> onTargetKeyDown()
            KeyEvent.ACTION_UP   -> onTargetKeyUp()
        }

        return true // consume the event
    }

    private fun onTargetKeyDown() {
        if (isKeyDown) return // ignore repeat events
        isKeyDown = true
        longPressTriggered = false

        // Cancel multi-press window (user is still pressing)
        handler.removeCallbacks(multiPressRunnable)

        // Start long-press timer
        handler.postDelayed(longPressRunnable, LONG_PRESS_THRESHOLD_MS)
    }

    private fun onTargetKeyUp() {
        isKeyDown = false

        // Cancel long-press timer
        handler.removeCallbacks(longPressRunnable)

        // If long-press already triggered, don't count this as a tap
        if (longPressTriggered) {
            longPressTriggered = false
            pressCount = 0
            return
        }

        // Count this press
        pressCount++

        // If we've hit 3 presses, fire immediately (no point waiting for more)
        if (pressCount >= 3) {
            handler.removeCallbacks(multiPressRunnable)
            onMultiPressWindowExpired()
            return
        }

        // Start/restart the multi-press detection window
        handler.removeCallbacks(multiPressRunnable)
        handler.postDelayed(multiPressRunnable, MULTI_PRESS_WINDOW_MS)
    }

    private fun onMultiPressWindowExpired() {
        val gesture = when (pressCount) {
            1    -> GESTURE_SINGLE
            2    -> GESTURE_DOUBLE
            else -> GESTURE_TRIPLE
        }
        pressCount = 0
        executeGestureAction(gesture)
    }

    private fun onLongPressTriggered() {
        longPressTriggered = true
        pressCount = 0
        executeGestureAction(GESTURE_LONG)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Action Execution
    // ──────────────────────────────────────────────────────────────────────────
    private fun executeGestureAction(gesture: String) {
        val inCamera = isCameraInForeground()
        val action: String

        if (inCamera) {
            // Try camera-specific override first, fall back to default
            val cameraAction = prefs.getString(cameraActionKey(gesture), "")
            action = if (!cameraAction.isNullOrEmpty()) cameraAction
                     else prefs.getString(actionKey(gesture), ACTION_NOTHING) ?: ACTION_NOTHING
        } else {
            action = prefs.getString(actionKey(gesture), getDefaultAction(gesture)) ?: ACTION_NOTHING
        }

        Log.d(TAG, "Gesture: $gesture | Camera: $inCamera | Action: $action")

        if (action == ACTION_DEFAULT) {
            // Don't execute any action — let the system handle it
            return
        }

        when (action) {
            ACTION_FLASHLIGHT   -> toggleFlashlight()
            ACTION_CAMERA       -> openCamera()
            ACTION_SHUTTER      -> triggerCameraShutter()
            ACTION_SCREENSHOT   -> takeScreenshot()
            ACTION_CYCLE_RINGER -> cycleRingerMode()
            ACTION_DIALER       -> openDialer()
            ACTION_NOTHING      -> { /* intentionally empty */ }
        }

        // Haptic feedback if enabled
        if (prefs.getBoolean(MainActivity.KEY_VIBRATE, true)) {
            vibrate(50)
        }
    }

    /** Default actions for each gesture when no preference is set */
    private fun getDefaultAction(gesture: String): String = when (gesture) {
        GESTURE_SINGLE -> ACTION_FLASHLIGHT
        GESTURE_DOUBLE -> ACTION_CAMERA
        GESTURE_TRIPLE -> ACTION_SCREENSHOT
        GESTURE_LONG   -> ACTION_CYCLE_RINGER
        else           -> ACTION_NOTHING
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Individual Actions
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

    private fun triggerCameraShutter() {
        // Send a media key event to trigger the camera shutter
        // This works when a camera app is in the foreground
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CAMERA)
            val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CAMERA)
            audioManager.dispatchMediaKeyEvent(downEvent)
            audioManager.dispatchMediaKeyEvent(upEvent)
            Log.d(TAG, "Camera shutter key dispatched")
        } catch (e: Exception) {
            Log.e(TAG, "Shutter error: ${e.message}")
            // Fallback: try volume key which many camera apps use as shutter
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
                val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP)
                audioManager.dispatchMediaKeyEvent(downEvent)
                audioManager.dispatchMediaKeyEvent(upEvent)
            } catch (e2: Exception) {
                Log.e(TAG, "Shutter fallback error: ${e2.message}")
            }
        }
    }

    private fun takeScreenshot() {
        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
    }

    private fun cycleRingerMode() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentMode = audioManager.ringerMode

            val nextMode = when (currentMode) {
                AudioManager.RINGER_MODE_NORMAL  -> AudioManager.RINGER_MODE_VIBRATE
                AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                else -> AudioManager.RINGER_MODE_NORMAL
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
        handler.removeCallbacksAndMessages(null)
        return super.onUnbind(intent)
    }
}
