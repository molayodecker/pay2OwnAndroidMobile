package com.pay2own.data

import com.pay2own.data.api.Pay2OwnApi
import com.pay2own.data.api.PushTokenDto
import com.pay2own.data.api.StartPaymentDto
import com.pay2own.data.api.toDomain
import com.pay2own.data.local.LocalStore
import com.pay2own.domain.Loan
import com.pay2own.domain.Outcome
import com.pay2own.domain.Pay2OwnRepository
import com.pay2own.domain.Payment
import com.pay2own.domain.ReleaseWorkScheduler
import com.pay2own.domain.StartedPayment
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Pay2OwnRepositoryImpl @Inject constructor(
    private val api: Pay2OwnApi,
    private val store: LocalStore,
    private val releaseWork: ReleaseWorkScheduler,
) : Pay2OwnRepository {
    override suspend fun hasCredentials(): Boolean = store.readCredentials() != null

    override suspend fun isConfirmed(): Boolean = store.isConfirmed()

    override suspend fun saveEnrollment(token: String, imei: String) {
        store.saveCredentials(token, imei)
        store.setConfirmed(false)
    }

    override suspend fun confirmEnrollment() {
        store.setConfirmed(true)
    }

    override suspend fun clearEnrollment() {
        releaseWork.cancel()
        store.clear()
    }

    override suspend fun loan(): Outcome<Loan> {
        val creds = store.readCredentials() ?: return Outcome.SignedOut
        return try {
            val dto = api.loan(creds.imei)
            store.saveLoan(dto)
            Outcome.Fresh(dto.toDomain())
        } catch (error: HttpException) {
            if (error.code() == 401 || error.code() == 403) Outcome.SignedOut
            else httpFailure(error, store.cachedLoan())
        } catch (_: IOException) {
            store.cachedLoan()?.let { Outcome.Cached(it) } ?: Outcome.Offline
        }
    }

    override suspend fun startPayment(installments: Int, payBalance: Boolean): Outcome<StartedPayment> {
        val creds = store.readCredentials() ?: return Outcome.SignedOut
        val body = if (payBalance) {
            StartPaymentDto(payBalance = true)
        } else {
            StartPaymentDto(installments = installments.coerceAtLeast(1))
        }
        return try {
            Outcome.Fresh(api.startPayment(creds.imei, body).toDomain())
        } catch (error: HttpException) {
            httpFailure(error, null)
        } catch (_: IOException) {
            Outcome.Offline
        }
    }

    override suspend fun payment(reference: String): Outcome<Payment> {
        val creds = store.readCredentials() ?: return Outcome.SignedOut
        return try {
            Outcome.Fresh(api.payment(creds.imei, reference).toDomain())
        } catch (error: HttpException) {
            if (error.code() == 401 || error.code() == 403) Outcome.SignedOut
            else httpFailure(error, store.cachedPayments()?.find { it.reference == reference })
        } catch (_: IOException) {
            store.cachedPayments()?.find { it.reference == reference }?.let { Outcome.Cached(it) }
                ?: Outcome.Offline
        }
    }

    override suspend fun payments(): Outcome<List<Payment>> {
        val creds = store.readCredentials() ?: return Outcome.SignedOut
        return try {
            val items = api.payments(creds.imei).payments
            store.savePayments(items)
            Outcome.Fresh(items.map { it.toDomain() })
        } catch (error: HttpException) {
            if (error.code() == 401 || error.code() == 403) Outcome.SignedOut
            else httpFailure(error, store.cachedPayments())
        } catch (_: IOException) {
            store.cachedPayments()?.let { Outcome.Cached(it) } ?: Outcome.Offline
        }
    }

    override suspend fun registerPushToken(token: String) {
        val creds = store.readCredentials() ?: return
        try {
            val response = api.pushToken(creds.imei, PushTokenDto(token))
            response.body()?.close()
            response.errorBody()?.close()
        } catch (_: Exception) {
            // The next launch tries again. A missed registration is not a crash.
        }
    }
}

private fun <T> httpFailure(error: HttpException, cached: T?): Outcome<T> {
    error.response()?.errorBody()?.close()
    return when (error.code()) {
        401, 403 -> Outcome.SignedOut
        404 -> Outcome.NotFound
        409 -> Outcome.Conflict
        400 -> Outcome.BadRequest
        502 -> Outcome.Busy
        else -> cached?.let { Outcome.Cached(it) } ?: Outcome.Failed
    }
}
