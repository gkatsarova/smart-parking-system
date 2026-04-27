package com.planzy.smartparkingsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.planzy.smartparkingsystem.ui.screens.ParkingScreen
import com.planzy.smartparkingsystem.ui.theme.SmartParkingSystemTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartParkingSystemTheme {
                ParkingScreen()
            }
        }
    }
}