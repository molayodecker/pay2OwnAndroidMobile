package com.pay2own.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pay2own.domain.Loan
import com.pay2own.domain.Pay2OwnRepository
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

data class HomeUiState(
    val loading: Boolean = true,
    val loan: Loan? = null,
    val offline: Boolean = false,
    val error: ScreenError? = null,
    val signedOut: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: Pay2OwnRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = it.loan == null, signedOut = false) }
            val outcome = repository.loan()
            val loan = outcome.valueOrNull()
            if (loan != null) {
                _state.update {
                    it.copy(
                        loading = false,
                        loan = loan,
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

    fun registerPushToken(token: String) {
        viewModelScope.launch { repository.registerPushToken(token) }
    }
}
