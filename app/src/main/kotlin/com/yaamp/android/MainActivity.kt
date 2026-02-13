package com.yaamp.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.yaamp.android.ui.theme.YaampTheme
import com.yaamp.android.ui.viewmodel.MainViewModel
import com.yaamp.android.ui.screens.MainScreen
import com.yaamp.android.ui.screens.AuthScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleAuthIntent(intent)

        // Можете вставить токен здесь для быстрого тестирования:
        // viewModel.setAuthToken("REMOVED_SECRET")

        setContent {
            YaampTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

                    if (isAuthenticated) {
                        MainScreen(viewModel = viewModel)
                    } else {
                        AuthScreen(
                            onTokenSubmit = { token ->
                                viewModel.setAuthToken(token)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent) {
        val data = intent.data ?: return
        val token = extractAccessToken(data)
        if (!token.isNullOrBlank()) {
            viewModel.setAuthToken(token)
        }
    }

    private fun extractAccessToken(uri: android.net.Uri): String? {
        val tokenFromFragment = uri.fragment
            ?.let { parseParams(it)["access_token"] }
            ?.takeIf { it.isNotBlank() }

        return tokenFromFragment ?: uri.getQueryParameter("access_token")
    }

    private fun parseParams(fragment: String): Map<String, String> {
        if (fragment.isBlank()) return emptyMap()
        return fragment.split("&")
            .mapNotNull { part ->
                val keyValue = part.split("=", limit = 2)
                if (keyValue.size < 2) return@mapNotNull null
                val key = java.net.URLDecoder.decode(keyValue[0], Charsets.UTF_8.name())
                val value = java.net.URLDecoder.decode(keyValue[1], Charsets.UTF_8.name())
                key to value
            }
            .toMap()
    }
}
