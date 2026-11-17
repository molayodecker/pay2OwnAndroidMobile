package com.pay2own

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle

/**
 * Android 12 and later ask the admin app which mode to use after the APK installs.
 * Finishing without [DevicePolicyManager.EXTRA_PROVISIONING_MODE] aborts setup.
 */
class GetProvisioningModeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val allowed = intent.getIntegerArrayListExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_ALLOWED_PROVISIONING_MODES,
        )
        val mode = selectProvisioningMode(allowed)
        if (mode == null) {
            setResult(RESULT_CANCELED)
        } else {
            setResult(RESULT_OK, Intent().putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE, mode))
        }
        finish()
    }
}
