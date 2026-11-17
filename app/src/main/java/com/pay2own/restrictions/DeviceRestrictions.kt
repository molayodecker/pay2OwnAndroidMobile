package com.pay2own.restrictions

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import com.pay2own.Pay2OwnDeviceAdminReceiver
import com.pay2own.domain.DeviceOwnerPolicies
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Financing controls that stay on while the server says the phone is restricted.
 * Emergency calls stay available. The phone is not put into kiosk mode.
 */
val FINANCING_RESTRICTIONS: List<String> = listOf(
    UserManager.DISALLOW_FACTORY_RESET,
    UserManager.DISALLOW_SAFE_BOOT,
    UserManager.DISALLOW_ADD_USER,
)

interface DevicePolicyClient {
    fun isDeviceOwner(): Boolean
    fun addUserRestriction(restriction: String)
    fun clearUserRestriction(restriction: String)
    fun hasUserRestriction(restriction: String): Boolean
    fun setUninstallBlocked(blocked: Boolean)
    fun isUninstallBlocked(): Boolean
}

@Singleton
class AndroidDevicePolicyClient @Inject constructor(
    @ApplicationContext private val context: Context,
) : DevicePolicyClient {
    private val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val users = context.getSystemService(Context.USER_SERVICE) as UserManager
    private val admin = ComponentName(context, Pay2OwnDeviceAdminReceiver::class.java)

    override fun isDeviceOwner(): Boolean = manager.isDeviceOwnerApp(context.packageName)

    override fun addUserRestriction(restriction: String) {
        manager.addUserRestriction(admin, restriction)
    }

    override fun clearUserRestriction(restriction: String) {
        manager.clearUserRestriction(admin, restriction)
    }

    override fun hasUserRestriction(restriction: String): Boolean = users.hasUserRestriction(restriction)

    override fun setUninstallBlocked(blocked: Boolean) {
        manager.setUninstallBlocked(admin, context.packageName, blocked)
    }

    override fun isUninstallBlocked(): Boolean = manager.isUninstallBlocked(admin, context.packageName)
}

/**
 * Applies financing restrictions only when this app is device owner.
 * A phone that is not enrolled has nothing to remove, so the release check passes.
 * An enrolled phone is not reported clear until factory reset, safe boot, extra users,
 * and uninstall of Pay2Own are all allowed again.
 */
@Singleton
class FinancingDeviceOwnerPolicies @Inject constructor(
    private val client: DevicePolicyClient,
) : DeviceOwnerPolicies {
    override fun applyLock() {
        if (!client.isDeviceOwner()) return
        FINANCING_RESTRICTIONS.forEach { quietly { client.addUserRestriction(it) } }
        quietly { client.setUninstallBlocked(true) }
    }

    override fun clear() {
        if (!client.isDeviceOwner()) return
        FINANCING_RESTRICTIONS.forEach { quietly { client.clearUserRestriction(it) } }
        quietly { client.setUninstallBlocked(false) }
    }

    override fun areClear(): Boolean {
        if (!client.isDeviceOwner()) return true
        return try {
            FINANCING_RESTRICTIONS.none { client.hasUserRestriction(it) } && !client.isUninstallBlocked()
        } catch (_: SecurityException) {
            false
        }
    }

    private fun quietly(block: () -> Unit) {
        try {
            block()
        } catch (_: SecurityException) {
        }
    }
}
