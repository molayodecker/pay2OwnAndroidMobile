package com.pay2own.ui.contact

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
class ContactViewModel @Inject constructor(
    private val repository: Pay2OwnRepository,
) : ViewModel() {
    private val _readyForSetup = MutableStateFlow(false)
    val readyForSetup: StateFlow<Boolean> = _readyForSetup.asStateFlow()

    fun enterNewToken() {
        viewModelScope.launch {
            repository.clearEnrollment()
            _readyForSetup.value = true
        }
    }
}
