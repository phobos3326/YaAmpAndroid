package com.yaamp.android

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
}
