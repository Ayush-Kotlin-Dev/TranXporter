package com.ayush.tranxporter.driver.presentation.home

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ayush.tranxporter.utils.getGreeting
import org.koin.compose.viewmodel.koinViewModel

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun DriverHomeScreen() {
    val coroutineScope = rememberCoroutineScope()
    val viewModel = koinViewModel<DriverHomeViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    val isRefreshing = uiState.isRefreshing

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            viewModel.refresh()
        }
    )
    var expandedOrderIds by remember { mutableStateOf(
        setOf(
            uiState.orders.firstOrNull()?.id ?: ""
        )
    ) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(getGreeting(), style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Driver Dashboard",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                    }
                },
                actions = {
                    OnlineStatusToggle(
                        isOnline = uiState.isOnline,
                        onStatusChange = { viewModel.toggleOnlineStatus() }
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        if (uiState.isLoading) {
                            EarningsCardShimmer()
                        } else {
                            EarningsCard(isRefreshing = isRefreshing)
                        }
                    }
                    item {
                        if (uiState.isLoading) {
                            StatsRowShimmer()
                        } else {
                            StatsRow()
                        }
                    }
                    item {
                        Text(
                            "Available Orders",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (uiState.isLoading) {
                        items(3) {
                            OrderCardShimmer()
                        }
                    } else {
                        itemsIndexed(
                            items = uiState.orders,
                            key = { _, order -> order.id }
                        ) { _, order ->
                            AnimatedVisibility(
                                visible = true,
                                enter = slideInVertically(
                                    initialOffsetY = { fullHeight -> fullHeight }
                                ) + fadeIn(
                                    initialAlpha = 0f
                                ),
                                modifier = Modifier.animateItem()
                            ) {
                                OrderCard(
                                    order = order,
                                    onOrderClick = { selectedOrder = order },
                                    expanded = selectedOrder?.id == order.id,
                                    onExpandedChange = { expanded ->

                                        expandedOrderIds = if (expanded) {
                                            expandedOrderIds + order.id
                                        } else {
                                            expandedOrderIds - order.id
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    scale = true,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    // Show error if present
    uiState.error?.let { error ->
        ErrorSnackbar(
            message = error,
            onDismiss = viewModel::clearError
        )
    }
}

@Composable
fun ErrorSnackbar(message: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
            .clickable { onDismiss() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun StatsRow() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Quick Stats",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickStatItem(
                    value = "6.5",
                    unit = "hrs",
                    label = "Active Hours"
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                )
                QuickStatItem(
                    value = "85",
                    unit = "%",
                    label = "Acceptance Rate"
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                )
                QuickStatItem(
                    value = "4.8",
                    unit = "★",
                    label = "Rating"
                )
            }
        }
    }
}

@Composable
private fun QuickStatItem(
    value: String,
    unit: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 4.dp)
        )
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
            )
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Status and Order ID Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Badge(
                        containerColor = Color(0xFFE3F2FD),
                        modifier = Modifier.shadow(
                            elevation = 0.dp,
                            shape = RoundedCornerShape(8.dp)
                        )
                    ) {
                        Text(
                            "Order #${order.id}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color(0xFF1E88E5),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    OrderStatusBadge(order.status)
                }
                PriceTag(order.price)
            }

            // Time and Distance Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(
                    icon = Icons.Default.Place,
                    text = "${order.distance} km",
                    contentColor = MaterialTheme.colorScheme.primary
                )
                InfoChip(
                    icon = Icons.Default.Close,
                    text = order.timeEstimate,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }

            // Location Info
            LocationInfo(
                pickup = order.pickup,
                dropoff = order.dropoff,
                distance = order.distance
            )

            // Package Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(
                    icon = Icons.Default.LocationOn,
                    text = order.vehicleType,
                    contentColor = MaterialTheme.colorScheme.secondary
                )
                InfoChip(
                    icon = Icons.Default.Place,
                    text = order.weight,
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Accept")
                    }
                }
                OutlinedButton(
                    onClick = { onOrderClick(order) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Details")
                    }
                }
            }

            // Expandable Details
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
private fun OrderStatusBadge(status: OrderStatus) {
    val (backgroundColor, textColor, statusText) = when(status) {
        OrderStatus.PENDING -> Triple(
            Color(0xFFFFF3E0),  // Light Orange
            Color(0xFFE65100),  // Dark Orange
            "New"
        )
        OrderStatus.ACCEPTED -> Triple(
            Color(0xFFE3F2FD),  // Light Blue
            Color(0xFF1565C0),  // Dark Blue
            "Accepted"
        )
        OrderStatus.IN_PROGRESS -> Triple(
            Color(0xFFE8F5E9),  // Light Green
            Color(0xFF2E7D32),  // Dark Green
            "In Progress"
        )
        OrderStatus.COMPLETED -> Triple(
            Color(0xFFF3E5F5),  // Light Purple
            Color(0xFF6A1B9A),  // Dark Purple
            "Completed"
        )
        OrderStatus.CANCELLED -> Triple(
            Color(0xFFFFEBEE),  // Light Red
            Color(0xFFC62828),  // Dark Red
            "Cancelled"
        )
    }

    Badge(
        containerColor = backgroundColor,
        modifier = Modifier.shadow(
            elevation = 0.dp,
            shape = RoundedCornerShape(6.dp)
        )
    ) {
        Text(
            statusText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(
                color = contentColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
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
            .padding(end = 8.dp)  // Reduced horizontal padding
            .height(40.dp),       // Fixed height for better alignment
        shape = RoundedCornerShape(20.dp),  // Slightly reduced corner radius
        colors = CardDefaults.cardColors(
            containerColor = if (isOnline)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()    // Fill card height
                .padding(horizontal = 8.dp),  // Reduced padding
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)  // Reduced spacing
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)    // Slightly smaller indicator
                    .background(
                        color = if (isOnline) Color.Green else Color.Red,
                        shape = CircleShape
                    )
            )
            Switch(
                checked = isOnline,
                onCheckedChange = onStatusChange,
                modifier = Modifier.scale(0.8f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.error,
                    uncheckedTrackColor = MaterialTheme.colorScheme.errorContainer
                )
            )
            Text(
                text = if (isOnline) "Online" else "Offline",
                style = MaterialTheme.typography.labelSmall,  // Smaller text
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

@Composable
private fun EarningsCardShimmer() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(24.dp)
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shimmerEffect()
            )
        }
    }
}

@Composable
private fun StatsRowShimmer() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(24.dp)
                    .shimmerEffect()
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(3) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(32.dp)
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(16.dp)
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCardShimmer() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(24.dp)
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(24.dp)
                        .shimmerEffect()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Location info shimmer
            repeat(2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(20.dp)
                            .shimmerEffect()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons shimmer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .shimmerEffect()
                )
            }
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition()
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )

    background(Color(0xFFE0E0E0))
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFE0E0E0),
                    Color(0xFFFFFFFF),
                    Color(0xFFE0E0E0)
                ),
                start = Offset(startOffsetX, 0f),
                end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
            )
        )
        .onGloballyPositioned {
            size = it.size
        }
}