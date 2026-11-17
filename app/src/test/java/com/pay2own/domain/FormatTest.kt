package com.pay2own.domain

import java.util.Calendar
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {
    @Test
    fun masksGhanaNumber() {
        assertEquals("055•••4987", maskPhone("0551234987"))
        assertFalse(maskPhone("0551234987").contains("123"))
    }

    @Test
    fun displayPhoneHidesARawNumber() {
        assertEquals("055•••4987", displayPhone("0551234987"))
        assertEquals("055•••4987", displayPhone("055•••4987"))
    }

    @Test
    fun enrollmentChecks() {
        assertTrue(validImei("000000000000001"))
        assertFalse(validImei("123"))
        assertTrue(validToken("a".repeat(64)))
        assertFalse(validToken("short"))
    }

    @Test
    fun moneyAndDates() {
        assertEquals("GHS 300.00", formatMoney("GHS", 30000))
        assertEquals("20 Nov 2026", formatIsoDate("2026-11-20"))
        assertEquals("overdue", dueLabel("unpaid", "2026-11-13", "2026-11-14"))
        assertEquals("paid", dueLabel("paid", "2026-11-13", "2026-11-14"))
    }

    @Test
    fun enrollLink() {
        val token = "ab".repeat(32)
        val link = parseEnrollLink("pay2own://enroll?token=$token&imei=000000000000001")
        assertEquals(token, link?.token)
        assertEquals("000000000000001", link?.imei)
        assertNull(parseEnrollLink("https://example.com"))
        assertNull(parseEnrollLink("pay2own://enroll?token=short&imei=000000000000001"))
        assertNull(parseEnrollLink("pay2own://enroll?token=%ZZ&imei=000000000000001"))
    }

    @Test
    fun todayKeepsAsciiDigits() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("ar-EG"))
        try {
            val calendar = Calendar.getInstance()
            calendar.set(2026, Calendar.NOVEMBER, 17)
            assertEquals("2026-11-17", todayIso(calendar))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
