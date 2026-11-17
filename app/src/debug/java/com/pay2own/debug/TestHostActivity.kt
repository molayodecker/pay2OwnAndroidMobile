package com.pay2own.debug

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity

/** Blank host so UI tests can draw while the phone stays on the lock screen. */
class TestHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }
}
