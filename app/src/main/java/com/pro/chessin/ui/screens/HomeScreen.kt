package com.pro.chessin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.pro.chessin.ui.screens.viewmodels.HomeViewModel

/**
 * Home screen with navigation buttons to major features.
 */
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    var nativeMessage by remember { mutableStateOf("Engine not initialized") }

    LaunchedEffect(Unit) {
        try {
            nativeMessage = "JNI library loaded successfully"
        } catch (e: Exception) {
            nativeMessage = "JNI Error: ${e.message}"
        }
    }

    val diMessage = viewModel.getDiTestMessage()

    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val authLabel = if (currentUser != null && !currentUser.isAnonymous) {
        "Account (${currentUser.email ?: "Signed In"})"
    } else {
        "Sign In / Account"
    }

    val items = listOf(
        "Analysis Board" to "analysis",
        "Analysis Dashboard" to "analysis_dashboard",
        "Repertoire" to "repertoire",
        "Puzzles" to "puzzles",
        authLabel to "sign_in",
        "Chessin PRO ⭐" to "paywall",
        "Settings" to "settings",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Chessin",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Hilt: $diMessage\nJNI: $nativeMessage",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.weight(1f))

        items.forEach { (label, route) ->
            Button(
                onClick = { navController.navigate(route) },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                Text(text = label)
            }
        }
    }
}
