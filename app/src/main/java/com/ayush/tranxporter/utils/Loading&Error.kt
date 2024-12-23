package com.ayush.tranxporter.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha


@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    isLoading: Boolean = true,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        content()
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun LoadingDialog(
    modifier: Modifier = Modifier,
    isLoading: Boolean = true,
    message: String = "Loading...",
    onDismissRequest: () -> Unit = {}
) {
    if (isLoading) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = onDismissRequest,
        ) {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun LoadingOverlay(
    modifier: Modifier = Modifier,
    isLoading: Boolean = true,
    alpha: Float = 0.6f,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        content()
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun ErrorScreen(
    modifier: Modifier = Modifier,
    message: String = "An error occurred",
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        content()
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = message)
        }
    }
}

@Composable
fun ErrorDialog(
    modifier: Modifier = Modifier,
    message: String = "An error occurred",
    onDismissRequest: () -> Unit = {}
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(text = message)
        }
    }
}

@Composable
fun ErrorOverlay(
    modifier: Modifier = Modifier,
    message: String = "An error occurred", 
    alpha: Float = 0.6f,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        content()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha),
            contentAlignment = Alignment.Center
        ) {
            Text(text = message)
        }
    }
}