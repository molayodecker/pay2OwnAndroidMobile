package com.pay2own.ui.support

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pay2own.R
import com.pay2own.debug.DebugTools
import com.pay2own.ui.components.LinkRow
import com.pay2own.ui.components.Pay2OwnScaffold
import com.pay2own.ui.components.ScrollColumn
import com.pay2own.ui.components.SecondaryButton

@Composable
fun SupportRoute(
    onBack: () -> Unit,
    onFaq: () -> Unit,
    onCleared: () -> Unit,
    viewModel: SupportViewModel = hiltViewModel(),
) {
    val cleared by viewModel.cleared.collectAsStateWithLifecycle()
    LaunchedEffect(cleared) { if (cleared) onCleared() }
    Pay2OwnScaffold(title = stringResource(R.string.support_title), onBack = onBack) { padding ->
        ScrollColumn(padding) {
            Text(stringResource(R.string.support_body), style = MaterialTheme.typography.bodyLarge)
            SupportActions()
            LinkRow(stringResource(R.string.faq), onFaq)
            SecondaryButton(stringResource(R.string.not_my_phone), viewModel::clearEnrollment)
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
