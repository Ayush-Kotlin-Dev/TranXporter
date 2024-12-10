package com.ayush.tranxporter.driver


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScreen() {
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
                            availableRides = availableRides.toMutableList().apply { removeAt(index) }
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