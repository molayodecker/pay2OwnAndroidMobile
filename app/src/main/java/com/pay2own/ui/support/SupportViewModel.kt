package com.pay2own.ui.support

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

enum class FaqTopic { HowToPay, MissedPayment, GetHelp }

@HiltViewModel
class FaqViewModel @Inject constructor() : ViewModel() {
    val topics: List<FaqTopic> = FaqTopic.entries
}
