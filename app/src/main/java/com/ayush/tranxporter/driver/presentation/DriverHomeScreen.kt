package com.ayush.tranxporter.driver.presentation

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun DriverHomeScreen() {
    var isOnline by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Driver Dashboard") },
                actions = {
                    // Status indicator with better visual feedback
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) Color.Green else Color.Red)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isOnline,
                            onCheckedChange = { isOnline = it }
                        )
                        Text(
                            text = if (isOnline) "Online" else "Offline",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Earnings Card
            item {
                EarningsCard()
            }

            // Today's Stats
            item {
                StatsRow()
            }

            // Available Orders Section
            item {
                Text(
                    "Available Orders",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Sample orders - replace with actual data
            items(sampleOrders) { order ->
                OrderCard(order)
            }
        }
    }
}

@Composable
private fun EarningsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Today's Earnings",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "₹1,250",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                // Quick earnings summary
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        "↑ 15% vs yesterday",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(Icons.Default.Place, "5", "Trips")
                StatItem(Icons.Default.CheckCircle, "6", "Hours")
                StatItem(Icons.Default.KeyboardArrowDown, "120", "Km")
            }
        }
    }
}

@Composable
private fun StatItem(icon: ImageVector, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatCard(
            icon = Icons.Default.Star,
            value = "4.8",
            label = "Rating"
        )
        StatCard(
            icon = Icons.Default.CheckCircle,
            value = "85%",
            label = "Acceptance"
        )
        StatCard(
            icon = Icons.Default.Done,
            value = "98%",
            label = "Completion"
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String
) {
    Card(
        modifier = Modifier.width(100.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun OrderCard(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "Order #${order.id}",
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                PriceTag(order.price)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LocationInfo(
                pickup = order.pickup,
                dropoff = order.dropoff,
                distance = order.distance
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* Handle order acceptance */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Accept")
                }
                OutlinedButton(
                    onClick = { /* Show more details */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Details")
                }
            }
        }
    }
}

@Composable
private fun PriceTag(price: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Text(
            "₹$price",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun LocationInfo(pickup: String, dropoff: String, distance: Double) {
    Column {
        LocationRow(
            icon = Icons.Default.Place,
            label = "Pickup",
            location = pickup
        )
        LocationRow(
            icon = Icons.Default.LocationOn,
            label = "Dropoff",
            location = dropoff
        )
        Text(
            "${String.format("%.1f", distance)} km",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 32.dp)
        )
    }
}

@Composable
private fun LocationRow(icon: ImageVector, label: String, location: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                location,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun Badge(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.extraSmall,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

data class Order(
    val id: String,
    val pickup: String,
    val dropoff: String,
    val distance: Double,
    val price: Int
)

private val sampleOrders = listOf(
    Order("1001", "Indiranagar", "Whitefield", 15.5, 450),
    Order("1002", "Koramangala", "Electronic City", 12.0, 350),
    Order("1003", "HSR Layout", "Airport", 25.0, 750)
)