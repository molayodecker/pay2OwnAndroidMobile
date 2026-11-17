package com.pay2own

import android.app.Activity
import android.os.Bundle

/**
 * Android 12 and later wait for this screen after device-owner provisioning.
 * Financing restrictions are applied later, once the phone is enrolled.
 */
class AdminPolicyComplianceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_OK)
        finish()
    }
}
