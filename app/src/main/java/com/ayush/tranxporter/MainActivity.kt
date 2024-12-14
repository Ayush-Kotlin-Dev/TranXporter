package com.ayush.tranxporter

import android.annotation.SuppressLint
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.SideEffect
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cafe.adriel.voyager.navigator.Navigator
import com.ayush.tranxporter.auth.presentation.login.AuthScreen
import com.ayush.tranxporter.auth.presentation.service_selection.ServiceSelectionScreen
import com.ayush.tranxporter.auth.presentation.service_selection.UserDetailsScreen
import com.ayush.tranxporter.core.components.UserType
import com.ayush.tranxporter.core.domain.model.AppState
import com.ayush.tranxporter.core.presentation.onboard.OnboardingScreen
import com.ayush.tranxporter.driver.DriverScreen
import com.ayush.tranxporter.ui.theme.TranXporterTheme
import com.ayush.tranxporter.user.BookingScreen
import com.ayush.tranxporter.user.LocationSelectionScreen
import com.ayush.tranxporter.user.SearchLocationScreen
import com.ayush.tranxporter.user.presentation.location.LocationSelectionViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.gms.maps.model.LatLng
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
                    AppState.Ready -> MainScreen()
                }
            }
        }
    }
}

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val viewModel: LocationSelectionViewModel = viewModel(
        factory = LocationSelectionViewModel.Factory
    )

    NavHost(
        navController = navController,
        startDestination = if (currentUser != null) "home" else "auth"
    ) {
        composable("auth") {
            Navigator(AuthScreen {
                // Navigate to service selection after auth
                navController.navigate("service_selection") {
                    popUpTo("auth") { inclusive = true }
                }
            })
        }
        composable("service_selection") {
            ServiceSelectionScreen { userType ->
                navController.navigate("user_details/${userType.name}")
            }
        }
        composable(
            "user_details/{type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val userType = UserType.valueOf(
                backStackEntry.arguments?.getString("type") ?: UserType.CONSUMER.name
            )
            UserDetailsScreen(
                userType = userType,
                onDetailsSubmitted = {
                    // Navigate to home screen after details are submitted
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(navController)
        }
        composable("booking") {
            BookingScreen(navController, viewModel = viewModel)
        }
        composable("searchLocation") {
            SearchLocationScreen(navController)
        }
        composable("locationSelection") {
            LocationSelectionScreen(navController, viewModel = viewModel)
        }
        composable("driver") {
            DriverScreen()
        }
        composable(
            "booking?type={type}&pickup_lat={pickup_lat}&pickup_lng={pickup_lng}&drop_lat={drop_lat}&drop_lng={drop_lng}",
            arguments = listOf(
                navArgument("type") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("pickup_lat") {
                    type = NavType.FloatType
                    defaultValue = 0f
                },
                navArgument("pickup_lng") {
                    type = NavType.FloatType
                    defaultValue = 0f
                },
                navArgument("drop_lat") {
                    type = NavType.FloatType
                    defaultValue = 0f
                },
                navArgument("drop_lng") {
                    type = NavType.FloatType
                    defaultValue = 0f
                }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            val type = args?.getString("type")
            val pickupLat = args?.getFloat("pickup_lat") ?: 0f
            val pickupLng = args?.getFloat("pickup_lng") ?: 0f
            val dropLat = args?.getFloat("drop_lat") ?: 0f
            val dropLng = args?.getFloat("drop_lng") ?: 0f

            BookingScreen(
                navController = navController,
                initialPickup = if (pickupLat != 0f && pickupLng != 0f)
                    LatLng(pickupLat.toDouble(), pickupLng.toDouble()) else null,
                initialDropoff = if (dropLat != 0f && dropLng != 0f)
                    LatLng(dropLat.toDouble(), dropLng.toDouble()) else null,
                locationType = type,
                viewModel = viewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val systemUiController = rememberSystemUiController()
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val isDarkIcons = MaterialTheme.colorScheme.primary.luminance() > 0.5

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
                onClick = { navController.navigate("searchLocation") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("Book a Vehicle")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { navController.navigate("driver") },
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
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
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

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}