package com.pay2own.ui.support

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pay2own.R
import com.pay2own.ui.components.PrimaryButton
import com.pay2own.ui.components.SecondaryButton

@Composable
fun SupportActions() {
    val context = LocalContext.current
    val phone = stringResource(R.string.support_phone)
    val whatsapp = stringResource(R.string.support_whatsapp)
    if (phone.isNotBlank()) {
        PrimaryButton(stringResource(R.string.call_support), onClick = {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
        })
    }
    if (whatsapp.isNotBlank()) {
        SecondaryButton(stringResource(R.string.whatsapp_support)) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$whatsapp")))
        }
    }
    if (phone.isBlank() && whatsapp.isBlank()) {
        Text(stringResource(R.string.support_not_configured), style = MaterialTheme.typography.bodyLarge)
    }
}
