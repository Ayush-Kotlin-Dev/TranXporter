package com.ayush.tranxporter.core.presentation.onboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.ayush.tranxporter.R
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch

data class OnboardingScreen(
    val onFinishOnboarding: () -> Unit
) : Screen {

    @Composable
    override fun Content() {
        val pagerState = rememberPagerState()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopSection(
                pagerState = pagerState,
                onSkipClick = onFinishOnboarding
            )

            HorizontalPager(
                count = 3,
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                PagerScreen(page = page)
            }

            BottomSection(
                pagerState = pagerState,
                onFinishClick = onFinishOnboarding
            )
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun TopSection(
    pagerState: PagerState,
    onSkipClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // Skip button only shown on first two pages
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterEnd),
            visible = pagerState.currentPage < 2
        ) {
            TextButton(onClick = onSkipClick) {
                Text("Skip")
            }
        }
    }
}

@Composable
fun PagerScreen(page: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (page) {
            0 -> WelcomePage()
            1 -> PermissionsPage()
            2 -> RegisterPage()
        }
    }
}

@Composable
fun WelcomePage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.welcome),
            contentDescription = "Welcome",
            modifier = Modifier.size(200.dp)
        )

        Text(
            text = "Welcome to TranXporter",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Your reliable partner for seamless goods transportation. Connect with trusted drivers and move your items safely.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PermissionsPage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Required Permissions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        PermissionItem(
            title = "Location Access",
            description = "To track shipments and find nearby drivers",
            icon = R.drawable.ic_location // Add your icons
        )

        PermissionItem(
            title = "Manage Call Logs",
            description = "For seamless communication with drivers",
            icon = R.drawable.ic_call
        )

        PermissionItem(
            title = "Store Call Logs",
            description = "To maintain delivery records",
            icon = R.drawable.ic_storage
        )
    }
}

@Composable
fun RegisterPage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.register),
            contentDescription = "Register",
            modifier = Modifier.size(200.dp)
        )

        Text(
            text = "Create Your Account",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Join TranXporter to start shipping your goods safely and efficiently",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = { /* Navigate to Auth Screen */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text("Get Started")
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    icon: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = title,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun BottomSection(
    pagerState: PagerState,
    onFinishClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // Pager indicator
        HorizontalPagerIndicator(
            pagerState = pagerState,
            modifier = Modifier.align(Alignment.Center),
            activeColor = MaterialTheme.colorScheme.primary
        )

        // Next/Finish button
        FloatingActionButton(
            onClick = {
                if (pagerState.currentPage == 2) onFinishClick()
                else scope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = if (pagerState.currentPage == 2)
                    Icons.Default.KeyboardArrowRight
                else Icons.Default.KeyboardArrowLeft,
                contentDescription = if (pagerState.currentPage == 2) "Finish" else "Next"
            )
        }
    }
}