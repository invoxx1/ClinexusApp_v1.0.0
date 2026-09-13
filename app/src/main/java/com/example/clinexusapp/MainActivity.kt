package com.example.clinexusapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.clinexusapp.navigation.SetupNavGraph
import com.example.clinexusapp.ui.theme.ClinexusAppTheme
import com.example.clinexusapp.util.AppNavigationRequests
import com.example.clinexusapp.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        AppNavigationRequests.handleIntent(intent)
        
        setContent {
            ClinexusAppTheme {
                val navController = rememberNavController()
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SetupNavGraph(
                    navController = navController,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AppNavigationRequests.handleIntent(intent)
    }
}
