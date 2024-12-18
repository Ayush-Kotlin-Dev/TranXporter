package com.ayush.tranxporter.driver


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen


@OptIn(ExperimentalMaterial3Api::class)
class DriverScreen : Screen {

    @Composable
    override fun Content() {
        var availableRides by remember { mutableStateOf(listOf<RideRequest>()) }

        // Simulate fetching ride requests
        LaunchedEffect(Unit) {
            availableRides = getAvailableRides()
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Available Rides") }
                )
            }
        ) { paddingValues ->
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                items(availableRides.size) { index ->
                    val ride = availableRides[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                // Accept ride logic (to be implemented)
                                // Remove the ride from the list
                                availableRides =
                                    availableRides
                                        .toMutableList()
                                        .apply { removeAt(index) }
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Pickup: ${ride.pickupLocationName}")
                            Text("Drop-off: ${ride.dropOffLocationName}")
                            Text("Fare: ₹${"%.2f".format(ride.fare)}")
                        }
                    }
                }
            }
        }
    }
}

data class RideRequest(
    val pickupLocationName: String,
    val dropOffLocationName: String,
    val fare: Double
)

fun getAvailableRides(): List<RideRequest> {
    // Simulate data
    return listOf(
        RideRequest("Sector 1, Noida", "Sector 18, Noida", 150.0),
        RideRequest("Connaught Place, Delhi", "Gurgaon Cyber Hub", 350.0)
    )
}