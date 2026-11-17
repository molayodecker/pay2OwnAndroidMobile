package com.pay2own.ui

import com.pay2own.domain.Loan
import com.pay2own.domain.Outcome
import com.pay2own.domain.Pay2OwnRepository
import com.pay2own.domain.Payment
import com.pay2own.domain.StartedPayment

class FakeRepository : Pay2OwnRepository {
    var loanOutcome: Outcome<Loan> = Outcome.Fresh(com.pay2own.domain.sampleLoan())
    var startOutcome: Outcome<StartedPayment> = Outcome.Fresh(StartedPayment("p2o_abc", "pay_offline"))
    var paymentOutcome: Outcome<Payment> = Outcome.Fresh(
        Payment("p2o_abc", 30_000, "GHS", "success", "2026-11-14T10:00:00Z", listOf(1)),
    )
    val paymentScript = ArrayDeque<Outcome<Payment>>()
    var startCalls = 0
    var confirmed = false
    var credentials = false

    override suspend fun hasCredentials() = credentials
    override suspend fun isConfirmed() = confirmed
    override suspend fun saveEnrollment(token: String, imei: String) {
        credentials = true
    }
    override suspend fun confirmEnrollment() {
        confirmed = true
    }
    override suspend fun clearEnrollment() {
        credentials = false
        confirmed = false
    }
    override suspend fun loan() = loanOutcome
    override suspend fun startPayment(installments: Int, payBalance: Boolean): Outcome<StartedPayment> {
        startCalls++
        return startOutcome
    }
    override suspend fun payment(reference: String): Outcome<Payment> {
        return if (paymentScript.isEmpty()) paymentOutcome else paymentScript.removeFirst()
    }
    override suspend fun payments() = Outcome.Fresh(emptyList<Payment>())
    override suspend fun registerPushToken(token: String) = Unit
}
