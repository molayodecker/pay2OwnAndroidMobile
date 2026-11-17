package com.pay2own.ui.pay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.pay2own.debug.TestHostActivity
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.pay2own.R
import com.pay2own.domain.Installment
import com.pay2own.domain.Loan
import com.pay2own.ui.theme.Pay2OwnTheme
import org.junit.Rule
import org.junit.Test

class PayScreenTest {
    @get:Rule val compose = createAndroidComposeRule<TestHostActivity>()

    @Test
    fun paymentFlowShowsAmountThenWaitingThenSuccess() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var state by mutableStateOf(
            PayUiState(loading = false, loan = previewLoan(), step = PayStep.Choose),
        )
        compose.setContent {
            Pay2OwnTheme {
                PayScreen(
                    state = state,
                    onRetry = {},
                    onSelectCount = {},
                    onSelectBalance = { state = state.copy(payBalance = true) },
                    onContinue = { state = state.copy(step = PayStep.Confirm) },
                    onBackToChoose = {},
                    onPay = { state = state.copy(step = PayStep.Waiting, reference = "p2o_test") },
                    onDone = {},
                    onReceipt = {},
                )
            }
        }

        compose.onNodeWithText(context.getString(R.string.pay_this)).performClick()
        compose.onNodeWithText(context.getString(R.string.continue_action)).performClick()
        compose.onNodeWithText("055•••4987").assertIsDisplayed()
        compose.onAllNodesWithText("0551234987").assertCountEquals(0)
        compose.onNodeWithText(context.getString(R.string.pay_approve)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.pay_submit, "GHS 300.00")).performClick()
        compose.onNodeWithText(context.getString(R.string.pay_waiting)).assertIsDisplayed()
        compose.onAllNodesWithText(context.getString(R.string.pay_submit, "GHS 300.00")).assertCountEquals(0)

        state = state.copy(
            step = PayStep.Done,
            result = PayResult.Success,
            reference = "p2o_test",
            resultAmountMinor = 30_000,
            resultCurrency = "GHS",
        )
        compose.onNodeWithText(context.getString(R.string.pay_success)).assertIsDisplayed()
    }
}

private fun previewLoan() = Loan(
    customerName = "Ama Mensah",
    phoneMasked = "055•••4987",
    provider = "mtn",
    model = "Samsung Galaxy A15",
    currency = "GHS",
    priceMinor = 200_000,
    depositMinor = 80_000,
    remainingMinor = 120_000,
    paidMinor = 80_000,
    startedOn = "2026-10-01",
    loanStatus = "active",
    accountStatus = "up_to_date",
    installmentCount = 4,
    paidCount = 0,
    next = Installment(1, "2026-11-20", 30_000, "unpaid"),
    pendingReference = null,
    schedule = (1..4).map { Installment(it, "2026-11-20", 30_000, "unpaid") },
    terms = "Pay2Own payment terms",
)
