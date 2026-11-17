package com.pay2own.ui.restriction

import com.pay2own.domain.DeviceOwnerPolicies
import com.pay2own.domain.Outcome
import com.pay2own.domain.PaidOffCopy
import com.pay2own.domain.ReleaseResult
import com.pay2own.domain.ReleaseWorkScheduler
import com.pay2own.domain.RestrictionRepository
import com.pay2own.domain.RestrictionState
import com.pay2own.domain.valueOrNull
import com.pay2own.ui.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestrictionViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun notEnrolledSaysPaidOff() = runTest(main.dispatcher) {
        val screen = open(Outcome.Fresh(RestrictionState.NotEnrolled))
        advanceUntilIdle()
        assertEquals(PaidOffCopy.PaidOff, screen.vm.state.value.copy)
        assertEquals(0, screen.policies.locks)
        assertEquals(0, screen.scheduler.enqueued)
    }

    @Test
    fun activeFromTheServerKeepsRestrictions() = runTest(main.dispatcher) {
        val screen = open(Outcome.Fresh(RestrictionState.Active))
        advanceUntilIdle()
        assertEquals(PaidOffCopy.StillRestricted, screen.vm.state.value.copy)
        assertEquals(1, screen.policies.locks)
        assertEquals(0, screen.scheduler.enqueued)
    }

    @Test
    fun cachedActiveDoesNotLock() = runTest(main.dispatcher) {
        val screen = open(Outcome.Offline, cached = RestrictionState.Active)
        advanceUntilIdle()
        assertEquals(PaidOffCopy.StillRestricted, screen.vm.state.value.copy)
        assertEquals(0, screen.policies.locks)
        assertEquals(0, screen.scheduler.enqueued)
    }

    @Test
    fun releasePendingFinishesUnlock() = runTest(main.dispatcher) {
        val screen = open(Outcome.Fresh(RestrictionState.ReleasePending))
        advanceUntilIdle()
        assertEquals(PaidOffCopy.FinishingUnlock, screen.vm.state.value.copy)
        assertEquals(0, screen.policies.locks)
        assertEquals(1, screen.scheduler.enqueued)
    }

    @Test
    fun releasedSaysFullyYours() = runTest(main.dispatcher) {
        val screen = open(Outcome.Fresh(RestrictionState.Released))
        advanceUntilIdle()
        assertEquals(PaidOffCopy.FullyYours, screen.vm.state.value.copy)
        assertEquals(0, screen.policies.locks)
        assertEquals(0, screen.scheduler.enqueued)
    }

    private fun open(result: Outcome<RestrictionState>, cached: RestrictionState? = null): Screen {
        val policies = RecordingPolicies()
        val scheduler = RecordingScheduler()
        val vm = RestrictionViewModel(FakeRestrictionRepository(result, cached), policies, scheduler)
        return Screen(vm, policies, scheduler)
    }

    private class Screen(
        val vm: RestrictionViewModel,
        val policies: RecordingPolicies,
        val scheduler: RecordingScheduler,
    )
}

private class FakeRestrictionRepository(
    private val result: Outcome<RestrictionState>,
    private val cachedState: RestrictionState?,
) : RestrictionRepository {
    private val states = MutableStateFlow<RestrictionState?>(null)
    override fun observe(): Flow<RestrictionState?> = states
    override suspend fun refresh(): Outcome<RestrictionState> {
        result.valueOrNull()?.let { states.value = it }
        return result
    }
    override suspend fun cached(): RestrictionState? = cachedState ?: states.value
    override suspend fun enrolledImei(): String? = "000000000000001"
    override suspend fun confirmRelease(): ReleaseResult = ReleaseResult.Done
}

private class RecordingPolicies : DeviceOwnerPolicies {
    var locks = 0
    override fun clear() = Unit
    override fun areClear() = true
    override fun applyLock() {
        locks++
    }
}

private class RecordingScheduler : ReleaseWorkScheduler {
    var enqueued = 0
    override suspend fun enqueue() {
        enqueued++
    }
    override fun cancel() = Unit
}
