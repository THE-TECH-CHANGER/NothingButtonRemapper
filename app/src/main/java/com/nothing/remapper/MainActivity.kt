package com.nothing.remapper

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.accessibility.AccessibilityManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import rikka.shizuku.Shizuku
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private var currentProfilePackage = ""
    
    // Broadcast receiver for Expert Mode (Key Test)
    private val keyEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.nothing.remapper.KEY_EVENT") {
                val scanCode = intent.getIntExtra("scanCode", -1)
                val keyCode = intent.getIntExtra("keyCode", -1)
                val tvKeyEvents = findViewById<TextView>(R.id.tvKeyEvents)
                val currentText = tvKeyEvents.text.toString()
                val newText = "Scan code $scanCode \t Key code $keyCode"
                
                if (currentText == "Waiting for key press...") {
                    tvKeyEvents.text = newText
                } else {
                    val lines = currentText.split("\n").takeLast(4)
                    tvKeyEvents.text = (lines + newText).joinToString("\n")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("RemapperPrefs", Context.MODE_PRIVATE)

        setupServiceCard()
        setupUnlockWizardCard()
        setupGestureCards()
        setupProfileSelector()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        updateUnlockStatus()
        updateGestureLabels()
        
        // Register receiver for Expert Mode
        registerReceiver(keyEventReceiver, IntentFilter("com.nothing.remapper.KEY_EVENT"), RECEIVER_NOT_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(keyEventReceiver)
    }

    private fun setupServiceCard() {
        val btnServiceAction = findViewById<Button>(R.id.btnServiceAction)
        btnServiceAction.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    private fun updateServiceStatus() {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val isEnabled = enabledServices.any { it.id.contains(packageName) }

        val tvServiceStatus = findViewById<TextView>(R.id.tvServiceStatus)
        val indicatorService = findViewById<android.view.View>(R.id.indicatorService)

        if (isEnabled) {
            tvServiceStatus.text = "SERVICE - RUNNING"
            indicatorService.setBackgroundResource(R.drawable.bg_status_dot_green)
        } else {
            tvServiceStatus.text = "SERVICE - STOPPED"
            indicatorService.setBackgroundResource(R.drawable.bg_status_dot_red)
        }
    }

    private fun setupUnlockWizardCard() {
        val btnUnlockWizard = findViewById<Button>(R.id.btnUnlockWizard)
        btnUnlockWizard.setOnClickListener {
            showUnlockWizardBottomSheet()
        }
    }

    private fun updateUnlockStatus() {
        val tvUnlockStatus = findViewById<TextView>(R.id.tvUnlockStatus)
        val indicatorUnlock = findViewById<android.view.View>(R.id.indicatorUnlock)
        
        val isFreed = isPackageDisabled("com.nothing.ntessentialspace") && isPackageDisabled("com.nothing.ntessentialrecorder")
        if (isFreed) {
            tvUnlockStatus.text = "SINGLE PRESS - FREED"
            indicatorUnlock.setBackgroundResource(R.drawable.bg_status_dot_green)
        } else {
            tvUnlockStatus.text = "SINGLE PRESS - LOCKED"
            indicatorUnlock.setBackgroundResource(R.drawable.bg_status_dot_red)
        }
    }

    private fun showUnlockWizardBottomSheet() {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_unlock_wizard, null)
        bottomSheet.setContentView(view)

        val tvSpaceStatus = view.findViewById<TextView>(R.id.tvSpaceStatus)
        val tvRecorderStatus = view.findViewById<TextView>(R.id.tvRecorderStatus)
        val tvShizukuStatus = view.findViewById<TextView>(R.id.tvShizukuStatus)
        val btnFreeSinglePress = view.findViewById<Button>(R.id.btnFreeSinglePress)
        val btnRestore = view.findViewById<Button>(R.id.btnRestore)

        val updateStatuses = {
            tvSpaceStatus.text = if (isPackageDisabled("com.nothing.ntessentialspace")) "FREED" else "INSTALLED"
            tvRecorderStatus.text = if (isPackageDisabled("com.nothing.ntessentialrecorder")) "FREED" else "INSTALLED"
        }
        updateStatuses()

        if (Shizuku.pingBinder()) {
            val hasPermission = if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                checkSelfPermission("moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }

            if (hasPermission) {
                tvShizukuStatus.text = "Shizuku ready"
                btnFreeSinglePress.setOnClickListener {
                    disablePackageViaShizuku("com.nothing.ntessentialspace")
                    disablePackageViaShizuku("com.nothing.ntessentialrecorder")
                    disablePackageViaShizuku("com.essentialintelligence")
                    updateStatuses()
                    updateUnlockStatus()
                    Toast.makeText(this, "Freed!", Toast.LENGTH_SHORT).show()
                }
                btnRestore.setOnClickListener {
                    enablePackageViaShizuku("com.nothing.ntessentialspace")
                    enablePackageViaShizuku("com.nothing.ntessentialrecorder")
                    enablePackageViaShizuku("com.essentialintelligence")
                    updateStatuses()
                    updateUnlockStatus()
                    Toast.makeText(this, "Restored!", Toast.LENGTH_SHORT).show()
                }
            } else {
                tvShizukuStatus.text = "Shizuku permission not granted"
                btnFreeSinglePress.setOnClickListener { Shizuku.requestPermission(0) }
            }
        } else {
            tvShizukuStatus.text = "Shizuku not running. Please start it."
            btnFreeSinglePress.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku/releases"))
                startActivity(intent)
            }
        }

        bottomSheet.show()
    }

    private fun setupGestureCards() {
        findViewById<LinearLayout>(R.id.rowSinglePress).setOnClickListener { showActionPicker("single_press") }
        findViewById<LinearLayout>(R.id.rowDoublePress).setOnClickListener { showActionPicker("double_press") }
        findViewById<LinearLayout>(R.id.rowTriplePress).setOnClickListener { showActionPicker("triple_press") }
        findViewById<LinearLayout>(R.id.rowLongPress).setOnClickListener { showActionPicker("long_press") }
    }

    private fun updateGestureLabels() {
        val getActionLabel = { baseKey: String -> 
            val profileKey = if (currentProfilePackage.isEmpty()) "" else "${currentProfilePackage}_$baseKey"
            var isGlobal = false
            var type = if (profileKey.isNotEmpty()) prefs.getString("${profileKey}_type", null) else null
            var action = if (profileKey.isNotEmpty()) prefs.getString(profileKey, null) else null
            var constraint = if (profileKey.isNotEmpty()) prefs.getString("${profileKey}_constraint", "always") else "always"
            
            if (type == null || action == null || action == "none") {
                type = prefs.getString("${baseKey}_type", "builtin")
                action = prefs.getString(baseKey, "none")
                constraint = prefs.getString("${baseKey}_constraint", "always")
                isGlobal = currentProfilePackage.isNotEmpty()
            }
            
            val constraintText = when (constraint) {
                "screen_on" -> " (Screen On)"
                "screen_off" -> " (Screen Off)"
                "unlocked" -> " (Unlocked)"
                "media_playing" -> " (Media Playing)"
                "media_not_playing" -> " (Media Not Playing)"
                "flashlight_on" -> " (Flashlight On)"
                "flashlight_off" -> " (Flashlight Off)"
                "bluetooth_connected" -> " (BT Connected)"
                "bluetooth_disconnected" -> " (BT Disconnected)"
                "wifi_connected" -> " (WiFi Connected)"
                "wifi_disconnected" -> " (WiFi Disconnected)"
                "power_charging" -> " (Charging)"
                "power_discharging" -> " (Discharging)"
                "call_ringing" -> " (Ringing)"
                "call_offhook" -> " (Call Active)"
                else -> ""
            }
            
            val labelText = if (type == "app" && action != null) {
                try {
                    val ai = packageManager.getApplicationInfo(action, 0)
                    packageManager.getApplicationLabel(ai).toString()
                } catch (e: Exception) {
                    action
                }
            } else if (type == "keycode" && action != null) {
                "Keycode $action"
            } else {
                action?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "None"
            }
            
            if (isGlobal && labelText != "None") {
                "Global ($labelText$constraintText)"
            } else {
                "$labelText$constraintText"
            }
        }

        findViewById<TextView>(R.id.tvSingleAction).text = getActionLabel("single_press")
        findViewById<TextView>(R.id.tvDoubleAction).text = getActionLabel("double_press")
        findViewById<TextView>(R.id.tvTripleAction).text = getActionLabel("triple_press")
        findViewById<TextView>(R.id.tvLongAction).text = getActionLabel("long_press")
    }

    private val categorizedConstraints = mapOf(
        "General" to listOf(
            "always" to "Always (Any State)"
        ),
        "Apps" to listOf(
            "app_foreground" to "App in foreground",
            "app_not_foreground" to "App not in foreground",
            "app_playing_media" to "App playing media",
            "app_not_playing_media" to "App not playing media"
        ),
        "Media" to listOf(
            "media_not_playing" to "No media is playing",
            "media_playing" to "Media is playing"
        ),
        "Bluetooth" to listOf(
            "bluetooth_connected" to "Bluetooth device is connected",
            "bluetooth_disconnected" to "Bluetooth device is disconnected"
        ),
        "Display" to listOf(
            "screen_on" to "Screen is on",
            "screen_off" to "Screen is off",
            "orientation_landscape" to "Screen orientation landscape",
            "orientation_portrait" to "Screen orientation portrait"
        ),
        "Flashlight" to listOf(
            "flashlight_on" to "Flashlight is on",
            "flashlight_off" to "Flashlight is off"
        ),
        "WiFi" to listOf(
            "wifi_on" to "WiFi is on",
            "wifi_off" to "WiFi is off"
        ),
        "Keyboard" to listOf(
            "keyboard_chosen" to "Input method is chosen",
            "keyboard_not_chosen" to "Input method is not chosen",
            "keyboard_showing" to "Keyboard is showing",
            "keyboard_not_showing" to "Keyboard is not showing"
        ),
        "Lock" to listOf(
            "locked" to "Device is locked",
            "unlocked" to "Device is unlocked",
            "lock_screen_showing" to "Lock screen is showing",
            "lock_screen_not_showing" to "Lock screen is not showing"
        ),
        "Phone" to listOf(
            "phone_call" to "In phone call",
            "not_phone_call" to "Not in phone call",
            "call_ringing" to "Phone ringing"
        ),
        "Power" to listOf(
            "power_charging" to "Charging",
            "power_discharging" to "Discharging"
        ),
        "Device" to listOf(
            "hinge_closed" to "Hinge closed",
            "hinge_open" to "Hinge open"
        )
    )

    private fun getConstraintDisplayName(key: String): String {
        for ((_, list) in categorizedConstraints) {
            for ((k, name) in list) {
                if (k == key) return name
            }
        }
        return "Always (Any State)"
    }

    private fun getSavedProfiles(): List<String> {
        val savedStr = prefs.getString("saved_profiles", "") ?: ""
        if (savedStr.isEmpty()) return emptyList()
        return savedStr.split(",").filter { it.isNotEmpty() }
    }

    private fun addSavedProfile(packageName: String) {
        val current = getSavedProfiles().toMutableList()
        if (!current.contains(packageName)) {
            current.add(packageName)
            prefs.edit().putString("saved_profiles", current.joinToString(",")).apply()
        }
    }

    private fun removeSavedProfile(packageName: String) {
        val current = getSavedProfiles().toMutableList()
        current.remove(packageName)
        prefs.edit()
            .putString("saved_profiles", current.joinToString(","))
            .remove("${packageName}_single_press_type")
            .remove("${packageName}_single_press")
            .remove("${packageName}_double_press_type")
            .remove("${packageName}_double_press")
            .remove("${packageName}_triple_press_type")
            .remove("${packageName}_triple_press")
            .remove("${packageName}_long_press_type")
            .remove("${packageName}_long_press")
            .apply()
    }

    private fun setupProfileSelector() {
        val btnProfileSelector = findViewById<LinearLayout>(R.id.btnProfileSelector)
        val tvCurrentProfile = findViewById<TextView>(R.id.tvCurrentProfile)
        val btnAddProfile = findViewById<LinearLayout>(R.id.btnAddProfile)
        val btnRemoveProfile = findViewById<LinearLayout>(R.id.btnRemoveProfile)
        
        val updateRemoveButton = {
            btnRemoveProfile.visibility = if (currentProfilePackage.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        }
        
        btnRemoveProfile.setOnClickListener {
            if (currentProfilePackage.isNotEmpty()) {
                removeSavedProfile(currentProfilePackage)
                currentProfilePackage = ""
                tvCurrentProfile.text = "Global"
                updateGestureLabels()
                updateRemoveButton()
            }
        }
        
        btnAddProfile.setOnClickListener {
            val bottomSheet = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.layout_action_picker, null)
            bottomSheet.setContentView(view)

            view.findViewById<TextView>(R.id.tvActionPickerSubtitle).text = "Select App to Profile"
            
            view.findViewById<TextView>(R.id.tvConstraintHeader).visibility = android.view.View.GONE
            view.findViewById<LinearLayout>(R.id.btnChooseConstraint).visibility = android.view.View.GONE
            view.findViewById<TextView>(R.id.tvBuiltInHeader).visibility = android.view.View.GONE
            view.findViewById<LinearLayout>(R.id.llSimulateKeyContainer).visibility = android.view.View.GONE
            view.findViewById<LinearLayout>(R.id.llBuiltInActions).visibility = android.view.View.GONE
            
            val llApps = view.findViewById<LinearLayout>(R.id.llApps)
            val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val apps = packageManager.queryIntentActivities(intent, 0).sortedBy { it.loadLabel(packageManager).toString() }
            
            for (info in apps) {
                val appView = layoutInflater.inflate(R.layout.item_app, llApps, false)
                appView.findViewById<ImageView>(R.id.ivAppIcon).setImageDrawable(info.loadIcon(packageManager))
                appView.findViewById<TextView>(R.id.tvAppName).text = info.loadLabel(packageManager)
                
                appView.setOnClickListener {
                    val pkg = info.activityInfo.packageName
                    addSavedProfile(pkg)
                    currentProfilePackage = pkg
                    tvCurrentProfile.text = info.loadLabel(packageManager).toString()
                    updateGestureLabels()
                    updateRemoveButton()
                    bottomSheet.dismiss()
                }
                llApps.addView(appView)
            }

            bottomSheet.show()
        }
        
        btnProfileSelector.setOnClickListener {
            val bottomSheet = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.layout_action_picker, null)
            bottomSheet.setContentView(view)

            view.findViewById<TextView>(R.id.tvActionPickerSubtitle).text = "Select Saved Profile"
            
            // Built-in Actions view used for "Global"
            val builtinActions = mutableListOf("Global (Default)")
            
            val savedProfiles = getSavedProfiles()
            val profileNames = mutableListOf<String>()
            val profilePkgs = mutableListOf<String>()
            
            for (pkg in savedProfiles) {
                try {
                    val ai = packageManager.getApplicationInfo(pkg, 0)
                    profileNames.add(packageManager.getApplicationLabel(ai).toString())
                    profilePkgs.add(pkg)
                } catch (e: Exception) {
                    profileNames.add(pkg)
                    profilePkgs.add(pkg)
                }
            }
            
            builtinActions.addAll(profileNames)
            
            val llBuiltIn = view.findViewById<LinearLayout>(R.id.llBuiltInActions)
            llBuiltIn.removeAllViews()
            
            for ((index, actionName) in builtinActions.withIndex()) {
                val itemView = layoutInflater.inflate(R.layout.item_action, llBuiltIn, false) as TextView
                itemView.text = actionName
                itemView.setOnClickListener {
                    if (index == 0) {
                        currentProfilePackage = ""
                        tvCurrentProfile.text = "Global"
                    } else {
                        currentProfilePackage = profilePkgs[index - 1]
                        tvCurrentProfile.text = profileNames[index - 1]
                    }
                    updateGestureLabels()
                    updateRemoveButton()
                    bottomSheet.dismiss()
                }
                llBuiltIn.addView(itemView)
            }

            view.findViewById<TextView>(R.id.tvConstraintHeader).visibility = android.view.View.GONE
            view.findViewById<LinearLayout>(R.id.btnChooseConstraint).visibility = android.view.View.GONE
            view.findViewById<TextView>(R.id.tvBuiltInHeader).visibility = android.view.View.GONE
            view.findViewById<LinearLayout>(R.id.llSimulateKeyContainer).visibility = android.view.View.GONE
            view.findViewById<TextView>(R.id.tvLaunchAppHeader).visibility = android.view.View.GONE
            view.findViewById<LinearLayout>(R.id.llApps).visibility = android.view.View.GONE

            bottomSheet.show()
        }
    }

    private fun showActionPicker(baseGestureKey: String) {
        val gestureKey = if (currentProfilePackage.isEmpty()) baseGestureKey else "${currentProfilePackage}_$baseGestureKey"
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_action_picker, null)
        bottomSheet.setContentView(view)

        view.findViewById<TextView>(R.id.tvActionPickerSubtitle).text = gestureKey.replace("_", " ").replaceFirstChar { it.uppercase() }

        // Constraints
        val btnChooseConstraint = view.findViewById<LinearLayout>(R.id.btnChooseConstraint)
        val tvCurrentConstraint = view.findViewById<TextView>(R.id.tvCurrentConstraint)
        
        var currentConstraint = prefs.getString("${gestureKey}_constraint", "always") ?: "always"
        tvCurrentConstraint.text = getConstraintDisplayName(currentConstraint)
        
        btnChooseConstraint.setOnClickListener {
            showConstraintPicker(gestureKey, tvCurrentConstraint) { newConstraint ->
                currentConstraint = newConstraint
            }
        }

        // Built-in Actions
        val builtinActions = listOf(
            "none", "flashlight", "screenshot", "camera", 
            "media_play_pause", "media_next", "media_previous", 
            "home", "back", "recents", "lock_screen", 
            "volume_up", "volume_down", 
            "notifications", "quick_settings", "power_menu", "split_screen"
        )
        val llBuiltIn = view.findViewById<LinearLayout>(R.id.llBuiltInActions)
        llBuiltIn.removeAllViews()
        
        for ((index, actionName) in builtinActions.withIndex()) {
            val itemView = layoutInflater.inflate(R.layout.item_action, llBuiltIn, false) as TextView
            itemView.text = actionName.replace("_", " ").replaceFirstChar { c -> c.uppercase() }
            itemView.setOnClickListener {
                prefs.edit()
                    .putString("${gestureKey}_type", "builtin")
                    .putString(gestureKey, builtinActions[index])
                    .putString("${gestureKey}_constraint", currentConstraint)
                    .apply()
                updateGestureLabels()
                bottomSheet.dismiss()
            }
            llBuiltIn.addView(itemView)
        }

        // Simulate Keycode
        val btnSimulateKeycode = view.findViewById<LinearLayout>(R.id.btnSimulateKeycode)
        btnSimulateKeycode?.setOnClickListener {
            val input = android.widget.EditText(this)
            input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            input.hint = "e.g. 24 for Volume Up"
            
            android.app.AlertDialog.Builder(this)
                .setTitle("Enter Android Keycode")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    val keycodeStr = input.text.toString()
                    if (keycodeStr.isNotEmpty()) {
                        prefs.edit()
                            .putString("${gestureKey}_type", "keycode")
                            .putString(gestureKey, keycodeStr)
                            .putString("${gestureKey}_constraint", currentConstraint)
                            .apply()
                        updateGestureLabels()
                        bottomSheet.dismiss()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Installed Apps
        val llApps = view.findViewById<LinearLayout>(R.id.llApps)
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val apps = packageManager.queryIntentActivities(intent, 0).sortedBy { it.loadLabel(packageManager).toString() }
        
        for (info in apps) {
            val appView = layoutInflater.inflate(R.layout.item_app, llApps, false)
            appView.findViewById<ImageView>(R.id.ivAppIcon).setImageDrawable(info.loadIcon(packageManager))
            appView.findViewById<TextView>(R.id.tvAppName).text = info.loadLabel(packageManager)
            
            appView.setOnClickListener {
                prefs.edit()
                    .putString("${gestureKey}_type", "app")
                    .putString(gestureKey, info.activityInfo.packageName)
                    .putString("${gestureKey}_constraint", currentConstraint)
                    .apply()
                updateGestureLabels()
                bottomSheet.dismiss()
            }
            llApps.addView(appView)
        }

        bottomSheet.show()
    }

    private fun showConstraintPicker(gestureKey: String, tvCurrentConstraint: TextView, onConstraintSelected: (String) -> Unit) {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_constraint_picker, null)
        bottomSheet.setContentView(view)

        val llConstraintsContainer = view.findViewById<LinearLayout>(R.id.llConstraintsContainer)

        for ((category, list) in categorizedConstraints) {
            val headerView = layoutInflater.inflate(R.layout.item_category_header, llConstraintsContainer, false) as TextView
            headerView.text = category.uppercase()
            llConstraintsContainer.addView(headerView)

            for ((constraintKey, constraintName) in list) {
                val itemView = layoutInflater.inflate(R.layout.item_action, llConstraintsContainer, false) as TextView
                itemView.text = constraintName
                itemView.setOnClickListener {
                    prefs.edit().putString("${gestureKey}_constraint", constraintKey).apply()
                    tvCurrentConstraint.text = constraintName
                    onConstraintSelected(constraintKey)
                    updateGestureLabels()
                    bottomSheet.dismiss()
                }
                llConstraintsContainer.addView(itemView)
            }
        }

        bottomSheet.show()
    }

    private fun isPackageDisabled(packageName: String): Boolean {
        try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            return !info.enabled
        } catch (e: Exception) {
            return true
        }
    }

    private fun disablePackageViaShizuku(packageName: String) {
        ShizukuUtils.runCommand(arrayOf("pm", "disable-user", "--user", "0", packageName))
    }

    private fun enablePackageViaShizuku(packageName: String) {
        ShizukuUtils.runCommand(arrayOf("pm", "enable", "--user", "0", packageName))
    }
}
