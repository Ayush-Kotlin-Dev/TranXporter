package com.ayush.tranxporter

import android.os.Bundle
import android.util.Log
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayush.tranxporter.ui.theme.TranXporterTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import cafe.adriel.voyager.navigator.Navigator
import com.ayush.tranxporter.auth.presentation.login.AuthScreen
import com.ayush.tranxporter.core.domain.model.AppState
import com.ayush.tranxporter.core.presentation.onboard.OnboardingScreen
import com.ayush.tranxporter.driver.DriverScreen
import com.ayush.tranxporter.user.BookingScreen
import com.ayush.tranxporter.user.LocationSelectionScreen
import com.google.android.gms.maps.model.LatLng
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
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val auth = Firebase.auth
    val currentUser = auth.currentUser

    NavHost(
        navController = navController,
        startDestination = if (currentUser != null) "home" else "auth"
    ) {

        composable("home") {
            HomeScreen(navController)
        }
        composable("booking") {
            BookingScreen(navController)
        }
        composable("searchLocation") {
            SearchLocationScreen(navController)
        }
        composable("locationSelection") {
            LocationSelectionScreen(navController)
        }
        composable("driver") {
            DriverScreen()
        }
        composable(
            "booking?pickup_lat={pickup_lat}&pickup_lng={pickup_lng}&drop_lat={drop_lat}&drop_lng={drop_lng}",
            arguments = listOf(
                navArgument("pickup_lat") { type = NavType.FloatType },
                navArgument("pickup_lng") { type = NavType.FloatType },
                navArgument("drop_lat") { type = NavType.FloatType },
                navArgument("drop_lng") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            val pickupLat = args?.getFloat("pickup_lat") ?: 0f
            val pickupLng = args?.getFloat("pickup_lng") ?: 0f
            val dropLat = args?.getFloat("drop_lat") ?: 0f
            val dropLng = args?.getFloat("drop_lng") ?: 0f

            BookingScreen(
                navController = navController,
                initialPickup = LatLng(pickupLat.toDouble(), pickupLng.toDouble()),
                initialDropoff = LatLng(dropLat.toDouble(), dropLng.toDouble())
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TranXporter") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
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
                    Firebase.auth.signOut()
                    navController.navigate("auth") {
                        popUpTo("home") { inclusive = true }
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