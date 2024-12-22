package com.ayush.tranxporter.driver.presentation

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Preview(name = "Light Theme", showBackground = true)
@Preview(
    name = "Dark Theme",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun DriverHomeScreenPreview() {
    DriverHomeScreen()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun DriverHomeScreen() {
    var isOnline by remember { mutableStateOf(true) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                delay(1500) // Simulate refresh
                isRefreshing = false
            }
        }
    )

    val onlineTransition = updateTransition(isOnline, label = "onlineTransition")
    val scale by onlineTransition.animateFloat(
        label = "scale",
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) }
    ) { if (it) 1.1f else 1f }

    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Driver Dashboard") },
                actions = {
                    OnlineStatusToggle(
                        isOnline = isOnline,
                        onStatusChange = { isOnline = it }
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(Modifier.pullRefresh(pullRefreshState)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    EarningsCard()
                }
                item {
                    StatsRow()
                }
                item {
                    Text(
                        "Available Orders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                itemsIndexed(
                    items = sampleOrders,
                    key = { _, order -> order.id }
                ) { _, order ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight } // slide from bottom
                        ) + fadeIn(
                            initialAlpha = 0f
                        ),
                        modifier = Modifier.animateItemPlacement()
                    ) {
                        OrderCard(
                            order = order,
                            onOrderClick = { selectedOrder = order },
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        )
                    }
                }
            }

            PullRefreshIndicator(
                state = pullRefreshState,
                refreshing = isRefreshing,
            )
        }
    }
}

@Composable
private fun EarningsCard() {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = shimmerColors,
                        startX = translateAnim.value - 1000f,
                        endX = translateAnim.value
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "Today's Earnings",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    "₹1,250",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.align(Alignment.End),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    )
                ) {
                    Text(
                        "↑ 15% vs yesterday",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
                Divider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = Color.White.copy(alpha = 0.2f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(Icons.Default.Place, "5", "Trips", Color.White)
                    StatItem(Icons.Default.CheckCircle, "6", "Hours", Color.White)
                    StatItem(Icons.Default.KeyboardArrowDown, "120", "Km", Color.White)
                }
            }
        }
    }
}

@Composable
private fun StatItem(icon: ImageVector, value: String, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = color
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
        modifier = Modifier
            .width(105.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    onOrderClick: (Order) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = { onExpandedChange(!expanded) }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        "Order #${order.id}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                PriceTag(order.price)
            }
            Spacer(modifier = Modifier.height(16.dp))
            LocationInfo(
                pickup = order.pickup,
                dropoff = order.dropoff,
                distance = order.distance
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { /* Handle order acceptance */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Accept",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                OutlinedButton(
                    onClick = { onOrderClick(order) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        "Details",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OrderDetails(order)
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

@Composable
private fun OnlineStatusToggle(
    isOnline: Boolean,
    onStatusChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOnline)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isOnline) Color.Green else Color.Red,
                        shape = CircleShape
                    )
            )
            Switch(
                checked = isOnline,
                onCheckedChange = onStatusChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.error,
                    uncheckedTrackColor = MaterialTheme.colorScheme.errorContainer
                )
            )
            Text(
                text = if (isOnline) "Online" else "Offline",
                style = MaterialTheme.typography.labelMedium,
                color = if (isOnline)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun OrderDetails(order: Order) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        DetailRow("Vehicle Type", "Mini Truck")
        DetailRow("Weight", "250 kg")
        DetailRow("Time Estimate", "45 mins")
        DetailRow("Payment Mode", "Online")

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Additional Notes",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Fragile items, handle with care",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

data class Order(
    val id: String,
    val pickup: String,
    val dropoff: String,
    val distance: Double,
    val price: Int,
    val vehicleType: String = "Mini Truck",
    val weight: String = "250 kg",
    val timeEstimate: String = "45 mins",
    val paymentMode: String = "Online",
    val additionalNotes: String = "Fragile items, handle with care",
    val status: OrderStatus = OrderStatus.PENDING
)

enum class OrderStatus {
    PENDING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED
}

private val sampleOrders = listOf(
    Order("1001", "Indiranagar", "Whitefield", 15.5, 450),
    Order("1002", "Koramangala", "Electronic City", 12.0, 350),
    Order("1003", "HSR Layout", "Airport", 25.0, 750)
)