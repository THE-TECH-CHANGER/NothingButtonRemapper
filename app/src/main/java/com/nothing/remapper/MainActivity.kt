package com.nothing.remapper

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.net.Uri
import android.content.ClipData
import android.content.ClipboardManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "remapper_prefs"
        const val KEY_ACTION = "selected_action"
        const val KEY_ENABLED = "remapping_enabled"
        const val KEY_KEYCODE = "essential_keycode"
        const val KEY_VIBRATE = "vibrate_feedback"

        const val ACTION_DEFAULT = "default"
        const val ACTION_FLASHLIGHT = "flashlight"
        const val ACTION_CAMERA = "camera"
        const val ACTION_SCREENSHOT = "screenshot"
        const val ACTION_NOTHING = "nothing"
        const val ACTION_CYCLE_RINGER = "cycle_ringer"
        const val ACTION_CALL = "call"
    }

    private lateinit var prefs: SharedPreferences
    private var currentAction = ACTION_FLASHLIGHT

    // Views
    private lateinit var tvStatusTitle: TextView
    private lateinit var tvServiceStatus: TextView
    private lateinit var switchEnable: SwitchMaterial
    private lateinit var switchVibrate: SwitchMaterial

    // Termux Views
    private lateinit var btnDownloadTermux: View
    private lateinit var btnCopyCommand: View

    // Action selection cards
    private lateinit var cardFlashlight: LinearLayout
    private lateinit var cardCamera: LinearLayout
    private lateinit var cardScreenshot: LinearLayout
    private lateinit var cardCycleRinger: LinearLayout
    private lateinit var cardCall: LinearLayout
    private lateinit var cardNothing: LinearLayout
    private lateinit var cardDefault: LinearLayout

    private val allCards: List<LinearLayout> get() =
        listOf(cardFlashlight, cardCamera, cardScreenshot, cardCycleRinger, cardCall, cardNothing, cardDefault)

    private val actionToCard: Map<String, LinearLayout> get() = mapOf(
        ACTION_FLASHLIGHT to cardFlashlight,
        ACTION_CAMERA     to cardCamera,
        ACTION_SCREENSHOT to cardScreenshot,
        ACTION_CYCLE_RINGER to cardCycleRinger,
        ACTION_CALL       to cardCall,
        ACTION_NOTHING    to cardNothing,
        ACTION_DEFAULT    to cardDefault
    )

    // ──────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        bindViews()
        loadPreferences()
    }

    // ──────────────────────────────────────────────────────────────────────────
    private fun bindViews() {
        tvStatusTitle   = findViewById(R.id.tv_root_status) // Recycled the TextView ID from root version
        tvServiceStatus = findViewById(R.id.tv_service_status)
        switchEnable    = findViewById(R.id.switch_enable)
        switchVibrate   = findViewById(R.id.switch_vibrate)
        
        cardFlashlight  = findViewById(R.id.card_flashlight)
        cardCamera      = findViewById(R.id.card_camera)
        cardScreenshot  = findViewById(R.id.card_screenshot)
        cardCycleRinger = findViewById(R.id.card_cycle_ringer)
        cardCall        = findViewById(R.id.card_call)
        cardNothing     = findViewById(R.id.card_nothing)
        cardDefault     = findViewById(R.id.card_default)

        // Accessibility check updates
        tvStatusTitle.text = "Checking..."

        switchEnable.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_ENABLED, isChecked).apply()
            
            if (isChecked) {
                if (!isAccessibilityServiceEnabled(this, KeyInterceptorService::class.java)) {
                    toast("Please enable the Accessibility Service first!")
                    openAccessibilitySettings()
                    switchEnable.isChecked = false
                }
            }
        }

        switchVibrate.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_VIBRATE, isChecked).apply()
        }

        allCards.forEach { card ->
            card.setOnClickListener { onCardSelected(card) }
        }

        // Termux logic
        btnDownloadTermux = findViewById(R.id.btn_download_termux)
        btnCopyCommand = findViewById(R.id.btn_copy_command)

        btnDownloadTermux.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/com.termux/"))
            startActivity(intent)
        }

        btnCopyCommand.setOnClickListener {
            val cmd = "adb shell pm disable-user --user 0 com.nothing.ntessentialspace && adb shell pm disable-user --user 0 com.nothing.ntessentialrecorder"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ADB Command", cmd)
            clipboard.setPrimaryClip(clip)
            toast("Command copied! Paste it in Termux (with ADB setup) or on your PC.")
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    private fun onCardSelected(card: LinearLayout) {
        currentAction = actionToCard.entries.find { it.value == card }?.key ?: return
        prefs.edit().putString(KEY_ACTION, currentAction).apply()
        updateCardStates()
    }

    // ──────────────────────────────────────────────────────────────────────────
    private fun updateCardStates() {
        allCards.forEach { card ->
            val action    = actionToCard.entries.find { it.value == card }?.key
            val selected  = (action == currentAction)
            applyCardState(card, selected)
        }
    }

    private fun applyCardState(card: LinearLayout, selected: Boolean) {
        val bgColor     = if (selected) 0xFF1E1E1E.toInt() else 0xFF141414.toInt()
        val strokeColor = if (selected) 0xFFFFFFFF.toInt() else 0xFF2A2A2A.toInt()
        val strokeWidth = dpToPx(if (selected) 1 else 1)

        card.background = GradientDrawable().apply {
            setColor(bgColor)
            setStroke(strokeWidth, strokeColor)
            cornerRadius = dpToPx(12).toFloat()
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    private fun loadPreferences() {
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        switchEnable.isChecked = enabled

        currentAction = prefs.getString(KEY_ACTION, ACTION_FLASHLIGHT) ?: ACTION_FLASHLIGHT
        updateCardStates()

        switchVibrate.isChecked = prefs.getBoolean(KEY_VIBRATE, true)
    }



    // ──────────────────────────────────────────────────────────────────────────
    private fun checkServiceStatus() {
        val isEnabled = isAccessibilityServiceEnabled(this, KeyInterceptorService::class.java)
        
        if (isEnabled) {
            tvStatusTitle.text = "Granted ✓"
            tvStatusTitle.setTextColor(0xFF3DDC84.toInt())
            tvServiceStatus.text = "Active"
            tvServiceStatus.setTextColor(0xFF3DDC84.toInt())
        } else {
            tvStatusTitle.text = "Denied ✗"
            tvStatusTitle.setTextColor(0xFFE8372C.toInt())
            tvServiceStatus.text = "Inactive"
            tvServiceStatus.setTextColor(0xFF666666.toInt())
            switchEnable.isChecked = false
        }
    }

    override fun onResume() {
        super.onResume()
        checkServiceStatus()
    }

    // ──────────────────────────────────────────────────────────────────────────
    private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (enabledService in enabledServices) {
            val serviceInfo = enabledService.resolveInfo.serviceInfo
            if (serviceInfo.packageName == context.packageName && serviceInfo.name == service.name) {
                return true
            }
        }
        return false
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
