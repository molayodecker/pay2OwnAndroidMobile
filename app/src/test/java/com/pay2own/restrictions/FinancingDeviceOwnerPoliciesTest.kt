package com.pay2own.restrictions

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
        assertEquals(FINANCING_RESTRICTIONS.toSet(), client.added)
        assertTrue(client.blocked)
        assertFalse(policies.areClear())
    }

    @Test
    fun clearingRemovesEveryFinancingRestriction() {
        val client = FakePolicyClient(owner = true)
        val policies = FinancingDeviceOwnerPolicies(client)
        policies.applyLock()
        policies.clear()
        assertEquals(FINANCING_RESTRICTIONS.toSet(), client.cleared)
        assertFalse(client.blocked)
        assertTrue(policies.areClear())
    }

    @Test
    fun aRestrictionThatStaysOnIsNotReportedClear() {
        val client = FakePolicyClient(owner = true, sticky = FINANCING_RESTRICTIONS.first())
        val policies = FinancingDeviceOwnerPolicies(client)
        policies.applyLock()
        policies.clear()
        assertFalse(policies.areClear())
    }
}

private class FakePolicyClient(
    private val owner: Boolean,
    private val sticky: String? = null,
) : DevicePolicyClient {
    val added = mutableSetOf<String>()
    val cleared = mutableSetOf<String>()
    var blocked = false
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
}
