package com.pay2own.ui.pay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pay2own.domain.Loan
import com.pay2own.domain.Outcome
import com.pay2own.domain.Pay2OwnRepository
import com.pay2own.domain.Sleeper
import com.pay2own.domain.StartedPayment
import com.pay2own.domain.canPay
import com.pay2own.domain.selectedAmount
import com.pay2own.domain.valueOrNull
import com.pay2own.ui.ScreenError
import com.pay2own.ui.toScreenError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PayStep { Choose, Confirm, Waiting, Done }

enum class PayResult { Success, Failure, Timeout }

data class PayUiState(
    val loading: Boolean = true,
    val loan: Loan? = null,
    val offline: Boolean = false,
    val error: ScreenError? = null,
    val signedOut: Boolean = false,
    val step: PayStep = PayStep.Choose,
    val installmentCount: Int = 1,
    val payBalance: Boolean = false,
    val submitting: Boolean = false,
    val reference: String? = null,
    val result: PayResult? = null,
    val resultAmountMinor: Long = 0,
    val resultCurrency: String = "GHS",
)

@HiltViewModel
class PayViewModel @Inject constructor(
    private val repository: Pay2OwnRepository,
    private val sleeper: Sleeper,
) : ViewModel() {
    private val _state = MutableStateFlow(PayUiState())
    val state: StateFlow<PayUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value.step == PayStep.Waiting) return
        viewModelScope.launch {
            _state.update { it.copy(loading = it.loan == null) }
            val outcome = repository.loan()
            val loan = outcome.valueOrNull()
            if (loan == null) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = outcome.toScreenError(),
                        signedOut = outcome is Outcome.SignedOut,
                        offline = outcome is Outcome.Offline,
                    )
                }
                return@launch
            }
            _state.update {
                it.copy(
                    loading = false,
                    loan = loan,
                    offline = outcome is Outcome.Cached,
                    error = null,
                )
            }
            val pending = loan.pendingReference
            if (pending != null && _state.value.step != PayStep.Done) {
                watch(StartedPayment(pending, "pending"), loan.next?.amountMinor ?: loan.remainingMinor, loan.currency)
            }
        }
    }

    fun selectInstallments(count: Int) {
        _state.update { it.copy(installmentCount = count.coerceAtLeast(1), payBalance = false) }
    }

    fun selectBalance() {
        _state.update { it.copy(payBalance = true) }
    }

    fun continueToConfirm() {
        _state.update { it.copy(step = PayStep.Confirm, error = null) }
    }

    fun backToChoose() {
        if (_state.value.step == PayStep.Waiting) return
        _state.update { it.copy(step = PayStep.Choose, error = null) }
    }

    fun pay() {
        val current = _state.value
        if (current.submitting || current.step == PayStep.Waiting) return
        val loan = current.loan ?: return
        if (!canPay(loan, current.offline)) return
        val amount = selectedAmount(loan, current.installmentCount, current.payBalance)
        _state.update { it.copy(submitting = true, step = PayStep.Waiting, error = null, result = null) }
        viewModelScope.launch {
            when (val outcome = repository.startPayment(current.installmentCount, current.payBalance)) {
                is Outcome.Fresh -> watch(outcome.value, amount, loan.currency)
                is Outcome.SignedOut -> _state.update { it.copy(submitting = false, signedOut = true) }
                is Outcome.Conflict -> _state.update {
                    it.copy(submitting = false, step = PayStep.Confirm, error = ScreenError.Conflict)
                }
                else -> _state.update {
                    it.copy(
                        submitting = false,
                        step = if (outcome is Outcome.BadRequest) PayStep.Choose else PayStep.Confirm,
                        error = outcome.toScreenError(),
                        offline = outcome is Outcome.Offline || it.offline,
                    )
                }
            }
        }
    }

    private suspend fun watch(started: StartedPayment, amount: Long, currency: String) {
        _state.update {
            it.copy(reference = started.reference, submitting = false, step = PayStep.Waiting, error = null)
        }
        when (started.status) {
            "success" -> finish(PayResult.Success, started.reference, amount, currency)
            "failed" -> finish(PayResult.Failure, started.reference, amount, currency)
            else -> poll(started.reference, amount, currency)
        }
    }

    private suspend fun poll(reference: String, amount: Long, currency: String) {
        repeat(MAX_POLLS) {
            sleeper.sleep(POLL_MS)
            when (val outcome = repository.payment(reference)) {
                is Outcome.SignedOut -> {
                    _state.update { it.copy(signedOut = true) }
                    return
                }
                else -> {
                    val payment = outcome.valueOrNull() ?: return@repeat
                    when (payment.status) {
                        "success" -> {
                            finish(PayResult.Success, payment.reference, payment.amountMinor, payment.currency)
                            return
                        }
                        "failed" -> {
                            finish(PayResult.Failure, payment.reference, payment.amountMinor, payment.currency)
                            return
                        }
                    }
                }
            }
        }
        finish(PayResult.Timeout, reference, amount, currency)
    }

    private fun finish(result: PayResult, reference: String, amount: Long, currency: String) {
        _state.update {
            it.copy(
                step = PayStep.Done,
                result = result,
                reference = reference,
                resultAmountMinor = amount,
                resultCurrency = currency,
                submitting = false,
            )
        }
    }

    private companion object {
        const val MAX_POLLS = 30
        const val POLL_MS = 3_000L
    }
}
