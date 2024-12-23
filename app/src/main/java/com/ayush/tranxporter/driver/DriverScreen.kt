package com.ayush.tranxporter.driver


import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import com.ayush.tranxporter.driver.presentation.home.DriverHomeScreen


@OptIn(ExperimentalMaterial3Api::class)
class DriverScreen : Screen {

    @Composable
    override fun Content() {
        DriverHomeScreen(

        )
    }
}
