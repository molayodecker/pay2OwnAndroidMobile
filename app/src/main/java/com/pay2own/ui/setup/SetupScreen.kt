package com.pay2own.ui.setup

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pay2own.R
import com.pay2own.domain.EnrollLink
import com.pay2own.domain.displayPhone
import com.pay2own.ui.components.LabeledValue
import com.pay2own.ui.components.LoadingState
import com.pay2own.ui.components.MessageState
import com.pay2own.ui.components.Pay2OwnScaffold
import com.pay2own.ui.components.PrimaryButton
import com.pay2own.ui.components.ScrollColumn
import com.pay2own.ui.components.SecondaryButton
import com.pay2own.ui.components.SectionTitle
import com.pay2own.ui.components.errorBody
import com.pay2own.ui.components.errorTitle
import com.pay2own.ui.components.providerName

@Composable
fun SetupRoute(
    enroll: EnrollLink?,
    onConfirmed: () -> Unit,
    onRejected: () -> Unit,
    onConsumed: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(enroll) {
        if (enroll != null) {
            viewModel.applyLink(enroll)
            onConsumed()
        }
    }
    LaunchedEffect(state.confirmed) {
        if (state.confirmed) onConfirmed()
    }
    LaunchedEffect(state.signedOut) {
        if (state.signedOut) onRejected()
    }
    SetupScreen(
        state = state,
        onImei = viewModel::onImei,
        onToken = viewModel::onToken,
        onSubmit = viewModel::submit,
        onRetry = viewModel::retry,
        onScanned = viewModel::applyLink,
        onConfirm = viewModel::confirm,
        onReject = viewModel::reject,
    )
}

@Composable
fun SetupScreen(
    state: SetupUiState,
    onImei: (String) -> Unit,
    onToken: (String) -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onScanned: (EnrollLink) -> Unit,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
) {
    var scanning by rememberSaveable { mutableStateOf(false) }
    Pay2OwnScaffold(title = stringResource(R.string.setup_title)) { padding ->
        when {
            state.checking -> LoadingState(padding)
            state.confirm != null -> ConfirmIdentity(padding, state, onConfirm, onReject)
            scanning -> EnrollScanner(
                padding = padding,
                onLink = {
                    scanning = false
                    onScanned(it)
                },
                onClose = { scanning = false },
            )
            else -> EnrollmentForm(padding, state, onImei, onToken, onSubmit, onRetry, onScan = { scanning = true })
        }
    }
}

@Composable
private fun EnrollmentForm(
    padding: PaddingValues,
    state: SetupUiState,
    onImei: (String) -> Unit,
    onToken: (String) -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onScan: () -> Unit,
) {
    var tokenVisible by rememberSaveable { mutableStateOf(false) }
    ScrollColumn(padding) {
        Text(stringResource(R.string.setup_body), style = MaterialTheme.typography.bodyLarge)
        SecondaryButton(stringResource(R.string.setup_scan), onScan)
        OutlinedTextField(
            value = state.imei,
            onValueChange = onImei,
            label = { Text(stringResource(R.string.setup_imei)) },
            supportingText = {
                if (state.fieldError == FieldError.Imei) Text(stringResource(R.string.setup_imei_error))
            },
            isError = state.fieldError == FieldError.Imei,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.token,
            onValueChange = onToken,
            label = { Text(stringResource(R.string.setup_token)) },
            supportingText = {
                if (state.fieldError == FieldError.Token) Text(stringResource(R.string.setup_token_error))
            },
            isError = state.fieldError == FieldError.Token,
            visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { tokenVisible = !tokenVisible }) {
                    Text(stringResource(if (tokenVisible) R.string.setup_hide_token else R.string.setup_show_token))
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.error != null) {
            Text(errorTitle(state.error), color = MaterialTheme.colorScheme.error)
            Text(errorBody(state.error))
            SecondaryButton(stringResource(R.string.retry), onRetry)
        }
        PrimaryButton(stringResource(R.string.setup_check), onSubmit)
    }
}

@Composable
private fun ConfirmIdentity(
    padding: PaddingValues,
    state: SetupUiState,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
) {
    val loan = state.confirm ?: return
    ScrollColumn(padding) {
        SectionTitle(stringResource(R.string.setup_confirm_title))
        Text(stringResource(R.string.setup_confirm_body))
        LabeledValue(stringResource(R.string.setup_name), loan.customerName)
        LabeledValue(stringResource(R.string.setup_momo), displayPhone(loan.phoneMasked))
        LabeledValue(stringResource(R.string.pay_momo_provider), providerName(loan.provider))
        PrimaryButton(stringResource(R.string.setup_yes), onConfirm)
        SecondaryButton(stringResource(R.string.setup_no), onReject)
    }
}
