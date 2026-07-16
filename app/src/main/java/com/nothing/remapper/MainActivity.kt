package com.nothing.remapper

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "remapper_prefs"
        const val KEY_ENABLED = "remapping_enabled"
        const val KEY_KEYCODE = "essential_keycode"
        const val KEY_VIBRATE = "vibrate_feedback"
        const val SHIZUKU_REQUEST_CODE = 1001
    }

    private lateinit var prefs: SharedPreferences

    // Views
    private lateinit var tvServiceStatus: TextView
    private lateinit var tvShizukuStatus: TextView
    private lateinit var switchEnable: SwitchMaterial
    private lateinit var switchVibrate: SwitchMaterial

    // Shizuku Setup Views
    private lateinit var cardShizukuSetup: LinearLayout
    private lateinit var tvSetupTitle: TextView
    private lateinit var indicatorStep1: TextView
    private lateinit var indicatorStep2: TextView
    private lateinit var indicatorStep3: TextView
    private lateinit var btnStep1: TextView
    private lateinit var btnStep2: TextView
    private lateinit var btnStep3: TextView

    // Gesture Cards
    private lateinit var cardSinglePress: LinearLayout
    private lateinit var cardDoublePress: LinearLayout
    private lateinit var cardTriplePress: LinearLayout
    private lateinit var cardLongPress: LinearLayout
    
    // Action Texts
    private lateinit var tvActionSingle: TextView
    private lateinit var tvActionDouble: TextView
    private lateinit var tvActionTriple: TextView
    private lateinit var tvActionLong: TextView
    
    // Actions map
    private val actionNames = mapOf(
        KeyInterceptorService.ACTION_FLASHLIGHT to "Flashlight",
        KeyInterceptorService.ACTION_CAMERA to "Open Camera",
        KeyInterceptorService.ACTION_SHUTTER to "Camera Shutter",
        KeyInterceptorService.ACTION_SCREENSHOT to "Screenshot",
        KeyInterceptorService.ACTION_CYCLE_RINGER to "Cycle Ringer",
        KeyInterceptorService.ACTION_DIALER to "Phone Dialer",
        KeyInterceptorService.ACTION_NOTHING to "Do Nothing",
        KeyInterceptorService.ACTION_DEFAULT to "Pass Through"
    )

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread { updateShizukuUI() }
    }
    
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        runOnUiThread { updateShizukuUI() }
    }
    
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_REQUEST_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                disableNothingPackages()
            } else {
                toast(getString(R.string.shizuku_permission_denied))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        bindViews()
        loadPreferences()
        
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }

    override fun onResume() {
        super.onResume()
        updateServiceUI()
        updateShizukuUI()
    }

    private fun bindViews() {
        tvServiceStatus = findViewById(R.id.tv_service_status)
        tvShizukuStatus = findViewById(R.id.tv_shizuku_status)
        switchEnable = findViewById(R.id.switch_enable)
        switchVibrate = findViewById(R.id.switch_vibrate)

        cardShizukuSetup = findViewById(R.id.card_shizuku_setup)
        tvSetupTitle = findViewById(R.id.tv_setup_title)
        indicatorStep1 = findViewById(R.id.indicator_step1)
        indicatorStep2 = findViewById(R.id.indicator_step2)
        indicatorStep3 = findViewById(R.id.indicator_step3)
        btnStep1 = findViewById(R.id.btn_step1)
        btnStep2 = findViewById(R.id.btn_step2)
        btnStep3 = findViewById(R.id.btn_step3)
        
        cardSinglePress = findViewById(R.id.card_single_press)
        cardDoublePress = findViewById(R.id.card_double_press)
        cardTriplePress = findViewById(R.id.card_triple_press)
        cardLongPress = findViewById(R.id.card_long_press)
        
        tvActionSingle = findViewById(R.id.tv_action_single)
        tvActionDouble = findViewById(R.id.tv_action_double)
        tvActionTriple = findViewById(R.id.tv_action_triple)
        tvActionLong = findViewById(R.id.tv_action_long)

        switchEnable.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_ENABLED, isChecked).apply()
            if (isChecked && !isAccessibilityServiceEnabled()) {
                toast("Please enable the Accessibility Service first!")
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                switchEnable.isChecked = false
            }
        }

        switchVibrate.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_VIBRATE, isChecked).apply()
        }

        // Shizuku Onboarding Buttons
        btnStep1.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"))
            startActivity(intent)
        }
        
        btnStep2.setOnClickListener {
            val intent = packageManager.getLaunchIntentForPackage(ShizukuHelper.SHIZUKU_PACKAGE)
            if (intent != null) {
                startActivity(intent)
            } else {
                toast(getString(R.string.shizuku_not_installed))
            }
        }
        
        btnStep3.setOnClickListener {
            if (!ShizukuHelper.isShizukuAvailable()) {
                toast(getString(R.string.shizuku_not_running))
                return@setOnClickListener
            }
            if (ShizukuHelper.isPermissionGranted()) {
                disableNothingPackages()
            } else {
                ShizukuHelper.requestPermission(SHIZUKU_REQUEST_CODE)
            }
        }
        
        // Restore default behavior
        findViewById<View>(R.id.btn_restore_default).setOnClickListener {
            if (!ShizukuHelper.isShizukuAvailable() || !ShizukuHelper.isPermissionGranted()) {
                toast("Please complete Shizuku setup first.")
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val result = ShizukuHelper.enableNothingPackages()
                if (result.isSuccess) {
                    toast(getString(R.string.shizuku_packages_restored))
                } else {
                    toast(getString(R.string.shizuku_error, result.exceptionOrNull()?.message))
                }
            }
        }
        
        // Gesture configurators
        cardSinglePress.setOnClickListener { showActionPicker(KeyInterceptorService.GESTURE_SINGLE, tvActionSingle) }
        cardDoublePress.setOnClickListener { showActionPicker(KeyInterceptorService.GESTURE_DOUBLE, tvActionDouble) }
        cardTriplePress.setOnClickListener { showActionPicker(KeyInterceptorService.GESTURE_TRIPLE, tvActionTriple) }
        cardLongPress.setOnClickListener { showActionPicker(KeyInterceptorService.GESTURE_LONG, tvActionLong) }
    }

    private fun loadPreferences() {
        switchEnable.isChecked = prefs.getBoolean(KEY_ENABLED, false)
        switchVibrate.isChecked = prefs.getBoolean(KEY_VIBRATE, true)
        
        tvActionSingle.text = getActionName(KeyInterceptorService.GESTURE_SINGLE, KeyInterceptorService.ACTION_FLASHLIGHT)
        tvActionDouble.text = getActionName(KeyInterceptorService.GESTURE_DOUBLE, KeyInterceptorService.ACTION_CAMERA)
        tvActionTriple.text = getActionName(KeyInterceptorService.GESTURE_TRIPLE, KeyInterceptorService.ACTION_SCREENSHOT)
        tvActionLong.text = getActionName(KeyInterceptorService.GESTURE_LONG, KeyInterceptorService.ACTION_CYCLE_RINGER)
    }

    private fun getActionName(gesture: String, defaultAction: String): String {
        val actionId = prefs.getString(KeyInterceptorService.actionKey(gesture), defaultAction) ?: defaultAction
        return actionNames[actionId] ?: actionId
    }

    private fun showActionPicker(gesture: String, tvTarget: TextView) {
        val keys = actionNames.keys.toTypedArray()
        val names = actionNames.values.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Choose Action")
            .setItems(names) { _, which ->
                val selectedAction = keys[which]
                prefs.edit().putString(KeyInterceptorService.actionKey(gesture), selectedAction).apply()
                
                // If they picked shutter, let's automatically set it as a camera-only override too
                // just so it works perfectly when camera is open
                if (selectedAction == KeyInterceptorService.ACTION_SHUTTER) {
                    prefs.edit().putString(KeyInterceptorService.cameraActionKey(gesture), KeyInterceptorService.ACTION_SHUTTER).apply()
                }
                
                tvTarget.text = names[which]
            }
            .show()
    }

    private fun disableNothingPackages() {
        lifecycleScope.launch {
            val result = ShizukuHelper.disableNothingPackages()
            if (result.isSuccess) {
                toast(getString(R.string.shizuku_packages_disabled))
                updateShizukuUI() // Refresh the checkmarks
            } else {
                toast(getString(R.string.shizuku_error, result.exceptionOrNull()?.message))
            }
        }
    }

    private fun updateShizukuUI() {
        val isInstalled = ShizukuHelper.isShizukuInstalled(this)
        val isRunning = ShizukuHelper.isShizukuAvailable()
        val isPermitted = ShizukuHelper.isPermissionGranted()
        
        // Update top status chip
        if (isRunning && isPermitted) {
            tvShizukuStatus.text = "Active"
            tvShizukuStatus.setTextColor(getColor(R.color.active_green))
        } else if (isRunning) {
            tvShizukuStatus.text = "Waiting Perm"
            tvShizukuStatus.setTextColor(getColor(R.color.error_red))
        } else {
            tvShizukuStatus.text = "Inactive"
            tvShizukuStatus.setTextColor(getColor(R.color.text_secondary))
        }

        // Update step 1 (Install)
        if (isInstalled) {
            indicatorStep1.setBackgroundResource(R.drawable.bg_step_complete)
            indicatorStep1.text = "✓"
            btnStep1.visibility = View.GONE
        } else {
            indicatorStep1.setBackgroundResource(R.drawable.bg_step_indicator)
            indicatorStep1.text = "1"
            btnStep1.visibility = View.VISIBLE
        }

        // Update step 2 (Running)
        if (isRunning) {
            indicatorStep2.setBackgroundResource(R.drawable.bg_step_complete)
            indicatorStep2.text = "✓"
            btnStep2.visibility = View.GONE
        } else {
            indicatorStep2.setBackgroundResource(R.drawable.bg_step_indicator)
            indicatorStep2.text = "2"
            btnStep2.visibility = View.VISIBLE
        }

        // We can check if packages are disabled by seeing if Shizuku works and checking them, 
        // but for UI simplicity, if everything is running and permitted, we assume step 3 is done 
        // or ready to be done. We could add an actual check via pm path.
        lifecycleScope.launch(Dispatchers.IO) {
            var step3Done = false
            if (isRunning && isPermitted) {
                val r = ShizukuHelper.executeCommand("pm list packages -d")
                if (r.isSuccess) {
                    val out = r.getOrNull() ?: ""
                    if (out.contains(ShizukuHelper.PACKAGE_ESSENTIAL_SPACE) && 
                        out.contains(ShizukuHelper.PACKAGE_ESSENTIAL_RECORDER)) {
                        step3Done = true
                    }
                }
            }
            
            withContext(Dispatchers.Main) {
                if (step3Done) {
                    indicatorStep3.setBackgroundResource(R.drawable.bg_step_complete)
                    indicatorStep3.text = "✓"
                    btnStep3.visibility = View.GONE
                    tvSetupTitle.text = getString(R.string.setup_complete)
                } else {
                    indicatorStep3.setBackgroundResource(R.drawable.bg_step_indicator)
                    indicatorStep3.text = "3"
                    btnStep3.visibility = View.VISIBLE
                    tvSetupTitle.text = getString(R.string.setup_title)
                }
                
                // Optional: Collapse setup card if everything is done
                // if (isInstalled && isRunning && step3Done) {
                //     cardShizukuSetup.visibility = View.GONE
                // }
            }
        }
    }

    private fun updateServiceUI() {
        val enabled = isAccessibilityServiceEnabled()
        if (enabled) {
            tvServiceStatus.text = "Active"
            tvServiceStatus.setTextColor(getColor(R.color.active_green))
        } else {
            tvServiceStatus.text = "Inactive"
            tvServiceStatus.setTextColor(getColor(R.color.text_secondary))
            switchEnable.isChecked = false
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in enabledServices) {
            val info = service.resolveInfo.serviceInfo
            if (info.packageName == packageName && info.name == KeyInterceptorService::class.java.name) {
                return true
            }
        }
        return false
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
