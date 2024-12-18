package com.ayush.tranxporter.core.presentation.onboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
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
import kotlinx.coroutines.launch

data class OnboardingScreen(
    @Transient val onFinishOnboarding: () -> Unit
) : Screen {
    @Composable
    override fun Content() {
        Box(modifier = Modifier.fillMaxSize()) {  // Wrap in Box for better layout control
            val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                TopSection(
                    pagerState = pagerState,
                    onSkipClick = onFinishOnboarding
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    PagerScreen(
                        onboardingPage = OnboardingPage.values()[page],
                        onFinishClick = onFinishOnboarding
                    )
                }

                BottomSection(
                    pagerState = pagerState,
                    onFinishClick = onFinishOnboarding
                )
            }
        }
    }

    companion object {
        private const val PAGE_COUNT = 3
    }
}
// Create an enum to manage pages
private enum class OnboardingPage {
    WELCOME,
    PERMISSIONS,
    REGISTER
}
@Composable
private fun PagerScreen(
    onboardingPage: OnboardingPage,
    onFinishClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (onboardingPage) {
            OnboardingPage.WELCOME -> WelcomePage()
            OnboardingPage.PERMISSIONS -> PermissionsPage()
            OnboardingPage.REGISTER -> RegisterPage(onFinishClick)
        }
    }
}
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
fun RegisterPage(
    onFinishClick: () -> Unit
) {
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
            onClick = { 
                onFinishClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text("Get Started")
        }
    }
}


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
        Row(
            Modifier
                .align(Alignment.Center)
                .height(50.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(8.dp)
                        .background(
                            color = color,
                            shape = CircleShape
                        )
                )
            }
        }

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
                    Icons.Default.Check
                else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (pagerState.currentPage == 2) "Finish" else "Next"
            )
        }
    }
}