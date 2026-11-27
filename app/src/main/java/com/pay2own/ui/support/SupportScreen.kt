package com.pay2own.ui.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pay2own.R
import com.pay2own.debug.DebugTools
import com.pay2own.ui.components.AppBottomBar
import com.pay2own.ui.components.AppTab
import com.pay2own.ui.components.Pay2OwnScaffold
import com.pay2own.ui.components.ScrollColumn
import com.pay2own.ui.theme.Navy

@Composable
fun SupportRoute(
    onHome: () -> Unit,
    onPayments: () -> Unit,
    onLoan: () -> Unit,
    onFaq: () -> Unit,
) {
    Pay2OwnScaffold(
        title = "",
        showTopBar = false,
        bottomBar = { AppBottomBar(AppTab.Support, onHome, onPayments, onLoan, onSupport = {}) },
    ) { padding ->
        ScrollColumn(padding) {
            Text(stringResource(R.string.support_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.support_body), style = MaterialTheme.typography.bodyLarge)
            SupportActions(onFaq)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Storefront, contentDescription = null, tint = Navy.copy(alpha = 0.55f), modifier = androidx.compose.ui.Modifier.padding(top = 2.dp))
                Text(stringResource(R.string.support_not_configured), color = Navy.copy(alpha = 0.7f))
            }
            DebugTools()
        }
    }
}

@Composable
fun FaqRoute(onBack: () -> Unit, viewModel: FaqViewModel = hiltViewModel()) {
    Pay2OwnScaffold(title = stringResource(R.string.faq_title), onBack = onBack) { padding ->
        ScrollColumn(padding) {
            viewModel.topics.forEach { topic ->
                val question = when (topic) {
                    FaqTopic.HowToPay -> R.string.faq_pay_q
                    FaqTopic.MissedPayment -> R.string.faq_miss_q
                    FaqTopic.GetHelp -> R.string.faq_help_q
                }
                val answer = when (topic) {
                    FaqTopic.HowToPay -> R.string.faq_pay_a
                    FaqTopic.MissedPayment -> R.string.faq_miss_a
                    FaqTopic.GetHelp -> R.string.faq_help_a
                }
                Text(stringResource(question), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(answer), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
