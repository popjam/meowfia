package com.meowfia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.meowfia.app.ui.navigation.GameNavGraph
import com.meowfia.app.ui.theme.MeowfiaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeowfiaTheme {
                val navController = rememberNavController()
                GameNavGraph(navController = navController)
            }
        }
    }
}
