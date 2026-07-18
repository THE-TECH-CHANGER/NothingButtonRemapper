package com.nothing.remapper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class KeyInterceptorService : AccessibilityService() {

    private lateinit var prefs: SharedPreferences
    private var isTorchOn = false

    // State machine for gestures
    private var pressCount = 0
    private var isLongPressing = false
    private val handler = Handler(Looper.getMainLooper())
    private val LONG_PRESS_TIMEOUT = 500L
    private val MULTI_PRESS_TIMEOUT = 300L
    
    // We only care about the Essential Key which typically sends KeyCode 0 on Nothing devices
    private val ESSENTIAL_KEYCODE = 0

    private val multiPressRunnable = Runnable {
        when (pressCount) {
            1 -> executeAction(getActionForGesture("single_press"))
            2 -> executeAction(getActionForGesture("double_press"))
            3 -> executeAction(getActionForGesture("triple_press"))
        }
        pressCount = 0
    }

    private val longPressRunnable = Runnable {
        isLongPressing = true
        executeAction(getActionForGesture("long_press"))
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            isTorchOn = enabled
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences("RemapperPrefs", Context.MODE_PRIVATE)
        
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        serviceInfo = info

        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var currentForegroundApp: String = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.let {
                currentForegroundApp = it.toString()
            }
        }
    }

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Broadcast the event for the Expert Mode UI
        val intent = Intent("com.nothing.remapper.KEY_EVENT")
        intent.putExtra("scanCode", event.scanCode)
        intent.putExtra("keyCode", event.keyCode)
        sendBroadcast(intent)

        if (event.keyCode != ESSENTIAL_KEYCODE && event.scanCode != 250) {
            return super.onKeyEvent(event)
        }

        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.repeatCount == 0) {
                // Initial press down
                isLongPressing = false
                handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT)
            }
            return true
        } else if (event.action == KeyEvent.ACTION_UP) {
            handler.removeCallbacks(longPressRunnable)
            if (!isLongPressing) {
                pressCount++
                handler.removeCallbacks(multiPressRunnable)
                handler.postDelayed(multiPressRunnable, MULTI_PRESS_TIMEOUT)
            }
            return true
        }

        return super.onKeyEvent(event)
    }

    private fun getActionForGesture(gesture: String): Triple<String, String, String> {
        // Check app-specific mapping first
        if (currentForegroundApp.isNotEmpty()) {
            val appType = prefs.getString("${currentForegroundApp}_${gesture}_type", null)
            val appAction = prefs.getString("${currentForegroundApp}_${gesture}", null)
            val appConstraint = prefs.getString("${currentForegroundApp}_${gesture}_constraint", "always")
            if (appType != null && appAction != null && appAction != "none") {
                return Triple(appType, appAction, appConstraint ?: "always")
            }
        }
        
        // Fallback to global mapping
        val type = prefs.getString("${gesture}_type", "builtin") ?: "builtin"
        val action = prefs.getString(gesture, "none") ?: "none"
        val constraint = prefs.getString("${gesture}_constraint", "always") ?: "always"
        return Triple(type, action, constraint)
    }

    @SuppressLint("MissingPermission")
    private fun executeAction(actionTriple: Triple<String, String, String>) {
        val type = actionTriple.first
        val action = actionTriple.second
        val constraint = actionTriple.third

        if (action == "none") return
        
        // Check constraints
        if (constraint != "always") {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val isScreenOn = pm.isInteractive
            val isUnlocked = !km.isDeviceLocked
            
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            when (constraint) {
                // General
                "always" -> {} // proceed
                
                // Apps
                "app_foreground" -> if (currentForegroundApp.isEmpty()) return
                "app_not_foreground" -> if (currentForegroundApp.isNotEmpty()) return
                "app_playing_media" -> if (!am.isMusicActive) return
                "app_not_playing_media" -> if (am.isMusicActive) return
                
                // Media
                "media_playing" -> if (!am.isMusicActive) return
                "media_not_playing" -> if (am.isMusicActive) return
                
                // Bluetooth
                "bluetooth_connected" -> if (!isBluetoothConnected()) return
                "bluetooth_disconnected" -> if (isBluetoothConnected()) return
                
                // Display
                "screen_on" -> if (!isScreenOn) return
                "screen_off" -> if (isScreenOn) return
                "orientation_landscape" -> if (resources.configuration.orientation != android.content.res.Configuration.ORIENTATION_LANDSCAPE) return
                "orientation_portrait" -> if (resources.configuration.orientation != android.content.res.Configuration.ORIENTATION_PORTRAIT) return
                
                // Flashlight
                "flashlight_on" -> if (!isTorchOn) return
                "flashlight_off" -> if (isTorchOn) return
                
                // WiFi
                "wifi_on" -> if (!isWifiConnected()) return
                "wifi_off" -> if (isWifiConnected()) return
                
                // Keyboard
                "keyboard_chosen" -> if (!isKeyboardChosen()) return
                "keyboard_not_chosen" -> if (isKeyboardChosen()) return
                "keyboard_showing" -> if (!isKeyboardShowing()) return
                "keyboard_not_showing" -> if (isKeyboardShowing()) return
                
                // Lock
                "locked" -> if (!isDeviceLocked()) return
                "unlocked" -> if (isDeviceLocked()) return
                "lock_screen_showing" -> if (!isKeyguardShowing()) return
                "lock_screen_not_showing" -> if (isKeyguardShowing()) return
                
                // Phone
                "phone_call" -> if (!isCallState(android.telephony.TelephonyManager.CALL_STATE_OFFHOOK)) return
                "not_phone_call" -> if (isCallState(android.telephony.TelephonyManager.CALL_STATE_OFFHOOK)) return
                "call_ringing" -> if (!isCallState(android.telephony.TelephonyManager.CALL_STATE_RINGING)) return
                
                // Power
                "power_charging" -> if (!isCharging()) return
                "power_discharging" -> if (isCharging()) return
                
                // Device
                "hinge_closed" -> if (!isHingeClosed()) return
                "hinge_open" -> if (isHingeClosed()) return
            }
        }

        if (type == "app") {
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(action)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                } else {
                    Toast.makeText(this, "Cannot launch app", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }
        
        if (type == "keycode") {
            try {
                val keycode = action.toInt()
                val injectIntent = Intent("com.nothing.remapper.INJECT_KEYCODE").apply {
                    putExtra("keycode", keycode)
                    setPackage(packageName)
                }
                sendBroadcast(injectIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        // Built-in actions
        when (action) {
            "flashlight" -> toggleFlashlight()
            "screenshot" -> performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            "camera" -> launchCamera()
            "media_play_pause" -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            "media_next" -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            "media_previous" -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            "lock_screen" -> performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            "quick_settings" -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            "power_menu" -> performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            "split_screen" -> performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
            "volume_up" -> adjustVolume(AudioManager.ADJUST_RAISE)
            "volume_down" -> adjustVolume(AudioManager.ADJUST_LOWER)
        }
    }

    private fun toggleFlashlight() {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            isTorchOn = !isTorchOn
            cameraManager.setTorchMode(cameraId, isTorchOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchCamera() {
        val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun sendMediaKey(keyCode: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val eventDown = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keyCode, 0)
        audioManager.dispatchMediaKeyEvent(eventDown)
        val eventUp = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0)
        audioManager.dispatchMediaKeyEvent(eventUp)
    }
    
    private fun adjustVolume(direction: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }

    @SuppressLint("MissingPermission")
    private fun isBluetoothConnected(): Boolean {
        return try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
            val adapter = bluetoothManager.adapter ?: return false
            adapter.getProfileConnectionState(android.bluetooth.BluetoothProfile.HEADSET) == android.bluetooth.BluetoothProfile.STATE_CONNECTED ||
            adapter.getProfileConnectionState(android.bluetooth.BluetoothProfile.A2DP) == android.bluetooth.BluetoothProfile.STATE_CONNECTED
        } catch (e: Exception) {
            false
        }
    }

    private fun isWifiConnected(): Boolean {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            cm.activeNetworkInfo?.type == android.net.ConnectivityManager.TYPE_WIFI && cm.activeNetworkInfo?.isConnected == true
        } catch (e: Exception) {
            false
        }
    }

    private fun isCharging(): Boolean {
        val intent = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
    }

    @SuppressLint("MissingPermission")
    private fun isCallState(state: Int): Boolean {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
        return tm.callState == state
    }
    
    private fun isKeyboardChosen(): Boolean {
        return try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.currentInputMethodSubtype != null
        } catch (e: Exception) {
            false
        }
    }

    private fun isKeyboardShowing(): Boolean {
        return try {
            softKeyboardController.showMode != android.accessibilityservice.AccessibilityService.SHOW_MODE_HIDDEN
        } catch (e: Exception) {
            false
        }
    }

    private fun isDeviceLocked(): Boolean {
        return try {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            km.isDeviceLocked
        } catch (e: Exception) {
            false
        }
    }

    private fun isKeyguardShowing(): Boolean {
        return try {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            km.isKeyguardLocked
        } catch (e: Exception) {
            false
        }
    }

    private fun isHingeClosed(): Boolean {
        // Fallback for non-foldable devices or without explicit hinge sensor handling
        return false
    }
}
