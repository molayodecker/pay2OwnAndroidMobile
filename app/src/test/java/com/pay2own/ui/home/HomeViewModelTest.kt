package com.pay2own.ui.home

import com.pay2own.domain.Outcome
import com.pay2own.domain.sampleLoan
import com.pay2own.ui.FakeRepository
import com.pay2own.ui.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun showsTheLoan() = runTest(main.dispatcher) {
        val vm = HomeViewModel(FakeRepository())
        advanceUntilIdle()
        assertEquals("Ama Mensah", vm.state.value.loan?.customerName)
        assertEquals("up_to_date", vm.state.value.loan?.accountStatus)
        assertFalse(vm.state.value.offline)
    }

    @Test
    fun cachedLoanIsOffline() = runTest(main.dispatcher) {
        val repo = FakeRepository()
        repo.loanOutcome = Outcome.Cached(sampleLoan())
        val vm = HomeViewModel(repo)
        advanceUntilIdle()
        assertTrue(vm.state.value.offline)
        assertEquals("Ama Mensah", vm.state.value.loan?.customerName)
    }

    @Test
    fun rejectedTokenSignsOut() = runTest(main.dispatcher) {
        val repo = FakeRepository()
        repo.loanOutcome = Outcome.SignedOut
        val vm = HomeViewModel(repo)
        advanceUntilIdle()
        assertTrue(vm.state.value.signedOut)
    }

    @Test
    fun noCacheShowsOffline() = runTest(main.dispatcher) {
        val repo = FakeRepository()
        repo.loanOutcome = Outcome.Offline
        val vm = HomeViewModel(repo)
        advanceUntilIdle()
        assertTrue(vm.state.value.offline)
        assertEquals(null, vm.state.value.loan)
    }
}
