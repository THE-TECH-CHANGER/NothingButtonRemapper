package com.nothing.remapper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.KeyEvent

class RemapperInputMethodService : InputMethodService() {

    private val keyInjectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_INJECT_KEYCODE) {
                val keycode = intent.getIntExtra(EXTRA_KEYCODE, -1)
                if (keycode != -1) {
                    injectKey(keycode)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(ACTION_INJECT_KEYCODE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(keyInjectionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(keyInjectionReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(keyInjectionReceiver)
    }

    private fun injectKey(keycode: Int) {
        val inputConnection = currentInputConnection ?: return
        inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keycode))
        inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keycode))
    }

    companion object {
        const val ACTION_INJECT_KEYCODE = "com.nothing.remapper.INJECT_KEYCODE"
        const val EXTRA_KEYCODE = "keycode"
    }
}
