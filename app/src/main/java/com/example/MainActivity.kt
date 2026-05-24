package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme(darkTheme = false) {
                val navController = rememberNavController()
                
                NavHost(
                    navController = navController,
                    startDestination = "login",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("login") {
                        LoginScreen(onLoginSuccess = { name ->
                            navController.navigate("chat/$name") {
                                popUpTo("login") { inclusive = true }
                            }
                        })
                    }
                    composable("chat/{name}") { backStackEntry ->
                        val name = backStackEntry.arguments?.getString("name") ?: "Öğrenci"
                        ChatScreen(userName = name)
                    }
                }
            }
        }
    }
}
