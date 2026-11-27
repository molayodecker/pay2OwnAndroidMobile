package com.pay2own.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pay2own.domain.Pay2OwnRepository
import com.pay2own.domain.Payment
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

data class HistoryUiState(
    val loading: Boolean = true,
    val payments: List<Payment> = emptyList(),
    val offline: Boolean = false,
    val error: ScreenError? = null,
    val signedOut: Boolean = false,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: Pay2OwnRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = it.payments.isEmpty()) }
            val outcome = repository.payments()
            val payments = outcome.valueOrNull()
            if (payments != null) {
                _state.update {
                    it.copy(
                        loading = false,
                        payments = payments,
                        offline = outcome is com.pay2own.domain.Outcome.Cached,
                        error = null,
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        loading = false,
                        error = outcome.toScreenError(),
                        signedOut = outcome is com.pay2own.domain.Outcome.SignedOut,
                        offline = outcome is com.pay2own.domain.Outcome.Offline,
                    )
                }
            }
        }
    }
}
