package com.ayush.tranxporter.utils

import androidx.compose.runtime.Composable
import java.util.Calendar

@Composable
fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning"
        hour < 16 -> "Good Afternoon"
        else -> "Good Evening"
    }
}