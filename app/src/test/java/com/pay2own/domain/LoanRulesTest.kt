package com.pay2own.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanRulesTest {
    @Test
    fun payIsBlockedWhileWaitingOfflineOrPaidOff() {
        val loan = sampleLoan()
        assertTrue(canPay(loan, offline = false))
        assertFalse(canPay(loan, offline = true))
        assertFalse(canPay(loan.copy(pendingReference = "p2o_wait"), offline = false))
        assertFalse(canPay(loan.copy(accountStatus = "paid_off", next = null, remainingMinor = 0), offline = false))
    }

    @Test
    fun amountIsTheNextInstallmentsOrTheBalance() {
        val loan = sampleLoan()
        assertEquals(30_000L, selectedAmount(loan, count = 1, payBalance = false))
        assertEquals(60_000L, selectedAmount(loan, count = 2, payBalance = false))
        assertEquals(120_000L, selectedAmount(loan, count = 1, payBalance = true))
    }
}

fun sampleLoan() = Loan(
    customerName = "Ama Mensah",
    phoneMasked = "055•••4987",
    provider = "mtn",
    model = "Samsung Galaxy A15",
    currency = "GHS",
    priceMinor = 200_000,
    depositMinor = 80_000,
    remainingMinor = 120_000,
    paidMinor = 80_000,
    startedOn = "2026-10-01",
    loanStatus = "active",
    accountStatus = "up_to_date",
    installmentCount = 4,
    paidCount = 0,
    next = Installment(1, "2026-11-20", 30_000, "unpaid"),
    pendingReference = null,
    schedule = (1..4).map { Installment(it, "2026-11-20", 30_000, "unpaid") },
    terms = "Pay2Own payment terms",
)
