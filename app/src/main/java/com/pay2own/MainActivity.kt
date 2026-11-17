package com.pay2own

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pay2own.domain.EnrollLink
import com.pay2own.domain.parseEnrollLink
import com.pay2own.ui.nav.AppRoot
import com.pay2own.ui.theme.Pay2OwnTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var enroll by mutableStateOf<EnrollLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enroll = readEnroll(intent)
        setContent {
            Pay2OwnTheme {
                AppRoot(enroll = enroll, onEnrollConsumed = { enroll = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        enroll = readEnroll(intent)
    }

    private fun readEnroll(intent: Intent?): EnrollLink? {
        val link = parseEnrollLink(intent?.dataString)
        intent?.data = null
        return link
    }
}
