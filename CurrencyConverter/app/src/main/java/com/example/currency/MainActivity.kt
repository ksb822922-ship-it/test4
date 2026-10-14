package com.example.currency

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.example.currency.ui.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = hiltViewModel()
            val theme by vm.theme.collectAsStateWithLifecycle()
            val dark = when (theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
            AppTheme(dark) {
                AuroraBackground(dark) {
                    val nav = rememberNavController()
                    NavHost(nav, "home") {
                        composable("home") { HomeScreen(vm, { nav.navigate("history") }, { nav.navigate("settings") }) }
                        composable("history") { HistoryScreen(vm) { nav.popBackStack() } }
                        composable("settings") { SettingsScreen(vm) { nav.popBackStack() } }
                    }
                }
            }
        }
    }
}
