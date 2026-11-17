package com.pay2own.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pay2own.domain.Pay2OwnRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val repository: Pay2OwnRepository,
) : ViewModel() {
    private val _cleared = MutableStateFlow(false)
    val cleared: StateFlow<Boolean> = _cleared.asStateFlow()

    fun clearEnrollment() {
        viewModelScope.launch {
            repository.clearEnrollment()
            _cleared.value = true
        }
    }
}

enum class FaqTopic { HowToPay, MissedPayment, GetHelp }

@HiltViewModel
class FaqViewModel @Inject constructor() : ViewModel() {
    val topics: List<FaqTopic> = FaqTopic.entries
}
