package com.pay2own.restrictions

import android.os.Build
import android.os.UserManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancingDeviceOwnerPoliciesTest {
    @Test
    fun unenrolledPhoneHasNothingToClear() {
        val client = FakePolicyClient(owner = false)
        val policies = FinancingDeviceOwnerPolicies(client)
        policies.applyLock()
        policies.clear()
        assertTrue(policies.areClear())
        assertEquals(0, client.added.size)
        assertEquals(0, client.cleared.size)
        assertFalse(client.blocked)
    }

    @Test
    fun deviceOwnerLockBlocksResetSafeBootUsersAndUninstall() {
        val client = FakePolicyClient(owner = true)
        val policies = FinancingDeviceOwnerPolicies(client)
        policies.applyLock()
        assertEquals(financingRestrictions().toSet(), client.added)
        assertTrue(client.blocked)
        assertFalse(policies.areClear())
    }

    @Test
    fun clearingRemovesEveryFinancingRestriction() {
        val client = FakePolicyClient(owner = true)
        val policies = FinancingDeviceOwnerPolicies(client)
        policies.applyLock()
        policies.clear()
        assertEquals(financingRestrictions().toSet(), client.cleared)
        assertFalse(client.blocked)
        assertTrue(policies.areClear())
    }

    @Test
    fun aRestrictionThatStaysOnIsNotReportedClear() {
        val client = FakePolicyClient(owner = true, sticky = financingRestrictions().first())
        val policies = FinancingDeviceOwnerPolicies(client)
        policies.applyLock()
        policies.clear()
        assertFalse(policies.areClear())
    }

    @Test
    fun oemUnlockIsBlockedFromAndroid8() {
        assertFalse(financingRestrictions(Build.VERSION_CODES.N).contains(DISALLOW_OEM_UNLOCK))
        assertTrue(financingRestrictions(Build.VERSION_CODES.O).contains(DISALLOW_OEM_UNLOCK))
    }

    @Test
    fun debuggingAndAppControlAreAlwaysBlocked() {
        val restrictions = financingRestrictions(Build.VERSION_CODES.N)
        assertTrue(restrictions.contains(UserManager.DISALLOW_DEBUGGING_FEATURES))
        assertTrue(restrictions.contains(UserManager.DISALLOW_APPS_CONTROL))
    }

    @Test
    fun lockSuspendsOtherAppsAndKeepsTheDialer() {
        val installed = listOf("com.pay2own", "com.android.dialer", "com.whatsapp", "com.android.settings")
        val keep = keepPackages(
            ownPackage = "com.pay2own",
            defaultDialer = "com.android.dialer",
            defaultIme = "com.google.android.inputmethod.latin",
            installed = installed.toSet(),
        )
        assertEquals(listOf("com.whatsapp", "com.android.settings"), packagesToSuspend(installed, keep))
        val client = FakePolicyClient(owner = true, installed = installed, keep = keep)
        val policies = FinancingDeviceOwnerPolicies(client)
        policies.applyLock()
        assertEquals(setOf("com.whatsapp", "com.android.settings"), client.suspended)
        assertTrue(client.home)
        policies.clear()
        assertTrue(client.suspended.isEmpty())
        assertFalse(client.home)
        assertTrue(policies.areClear())
    }

    @Test
    fun aSuspendedAppIsNotReportedClear() {
        val client = FakePolicyClient(
            owner = true,
            installed = listOf("com.pay2own", "com.whatsapp"),
            keep = setOf("com.pay2own"),
            stickySuspended = "com.whatsapp",
        )
        val policies = FinancingDeviceOwnerPolicies(client)
        policies.applyLock()
        policies.clear()
        assertEquals(setOf("com.whatsapp"), client.suspended)
        assertFalse(policies.areClear())
    }
}

private class FakePolicyClient(
    private val owner: Boolean,
    private val sticky: String? = null,
    private val installed: List<String> = emptyList(),
    private val keep: Set<String> = emptySet(),
    private val stickySuspended: String? = null,
) : DevicePolicyClient {
    val added = mutableSetOf<String>()
    val cleared = mutableSetOf<String>()
    val suspended = mutableSetOf<String>()
    var blocked = false
    var home = false
    override fun isDeviceOwner() = owner
    override fun addUserRestriction(restriction: String) {
        added += restriction
    }
    override fun clearUserRestriction(restriction: String) {
        cleared += restriction
        if (restriction != sticky) added -= restriction
    }
    override fun hasUserRestriction(restriction: String) = restriction in added
    override fun setUninstallBlocked(blocked: Boolean) {
        this.blocked = blocked
    }
    override fun isUninstallBlocked() = blocked
    override fun installedPackages() = installed
    override fun keepPackages() = keep
    override fun suspendPackages(packages: List<String>) {
        suspended += packages
    }
    override fun restorePackages(packages: List<String>) {
        suspended -= packages.filter { it != stickySuspended }.toSet()
    }
    override fun suspendedPackages() = suspended.toList()
    override fun setLockedHome(enabled: Boolean) {
        home = enabled
    }
    override fun isLockedHomeEnabled() = home
}
