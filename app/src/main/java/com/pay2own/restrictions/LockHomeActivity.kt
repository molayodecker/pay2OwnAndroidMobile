package com.pay2own.restrictions

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.pay2own.MainActivity

/** Home screen while other apps are suspended. Opens Pay2Own so the phone can still be paid. */
class LockHomeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }
}
