package com.pay2own.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pay2own.domain.EnrollLink
import com.pay2own.domain.Loan
import com.pay2own.domain.Pay2OwnRepository
import com.pay2own.domain.validImei
import com.pay2own.domain.validToken
import com.pay2own.domain.valueOrNull
import com.pay2own.ui.ScreenError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FieldError { Imei, Token }

data class SetupUiState(
    val imei: String = "",
    val token: String = "",
    val checking: Boolean = false,
    val confirm: Loan? = null,
    val fieldError: FieldError? = null,
    val error: ScreenError? = null,
    val signedOut: Boolean = false,
    val confirmed: Boolean = false,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repository: Pay2OwnRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (repository.hasCredentials() && !repository.isConfirmed()) {
                loadConfirm()
            }
        }
    }

    fun onImei(value: String) {
        _state.update { it.copy(imei = value.filter(Char::isDigit).take(15), fieldError = null) }
    }

    fun onToken(value: String) {
        _state.update { it.copy(token = value.trim().take(64), fieldError = null) }
    }

    fun applyLink(link: EnrollLink) {
        onImei(link.imei)
        onToken(link.token)
        submit()
    }

    fun submit() {
        val current = _state.value
        if (current.checking) return
        val imei = current.imei.trim()
        val token = current.token.trim()
        when {
            !validImei(imei) -> _state.update { it.copy(fieldError = FieldError.Imei) }
            !validToken(token) -> _state.update { it.copy(fieldError = FieldError.Token) }
            else -> viewModelScope.launch {
                _state.update { it.copy(checking = true, error = null, fieldError = null, signedOut = false) }
                repository.saveEnrollment(token, imei)
                _state.update { it.copy(token = "") }
                loadConfirm()
            }
        }
    }

    fun retry() {
        if (_state.value.checking) return
        viewModelScope.launch { loadConfirm() }
    }

    fun confirm() {
        viewModelScope.launch {
            repository.confirmEnrollment()
            _state.update { it.copy(confirmed = true, confirm = null) }
        }
    }

    fun backToForm() {
        _state.update { it.copy(confirm = null, checking = false, error = null) }
    }

    fun reject() {
        viewModelScope.launch {
            repository.clearEnrollment()
            _state.update { SetupUiState(signedOut = true) }
        }
    }

    private suspend fun loadConfirm() {
        _state.update { it.copy(checking = true, error = null) }
        when (val outcome = repository.loan()) {
            else -> {
                val loan = outcome.valueOrNull()
                if (loan != null) {
                    _state.update { it.copy(checking = false, confirm = loan, error = null, signedOut = false) }
                } else if (outcome is com.pay2own.domain.Outcome.SignedOut) {
                    repository.clearEnrollment()
                    _state.update { it.copy(checking = false, confirm = null, signedOut = true, token = "") }
                } else {
                    _state.update {
                        it.copy(
                            checking = false,
                            error = when (outcome) {
                                com.pay2own.domain.Outcome.Offline -> ScreenError.Offline
                                com.pay2own.domain.Outcome.NotFound -> ScreenError.NotFound
                                else -> ScreenError.Failed
                            },
                        )
                    }
                }
            }
        }
    }
}
