package com.pay2own.ui.pay

import com.pay2own.domain.Outcome
import com.pay2own.domain.Payment
import com.pay2own.domain.Sleeper
import com.pay2own.domain.StartedPayment
import com.pay2own.domain.sampleLoan
import com.pay2own.ui.FakeRepository
import com.pay2own.ui.MainDispatcherRule
import com.pay2own.ui.ScreenError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PayViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun payNowReachesSuccessAndDoesNotStartTwice() = runTest(main.dispatcher) {
        val repo = FakeRepository()
        repo.paymentScript.add(pending())
        repo.paymentOutcome = success()
        val vm = PayViewModel(repo, ImmediateSleeper())
        runCurrent()

        vm.pay()
        vm.pay()
        advanceUntilIdle()

        assertEquals(1, repo.startCalls)
        assertEquals(PayResult.Success, vm.state.value.result)
        assertEquals("p2o_abc", vm.state.value.reference)
    }

    @Test
    fun pendingPaymentDoesNotStartAnother() = runTest(main.dispatcher) {
        val repo = FakeRepository()
        repo.loanOutcome = Outcome.Fresh(sampleLoan().copy(pendingReference = "p2o_wait"))
        repo.paymentOutcome = pending()
        val vm = PayViewModel(repo, object : Sleeper {
            override suspend fun sleep(millis: Long) = delay(millis)
        })
        runCurrent()
        assertEquals(PayStep.Waiting, vm.state.value.step)
        vm.pay()
        assertEquals(0, repo.startCalls)
    }

    @Test
    fun conflictStaysOnConfirm() = runTest(main.dispatcher) {
        val repo = FakeRepository()
        repo.startOutcome = Outcome.Conflict
        val vm = PayViewModel(repo, ImmediateSleeper())
        runCurrent()
        vm.pay()
        advanceUntilIdle()
        assertEquals(ScreenError.Conflict, vm.state.value.error)
        assertEquals(PayStep.Confirm, vm.state.value.step)
        assertEquals(1, repo.startCalls)
    }

    @Test
    fun rejectedTokenSignsOut() = runTest(main.dispatcher) {
        val repo = FakeRepository()
        repo.startOutcome = Outcome.SignedOut
        val vm = PayViewModel(repo, ImmediateSleeper())
        runCurrent()
        vm.pay()
        advanceUntilIdle()
        assertTrue(vm.state.value.signedOut)
    }

    @Test
    fun timeoutWhenThePaymentStaysPending() = runTest(main.dispatcher) {
        val repo = FakeRepository()
        repo.paymentOutcome = pending()
        val vm = PayViewModel(repo, ImmediateSleeper())
        runCurrent()
        vm.pay()
        advanceUntilIdle()
        assertEquals(PayResult.Timeout, vm.state.value.result)
    }

    @Test
    fun failedPaymentEndsAsFailure() = runTest(main.dispatcher) {
        val repo = FakeRepository()
        repo.startOutcome = Outcome.Fresh(StartedPayment("p2o_abc", "failed"))
        val vm = PayViewModel(repo, ImmediateSleeper())
        runCurrent()
        vm.pay()
        advanceUntilIdle()
        assertEquals(PayResult.Failure, vm.state.value.result)
        vm.retryPayment()
        advanceUntilIdle()
        assertEquals(PayStep.Choose, vm.state.value.step)
        assertNull(vm.state.value.result)
    }

    @Test
    fun offlinePaymentCanBeRetriedFromConfirm() = runTest(main.dispatcher) {
        val repo = FakeRepository()
        repo.startOutcome = Outcome.Offline
        val vm = PayViewModel(repo, ImmediateSleeper())
        runCurrent()
        vm.continueToConfirm()
        vm.pay()
        advanceUntilIdle()
        assertEquals(PayStep.Confirm, vm.state.value.step)
        assertTrue(vm.state.value.offline)
        vm.refresh()
        advanceUntilIdle()
        assertEquals(PayStep.Confirm, vm.state.value.step)
        assertFalse(vm.state.value.offline)
    }
}

private class ImmediateSleeper : Sleeper {
    override suspend fun sleep(millis: Long) = Unit
}

private fun pending() = Outcome.Fresh(Payment("p2o_abc", 30_000, "GHS", "pending", "2026-11-14T10:00:00Z", listOf(1)))

private fun success() = Outcome.Fresh(Payment("p2o_abc", 30_000, "GHS", "success", "2026-11-14T10:00:00Z", listOf(1)))
