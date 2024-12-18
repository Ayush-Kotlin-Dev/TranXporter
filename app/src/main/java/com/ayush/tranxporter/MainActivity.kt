package com.ayush.tranxporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ayush.tranxporter.auth.presentation.login.AuthScreen
import com.ayush.tranxporter.core.domain.model.AppState
import com.ayush.tranxporter.core.presentation.onboard.OnboardingScreen
import com.ayush.tranxporter.driver.DriverScreen
import com.ayush.tranxporter.ui.theme.TranXporterTheme
import com.ayush.tranxporter.user.presentation.bookingdetails.BookingDetailsScreen
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TranXporterTheme {
                val viewModel = koinViewModel<MainActivityViewModel>()
                val appState by viewModel.appState.collectAsStateWithLifecycle()

                when (appState) {
                    AppState.Loading -> LoadingScreen()
                    AppState.NeedsOnboarding -> {
                        Navigator(OnboardingScreen(viewModel::completeOnboarding))
                    }

                    AppState.Ready -> {
                        Navigator(
                             if (FirebaseAuth.getInstance().currentUser == null) {
                                AuthScreen()
                            } else {
                                HomeScreen()
                             }
                        )
                    }
                }
            }
        }
    }
}


class HomeScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {

        val systemUiController = rememberSystemUiController()
        val primaryContainer = MaterialTheme.colorScheme.primaryContainer
        val isDarkIcons = MaterialTheme.colorScheme.primary.luminance() > 0.5
        val navigator = LocalNavigator.currentOrThrow

        SideEffect {
            systemUiController.setStatusBarColor(
                color = primaryContainer,
                darkIcons = isDarkIcons
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "TranXporter",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = primaryContainer
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            },
            modifier = Modifier.systemBarsPadding()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Welcome to TranXporter", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { navigator.push(BookingDetailsScreen()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Book a Vehicle")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { navigator.push(DriverScreen()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Driver Mode")
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()

                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Logout")
                }
            }
        }

    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}