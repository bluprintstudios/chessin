package com.pro.chessin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pro.chessin.ui.screens.AnalysisDashboardScreen
import com.pro.chessin.ui.screens.AnalysisScreen
import com.pro.chessin.ui.screens.HomeScreen
import com.pro.chessin.ui.screens.PuzzlesScreen
import com.pro.chessin.ui.screens.RepertoireScreen
import com.pro.chessin.ui.screens.SettingsScreen
import com.pro.chessin.ui.theme.ChessinTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChessinTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") { HomeScreen(navController = navController) }
                        composable("analysis") { AnalysisScreen() }
                        composable("analysis_dashboard") { AnalysisDashboardScreen() }
                        composable("repertoire") { RepertoireScreen() }
                        composable("puzzles") { PuzzlesScreen() }
                        composable("settings") { SettingsScreen() }
                    }
                }
            }
        }
    }
}
