package com.pay2own.domain

import kotlinx.coroutines.flow.Flow

enum class RestrictionState {
    NotEnrolled,
    Active,
    ReleasePending,
    Released,
    ;

    val wire: String
        get() = when (this) {
            NotEnrolled -> "not_enrolled"
            Active -> "active"
            ReleasePending -> "release_pending"
            Released -> "released"
        }

    fun paidOffCopy(): PaidOffCopy = when (this) {
        NotEnrolled -> PaidOffCopy.PaidOff
        Active -> PaidOffCopy.StillRestricted
        ReleasePending -> PaidOffCopy.FinishingUnlock
        Released -> PaidOffCopy.FullyYours
    }

    companion object {
        fun parse(raw: String?): RestrictionState? = when (raw) {
            "not_enrolled" -> NotEnrolled
            "active" -> Active
            "release_pending" -> ReleasePending
            "released" -> Released
            else -> null
        }
    }
}

/** What the paid-off screen says. Active is the only state that may keep restrictions on. */
enum class PaidOffCopy {
    PaidOff,
    StillRestricted,
    FinishingUnlock,
    FullyYours,
}

enum class ReleaseResult {
    Done,
    NotPending,
    Retry,
}

interface RestrictionRepository {
    fun observe(): Flow<RestrictionState?>
    suspend fun refresh(): Outcome<RestrictionState>
    suspend fun cached(): RestrictionState?
    suspend fun enrolledImei(): String?
    suspend fun confirmRelease(): ReleaseResult
}

/** Device-owner financing controls. Unenrolled phones have none to clear. */
interface DeviceOwnerPolicies {
    fun clear()
    fun areClear(): Boolean

    /** Only call this for a restriction state just returned by the server. */
    fun applyLock()
}

interface ReleaseWorkScheduler {
    suspend fun enqueue()
    fun cancel()
}
