package com.pay2own.data

import com.pay2own.data.api.Pay2OwnApi
import com.pay2own.data.local.LocalStore
import com.pay2own.domain.Outcome
import com.pay2own.domain.ReleaseResult
import com.pay2own.domain.RestrictionRepository
import com.pay2own.domain.RestrictionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface RestrictionStore {
    suspend fun imei(): String?
    suspend fun readRestriction(): String?
    suspend fun writeRestriction(state: String)
    fun restrictionFlow(): Flow<String?>
}

@Singleton
class LocalRestrictionStore @Inject constructor(
    private val store: LocalStore,
) : RestrictionStore {
    override suspend fun imei() = store.readCredentials()?.imei
    override suspend fun readRestriction() = store.readRestriction()
    override suspend fun writeRestriction(state: String) = store.writeRestriction(state)
    override fun restrictionFlow() = store.restrictionFlow()
}

@Singleton
class RestrictionRepositoryImpl @Inject constructor(
    private val api: Pay2OwnApi,
    private val store: RestrictionStore,
) : RestrictionRepository {
    override fun observe(): Flow<RestrictionState?> =
        store.restrictionFlow().map { RestrictionState.parse(it) }

    override suspend fun refresh(): Outcome<RestrictionState> {
        val imei = store.imei() ?: return Outcome.SignedOut
        return try {
            val state = RestrictionState.parse(api.restriction(imei).state)
                ?: return keepCache(Outcome.Failed)
            store.writeRestriction(state.wire)
            Outcome.Fresh(state)
        } catch (error: HttpException) {
            error.response()?.errorBody()?.close()
            if (error.code() == 401 || error.code() == 403) Outcome.SignedOut
            else keepCache(Outcome.Failed)
        } catch (_: IOException) {
            keepCache(Outcome.Offline)
        }
    }

    override suspend fun cached(): RestrictionState? = store.readRestriction()?.let(RestrictionState::parse)

    override suspend fun confirmRelease(): ReleaseResult {
        val imei = store.imei() ?: return ReleaseResult.Retry
        return try {
            val response = api.releaseConfirmed(imei)
            response.body()?.close()
            response.errorBody()?.close()
            when (response.code()) {
                204 -> {
                    store.writeRestriction(RestrictionState.Released.wire)
                    ReleaseResult.Done
                }
                409 -> ReleaseResult.NotPending
                else -> ReleaseResult.Retry
            }
        } catch (_: IOException) {
            ReleaseResult.Retry
        }
    }

    private suspend fun keepCache(fallback: Outcome<RestrictionState>): Outcome<RestrictionState> {
        val cached = cached() ?: return fallback
        return Outcome.Cached(cached)
    }
}
