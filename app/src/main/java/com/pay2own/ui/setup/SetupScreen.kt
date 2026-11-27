package com.pay2own.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pay2own.R
import com.pay2own.domain.EnrollLink
import com.pay2own.domain.initials
import com.pay2own.domain.spacedPhone
import com.pay2own.ui.components.LoadingState
import com.pay2own.ui.components.MessageState
import com.pay2own.ui.components.Pay2OwnScaffold
import com.pay2own.ui.components.PrimaryButton
import com.pay2own.ui.components.SecondaryButton
import com.pay2own.ui.components.errorBody
import com.pay2own.ui.components.errorTitle
import com.pay2own.ui.components.providerName
import com.pay2own.ui.theme.Cyan
import com.pay2own.ui.theme.Ice
import com.pay2own.ui.theme.Navy
import com.pay2own.ui.theme.NavyMuted

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
        onBackToForm = viewModel::backToForm,
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
    onBackToForm: () -> Unit,
) {
    var scanning by rememberSaveable { mutableStateOf(false) }
    when {
        state.checking -> Pay2OwnScaffold(title = stringResource(R.string.setup_title)) { LoadingState(it) }
        state.confirm != null -> ConfirmIdentity(state, onConfirm, onReject, onBackToForm)
        scanning -> EnrollScanner(
            padding = PaddingValues(),
            onLink = {
                scanning = false
                onScanned(it)
            },
            onClose = { scanning = false },
        )
        else -> EnrollmentForm(state, onImei, onToken, onSubmit, onRetry, onScan = { scanning = true })
    }
}

@Composable
private fun EnrollmentForm(
    state: SetupUiState,
    onImei: (String) -> Unit,
    onToken: (String) -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onScan: () -> Unit,
) {
    var tokenVisible by rememberSaveable { mutableStateOf(false) }
    Pay2OwnScaffold(title = "", showTopBar = false) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(R.string.setup_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
                StepLabel(1)
                Text(stringResource(R.string.setup_body), style = MaterialTheme.typography.bodyLarge)
                SecondaryButton(stringResource(R.string.setup_scan), Icons.Filled.QrCodeScanner, onScan)
                OrDivider()
                OutlinedTextField(
                    value = state.imei,
                    onValueChange = onImei,
                    label = { Text(stringResource(R.string.setup_imei)) },
                    placeholder = { Text(stringResource(R.string.setup_imei_hint)) },
                    supportingText = {
                        if (state.fieldError == FieldError.Imei) Text(stringResource(R.string.setup_imei_error))
                    },
                    isError = state.fieldError == FieldError.Imei,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.token,
                    onValueChange = onToken,
                    label = { Text(stringResource(R.string.setup_token)) },
                    placeholder = { Text(stringResource(R.string.setup_token_hint)) },
                    supportingText = {
                        if (state.fieldError == FieldError.Token) Text(stringResource(R.string.setup_token_error))
                    },
                    isError = state.fieldError == FieldError.Token,
                    visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { tokenVisible = !tokenVisible }) {
                            Icon(
                                if (tokenVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = stringResource(
                                    if (tokenVisible) R.string.setup_hide_token else R.string.setup_show_token,
                                ),
                                tint = Navy,
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.error != null) {
                    Text(errorTitle(state.error), color = MaterialTheme.colorScheme.error)
                    Text(errorBody(state.error))
                    SecondaryButton(stringResource(R.string.retry), onRetry)
                }
            }
            PrimaryButton(stringResource(R.string.setup_check), onSubmit)
            Box(Modifier.padding(bottom = 12.dp))
        }
    }
}

@Composable
private fun ConfirmIdentity(
    state: SetupUiState,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onBack: () -> Unit,
) {
    val loan = state.confirm ?: return
    Pay2OwnScaffold(title = stringResource(R.string.setup_confirm_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StepLabel(2)
                Text(stringResource(R.string.setup_confirm_body), style = MaterialTheme.typography.bodyLarge)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Ice),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(52.dp).clip(CircleShape).background(Navy),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(initials(loan.customerName), color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(stringResource(R.string.setup_name), color = NavyMuted, style = MaterialTheme.typography.labelLarge)
                            Text(loan.customerName, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    HorizontalDivider(color = androidx.compose.ui.graphics.Color.White)
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.setup_momo), color = NavyMuted, style = MaterialTheme.typography.labelLarge)
                        Text(spacedPhone(loan.phoneMasked), style = MaterialTheme.typography.titleLarge)
                    }
                    HorizontalDivider(color = androidx.compose.ui.graphics.Color.White)
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.pay_momo_provider), color = NavyMuted, style = MaterialTheme.typography.labelLarge)
                        Text(providerName(loan.provider), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            PrimaryButton(stringResource(R.string.setup_yes), onConfirm)
            TextButton(onClick = onReject, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(stringResource(R.string.setup_no), color = Cyan, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun StepLabel(step: Int) {
    Text(
        stringResource(R.string.setup_step, step, 2),
        color = Cyan,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Navy.copy(alpha = 0.15f))
        Text(stringResource(R.string.setup_or), color = NavyMuted, textAlign = TextAlign.Center)
        HorizontalDivider(modifier = Modifier.weight(1f), color = Navy.copy(alpha = 0.15f))
    }
}

