package com.pay2own

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProvisioningModeTest {
    @Test
    fun factoryResetQrUsesFullyManagedWhenTheExtraIsMissing() {
        assertEquals(PROVISIONING_MODE_FULLY_MANAGED, selectProvisioningMode(null))
    }

    @Test
    fun choosesFullyManagedWhenItIsOffered() {
        assertEquals(PROVISIONING_MODE_FULLY_MANAGED, selectProvisioningMode(listOf(2, 1)))
    }

    @Test
    fun refusesAWorkProfileOnlyPhone() {
        assertNull(selectProvisioningMode(listOf(2)))
        assertNull(selectProvisioningMode(emptyList()))
    }
}
