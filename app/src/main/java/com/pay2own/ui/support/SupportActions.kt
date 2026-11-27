package com.pay2own.ui.support

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pay2own.R
import com.pay2own.ui.theme.Cyan
import com.pay2own.ui.theme.Ice
import com.pay2own.ui.theme.Navy
import com.pay2own.ui.theme.White

@Composable
fun SupportActions(onFaq: (() -> Unit)? = null) {
    val context = LocalContext.current
    val phone = stringResource(R.string.support_phone)
    val whatsapp = stringResource(R.string.support_whatsapp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ice),
    ) {
        if (phone.isNotBlank()) {
            SupportRow(Icons.Filled.Call, stringResource(R.string.call_support)) {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
            }
            HorizontalDivider(color = White)
        }
        if (whatsapp.isNotBlank()) {
            SupportRow(Icons.Filled.Chat, stringResource(R.string.whatsapp_support)) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$whatsapp")))
            }
            HorizontalDivider(color = White)
        }
        if (onFaq != null) {
            SupportRow(Icons.Filled.HelpOutline, stringResource(R.string.faq), onFaq)
        }
    }
}

@Composable
private fun SupportRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(Cyan.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
        }
        Text(label, color = Navy, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Navy.copy(alpha = 0.4f))
    }
}
