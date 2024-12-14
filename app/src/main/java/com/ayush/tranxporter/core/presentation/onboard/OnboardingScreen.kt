package com.ayush.tranxporter.core.presentation.onboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.ayush.tranxporter.R
import com.ayush.tranxporter.core.presentation.util.PermissionUtils
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch

data class OnboardingScreen(
    val onFinishOnboarding: () -> Unit
) : Screen {

    @Composable
    override fun Content() {
        val pagerState = rememberPagerState()
        TopSection(
            pagerState = pagerState,
            onSkipClick = onFinishOnboarding
        )
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
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
    val context = LocalContext.current
    var permissionsState by remember {
        mutableStateOf(
            PermissionUtils.requiredPermissions.associateWith { permission ->
                PermissionUtils.checkPermission(context, permission.permission)
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Update the permission state that was just requested
        permissionsState = permissionsState.mapValues { (permission, _) ->
            PermissionUtils.checkPermission(context, permission.permission)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Required Permissions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        PermissionUtils.requiredPermissions.forEach { permission ->
            PermissionItemWithChip(
                permission = permission,
                isGranted = permissionsState[permission] ?: false,
                onRequestPermission = {
                    permissionLauncher.launch(permission.permission)
                }
            )
        }
    }
}

@Composable
fun PermissionItemWithChip(
    permission: PermissionUtils.Permission,
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = permission.icon),
                contentDescription = permission.title,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = permission.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = permission.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        AssistChip(
            onClick = onRequestPermission,
            label = {
                Text(if (isGranted) "Granted" else "Grant")
            },
            leadingIcon = if (isGranted) {
                {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else null,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (isGranted)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
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