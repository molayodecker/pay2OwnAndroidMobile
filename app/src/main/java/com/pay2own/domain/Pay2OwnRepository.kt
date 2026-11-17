package com.pay2own.domain

interface Pay2OwnRepository {
    suspend fun hasCredentials(): Boolean
    suspend fun isConfirmed(): Boolean
    suspend fun saveEnrollment(token: String, imei: String)
    suspend fun confirmEnrollment()
    suspend fun clearEnrollment()
    suspend fun loan(): Outcome<Loan>
    suspend fun startPayment(installments: Int, payBalance: Boolean): Outcome<StartedPayment>
    suspend fun payment(reference: String): Outcome<Payment>
    suspend fun payments(): Outcome<List<Payment>>
    suspend fun registerPushToken(token: String)
}

interface Sleeper {
    suspend fun sleep(millis: Long)
}
