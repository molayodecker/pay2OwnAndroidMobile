package com.pay2own.restrictions

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import android.provider.Settings
import android.telecom.TelecomManager
import com.pay2own.MainActivity
import com.pay2own.Pay2OwnDeviceAdminReceiver
import com.pay2own.domain.DeviceOwnerPolicies
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Financing controls that stay on while the server says the phone is restricted.
 * Calls stay available. The phone is not put into kiosk mode.
 */
/** Platform restriction key. The public SDK does not expose the constant. */
internal const val DISALLOW_OEM_UNLOCK = "no_oem_unlock"

/** Packages a restricted phone still needs in order to call, type, and open Pay2Own. */
internal val CALL_AND_SYSTEM_PACKAGES = setOf(
    "android",
    "com.android.systemui",
    "com.android.phone",
    "com.android.server.telecom",
    "com.android.providers.telephony",
    "com.android.providers.settings",
    "com.android.emergency",
    "com.android.dialer",
    "com.google.android.dialer",
    "com.android.incallui",
    "com.samsung.android.incallui",
    "com.android.bluetooth",
    "com.android.permissioncontroller",
    "com.google.android.permissioncontroller",
    "com.google.android.gms",
    "com.google.android.gsf",
    "com.android.networkstack",
    "com.google.android.networkstack",
)

fun financingRestrictions(sdkInt: Int = Build.VERSION.SDK_INT): List<String> = buildList {
    add(UserManager.DISALLOW_FACTORY_RESET)
    add(UserManager.DISALLOW_SAFE_BOOT)
    add(UserManager.DISALLOW_ADD_USER)
    add(UserManager.DISALLOW_DEBUGGING_FEATURES)
    add(UserManager.DISALLOW_APPS_CONTROL)
    if (sdkInt >= Build.VERSION_CODES.O) add(DISALLOW_OEM_UNLOCK)
}

fun keepPackages(
    ownPackage: String,
    defaultDialer: String?,
    defaultIme: String?,
    installed: Set<String>,
): Set<String> = buildSet {
    add(ownPackage)
    if (!defaultDialer.isNullOrBlank()) add(defaultDialer)
    if (!defaultIme.isNullOrBlank()) add(defaultIme)
    CALL_AND_SYSTEM_PACKAGES.filterTo(this) { it in installed }
}

fun packagesToSuspend(installed: Collection<String>, keep: Set<String>): List<String> =
    installed.filter { it !in keep }.distinct()

interface DevicePolicyClient {
    fun isDeviceOwner(): Boolean
    fun addUserRestriction(restriction: String)
    fun clearUserRestriction(restriction: String)
    fun hasUserRestriction(restriction: String): Boolean
    fun setUninstallBlocked(blocked: Boolean)
    fun isUninstallBlocked(): Boolean
    fun installedPackages(): List<String>
    fun keepPackages(): Set<String>
    fun suspendPackages(packages: List<String>)
    fun restorePackages(packages: List<String>)
    fun suspendedPackages(): List<String>
    fun setLockedHome(enabled: Boolean)
    fun isLockedHomeEnabled(): Boolean
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

    override fun installedPackages(): List<String> =
        context.packageManager.getInstalledPackages(PackageManager.MATCH_DISABLED_COMPONENTS)
            .map { it.packageName }

    override fun keepPackages(): Set<String> = keepPackages(
        ownPackage = context.packageName,
        defaultDialer = defaultDialer(),
        defaultIme = defaultIme(),
        installed = installedPackages().toSet(),
    )

    override fun suspendPackages(packages: List<String>) = applySuspension(packages, true)

    override fun restorePackages(packages: List<String>) = applySuspension(packages, false)

    override fun suspendedPackages(): List<String> {
        val keep = keepPackages()
        return installedPackages().filter { name -> name !in keep && isSuspended(name) }
    }

    override fun setLockedHome(enabled: Boolean) {
        val home = ComponentName(context, LockHomeActivity::class.java)
        if (enabled) {
            if (isLockedHomeEnabled()) return
            context.packageManager.setComponentEnabledSetting(
                home,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            val filter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            manager.addPersistentPreferredActivity(admin, filter, home)
            try {
                context.startActivity(
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (_: RuntimeException) {
            }
            return
        }
        manager.clearPackagePersistentPreferredActivities(admin, context.packageName)
        context.packageManager.setComponentEnabledSetting(
            home,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    override fun isLockedHomeEnabled(): Boolean =
        context.packageManager.getComponentEnabledSetting(ComponentName(context, LockHomeActivity::class.java)) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    private fun applySuspension(packages: List<String>, suspended: Boolean) {
        if (packages.isEmpty()) return
        try {
            suspendBatch(packages, suspended)
        } catch (_: IllegalArgumentException) {
            packages.forEach { suspendOne(it, suspended) }
        } catch (_: SecurityException) {
            packages.forEach { suspendOne(it, suspended) }
        }
    }

    private fun suspendOne(name: String, suspended: Boolean) {
        try {
            suspendBatch(listOf(name), suspended)
        } catch (_: IllegalArgumentException) {
        } catch (_: SecurityException) {
        }
    }

    private fun suspendBatch(packages: List<String>, suspended: Boolean) {
        manager.setPackagesSuspended(admin, packages.toTypedArray(), suspended)
    }

    private fun isSuspended(name: String): Boolean = try {
        manager.isPackageSuspended(admin, name)
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (_: SecurityException) {
        true
    }

    private fun defaultDialer(): String? = try {
        (context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.defaultDialerPackage
    } catch (_: SecurityException) {
        null
    }

    private fun defaultIme(): String? =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')
            ?.takeIf { it.isNotBlank() }
}

/**
 * Applies financing restrictions only when this app is device owner.
 * A phone that is not enrolled has nothing to remove, so the release check passes.
 * An enrolled phone is not reported clear until factory reset, safe boot, extra users,
 * USB debugging, force stop, OEM unlocking, uninstall of Pay2Own, and the other apps
 * are all allowed again.
 */
@Singleton
class FinancingDeviceOwnerPolicies @Inject constructor(
    private val client: DevicePolicyClient,
) : DeviceOwnerPolicies {
    override fun applyLock() {
        if (!client.isDeviceOwner()) return
        financingRestrictions().forEach { quietly { client.addUserRestriction(it) } }
        quietly { client.setUninstallBlocked(true) }
        quietly { client.setLockedHome(true) }
        quietly { client.suspendPackages(packagesToSuspend(client.installedPackages(), client.keepPackages())) }
    }

    override fun clear() {
        if (!client.isDeviceOwner()) return
        financingRestrictions().forEach { quietly { client.clearUserRestriction(it) } }
        quietly { client.setUninstallBlocked(false) }
        quietly { client.restorePackages(client.suspendedPackages()) }
        quietly { client.setLockedHome(false) }
    }

    override fun areClear(): Boolean {
        if (!client.isDeviceOwner()) return true
        return try {
            financingRestrictions().none { client.hasUserRestriction(it) } &&
                !client.isUninstallBlocked() &&
                client.suspendedPackages().isEmpty() &&
                !client.isLockedHomeEnabled()
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
