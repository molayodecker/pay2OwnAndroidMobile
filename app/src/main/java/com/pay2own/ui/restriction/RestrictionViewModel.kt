package com.pay2own.ui.restriction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pay2own.domain.DeviceOwnerPolicies
import com.pay2own.domain.Outcome
import com.pay2own.domain.PaidOffCopy
import com.pay2own.domain.ReleaseWorkScheduler
import com.pay2own.domain.RestrictionRepository
import com.pay2own.domain.RestrictionState
import com.pay2own.domain.valueOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RestrictionUiState(
    val state: RestrictionState = RestrictionState.NotEnrolled,
    val copy: PaidOffCopy = PaidOffCopy.PaidOff,
)

@HiltViewModel
class RestrictionViewModel @Inject constructor(
    private val repository: RestrictionRepository,
    private val policies: DeviceOwnerPolicies,
    private val scheduler: ReleaseWorkScheduler,
) : ViewModel() {
    private val _state = MutableStateFlow(RestrictionUiState())
    val state: StateFlow<RestrictionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observe().collect { state ->
                if (state == null) return@collect
                _state.update { RestrictionUiState(state, state.paidOffCopy()) }
                if (state == RestrictionState.ReleasePending) scheduler.enqueue()
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val outcome = repository.refresh()
            // Cached "active" must not keep or apply a lock. Only a fresh server response can.
            if (outcome is Outcome.Fresh && outcome.value == RestrictionState.Active) {
                policies.applyLock()
            }
            if (outcome.valueOrNull() == null) {
                val state = repository.cached() ?: RestrictionState.NotEnrolled
                _state.update { RestrictionUiState(state, state.paidOffCopy()) }
            }
        }
    }
}
