package com.ayush.tranxporter.user.presentation.bookingdetails

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ayush.tranxporter.R
import com.ayush.tranxporter.user.LocationSelectionScreen
import org.koin.androidx.compose.koinViewModel

private object Dimens {
    val paddingLarge = 24.dp
    val paddingMedium = 16.dp
    val cornerRadiusLarge = 16.dp
    val cornerRadiusMedium = 12.dp
    val buttonHeight = 56.dp
}

@OptIn(ExperimentalMaterial3Api::class)
class BookingDetailsScreen : Screen {

    @Composable
    override fun Content() {
        val navController = LocalNavigator.currentOrThrow
        val viewModel: BookingDetailsViewModel = koinViewModel()
        val state = viewModel.state
        val scrollState = rememberScrollState()
        val context = LocalContext.current

        var hasNavigated by remember { mutableStateOf(false) }

        // Handle submission and navigation
        LaunchedEffect(state.submittedDetails) {
            if (state.submittedDetails != null && !hasNavigated) {
                hasNavigated = true
                navController.push(LocationSelectionScreen())

            }
        }

        DisposableEffect(Unit) {
            onDispose {
                hasNavigated = false
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Transport Details")
                            Text(
                                "Step 1 of 3",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.pop()
                        }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { padding ->
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(scrollState)
                ) {
                    ItemDetailsCard(
                        state = state,
                        onEvent = viewModel::onEvent
                    )
                }
            }
        }
    }
}

@Composable
fun ItemDetailsCard(
    state: BookingDetailsState,
    onEvent: (BookingDetailsEvent) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.paddingMedium),
        shape = RoundedCornerShape(Dimens.cornerRadiusLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(Dimens.paddingLarge)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.paddingLarge)
        ) {
            // Category Section
            FormSection(title = "Item Category") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(transportCategories) { category ->
                        CategoryChip(
                            category = category,
                            selected = category == state.selectedCategory,
                            onSelect = {
                                onEvent(BookingDetailsEvent.CategorySelected(category))
                            }
                        )
                    }
                }
            }

            // Weight and Dimensions Section
            FormSection(title = "Item Details") {
                OutlinedTextField(
                    value = state.weight,
                    onValueChange = {
                        onEvent(BookingDetailsEvent.WeightChanged(it))
                    },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = state.weightError != null,
                    supportingText = state.weightError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.weight),
                            contentDescription = null
                        )
                    },
                    shape = RoundedCornerShape(Dimens.cornerRadiusMedium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        focusedLeadingIconColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.dimensions,
                    onValueChange = {
                        onEvent(BookingDetailsEvent.DimensionsChanged(it))

                    },
                    label = { Text("Dimensions (L x W x H) cm") },
                    placeholder = { Text("100 x 50 x 75") },
                    isError = state.dimensionsError != null,
                    supportingText = state.dimensionsError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.dimensions),
                            contentDescription = null
                        )
                    },
                    shape = RoundedCornerShape(Dimens.cornerRadiusMedium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        focusedLeadingIconColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Vehicle Type Section
            FormSection(title = "Vehicle Type") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(TruckType.entries.toList()) { truckType ->
                        VehicleTypeChip(
                            type = truckType,
                            selected = truckType == state.selectedTruckType,
                            onSelect = {
                                onEvent(BookingDetailsEvent.TruckTypeSelected(truckType))
                            }
                        )
                    }
                }
            }

            // Additional Details Section
            FormSection(title = "Additional Details") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Requires Special Handling",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.specialHandling,
                        onCheckedChange = {
                            onEvent(BookingDetailsEvent.SpecialHandlingChanged(it))
                        }
                    )
                }

                OutlinedTextField(
                    value = state.description,
                    onValueChange = {
                        onEvent(BookingDetailsEvent.DescriptionChanged(it))
                    },
                    label = { Text("Additional Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(Dimens.cornerRadiusMedium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        focusedLeadingIconColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Button(
                onClick = {
                    onEvent(BookingDetailsEvent.Submit)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeight),
                enabled = state.isFormValid,
                shape = RoundedCornerShape(Dimens.cornerRadiusLarge),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Continue to Location Selection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun VehicleTypeChip(
    type: TruckType,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onSelect)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 4.dp else 1.dp
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = when (type) {
                        TruckType.PICKUP -> R.drawable.ic_pickup
                        TruckType.LORRY -> R.drawable.ic_lorry
                        TruckType.SIXTEEN_WHEELER -> R.drawable.ic_truck
                        TruckType.TRACTOR -> R.drawable.ic_tractor
                    }
                ),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = type.name.replace("_", " "),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            if (selected) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Selected",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

enum class BookingStep {
    LOCATION_SELECTION,
    VEHICLE_SELECTION
}

val transportCategories = listOf(
    "Furniture",
    "Electronics",
    "Appliances",
    "Construction Materials",
    "Machinery",
    "Retail Goods",
    "Others"
)

@Composable
private fun CategoryChip(
    category: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onSelect)
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            ),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = category,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface
        )
    }
}