package com.pay2own.restrictions

import com.pay2own.data.RestrictionRepositoryImpl
import com.pay2own.data.RestrictionStore
import com.pay2own.data.api.LoanDto
import com.pay2own.data.api.Pay2OwnApi
import com.pay2own.data.api.PaymentDto
import com.pay2own.data.api.PaymentsDto
import com.pay2own.data.api.PushTokenDto
import com.pay2own.data.api.RestrictionDto
import com.pay2own.data.api.StartPaymentDto
import com.pay2own.data.api.StartedDto
import com.pay2own.domain.DeviceOwnerPolicies
import com.pay2own.domain.RestrictionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ReleaseWorkerTest {
    @Test
    fun noContentClearsPoliciesThenReportsReleased() = runTest {
        val store = MemoryStore()
        val api = ScriptedApi(204)
        val policies = RecordingPolicies()
        val result = releaseOnce(RestrictionRepositoryImpl(api, store), policies)
        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
        assertTrue(policies.cleared)
        assertEquals(1, api.releaseCalls)
        assertEquals(RestrictionState.Released.wire, store.value)
    }

    @Test
    fun conflictDoesNotRetryOrMarkReleased() = runTest {
        val store = MemoryStore()
        val api = ScriptedApi(409)
        val result = releaseOnce(RestrictionRepositoryImpl(api, store), RecordingPolicies())
        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
        assertEquals(1, api.releaseCalls)
        assertEquals(RestrictionState.ReleasePending.wire, store.value)
    }

    @Test
    fun serverErrorRetriesLater() = runTest {
        val store = MemoryStore()
        val api = ScriptedApi(500)
        val result = releaseOnce(RestrictionRepositoryImpl(api, store), RecordingPolicies())
        assertEquals(androidx.work.ListenableWorker.Result.retry(), result)
        assertEquals(1, api.releaseCalls)
        assertEquals(RestrictionState.ReleasePending.wire, store.value)
    }

    @Test
    fun policiesStillPresentAreNotReportedReleased() = runTest {
        val store = MemoryStore()
        val api = ScriptedApi(204)
        val policies = RecordingPolicies(clear = false)
        val result = releaseOnce(RestrictionRepositoryImpl(api, store), policies)
        assertEquals(androidx.work.ListenableWorker.Result.retry(), result)
        assertEquals(0, api.releaseCalls)
        assertEquals(RestrictionState.ReleasePending.wire, store.value)
    }
}

private class MemoryStore(var value: String? = RestrictionState.ReleasePending.wire) : RestrictionStore {
    override suspend fun imei() = "000000000000001"
    override suspend fun readRestriction() = value
    override suspend fun writeRestriction(state: String) {
        value = state
    }
    override fun restrictionFlow(): Flow<String?> = flowOf(value)
}

private class RecordingPolicies(private val clear: Boolean = true) : DeviceOwnerPolicies {
    var cleared = false
    override fun clear() {
        cleared = true
    }
    override fun areClear() = clear
    override fun applyLock() = Unit
}

private class ScriptedApi(private val code: Int) : Pay2OwnApi {
    var releaseCalls = 0
    override suspend fun loan(imei: String): LoanDto = error("unused")
    override suspend fun startPayment(imei: String, body: StartPaymentDto): StartedDto = error("unused")
    override suspend fun payment(imei: String, reference: String): PaymentDto = error("unused")
    override suspend fun payments(imei: String): PaymentsDto = error("unused")
    override suspend fun pushToken(imei: String, body: PushTokenDto): Response<ResponseBody> = error("unused")
    override suspend fun restriction(imei: String) = RestrictionDto(RestrictionState.ReleasePending.wire)
    override suspend fun releaseConfirmed(imei: String): Response<ResponseBody> {
        releaseCalls++
        val raw = okhttp3.Response.Builder()
            .request(Request.Builder().url("http://localhost/").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("test")
            .build()
        val body = "".toResponseBody("text/plain".toMediaType())
        return if (code in 200..299) Response.success(body, raw) else Response.error(body, raw)
    }
}
