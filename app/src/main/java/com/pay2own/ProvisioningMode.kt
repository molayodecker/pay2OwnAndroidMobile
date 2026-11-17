package com.pay2own

/** [android.app.admin.DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE] */
const val PROVISIONING_MODE_FULLY_MANAGED = 1

/**
 * Shop phones are factory reset, then enrolled as device owner.
 * A work profile cannot lock the whole phone, so any other offered mode is refused.
 * A missing list means the extra was omitted; fully managed is the only mode this app supports.
 */
fun selectProvisioningMode(allowed: List<Int>?): Int? = when {
    allowed == null -> PROVISIONING_MODE_FULLY_MANAGED
    PROVISIONING_MODE_FULLY_MANAGED in allowed -> PROVISIONING_MODE_FULLY_MANAGED
    else -> null
}
