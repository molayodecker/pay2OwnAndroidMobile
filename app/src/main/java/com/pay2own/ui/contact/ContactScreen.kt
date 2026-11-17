package com.pay2own.ui.contact

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pay2own.R
import com.pay2own.ui.components.Pay2OwnScaffold
import com.pay2own.ui.components.ScrollColumn
import com.pay2own.ui.components.SecondaryButton
import com.pay2own.ui.support.SupportActions

@Composable
fun ContactRoute(
    onEnterToken: () -> Unit,
    viewModel: ContactViewModel = hiltViewModel(),
) {
    val ready by viewModel.readyForSetup.collectAsStateWithLifecycle()
    LaunchedEffect(ready) { if (ready) onEnterToken() }
    Pay2OwnScaffold(title = stringResource(R.string.contact_title)) { padding ->
        ScrollColumn(padding) {
            Text(
                stringResource(R.string.contact_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = androidx.compose.ui.Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.contact_body), style = MaterialTheme.typography.bodyLarge)
            SupportActions()
            SecondaryButton(stringResource(R.string.contact_new_token), viewModel::enterNewToken)
        }
    }
}
