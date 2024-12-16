package com.ayush.tranxporter.user.presentation.bookingdetails

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ayush.tranxporter.R
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsScreen(
    navController: NavController,
    onDetailsSubmitted: (TransportItemDetails) -> Unit
) {
    var isFormValid by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val context = LocalContext.current

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
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            ItemDetailsCard(
                onFormValidityChanged = { isFormValid = it },
                onDetailsSubmitted = { details ->
                    onDetailsSubmitted(details)
                    Toast.makeText(context, "Details saved successfully!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun ItemDetailsCard(
    onFormValidityChanged: (Boolean) -> Unit,
    onDetailsSubmitted: (TransportItemDetails) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var weight by remember { mutableStateOf("") }
    var dimensions by remember { mutableStateOf("") }
    var selectedTruckType by remember { mutableStateOf<TruckType?>(null) }
    var specialHandling by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    // Validation states
    var weightError by remember { mutableStateOf<String?>(null) }
    var dimensionsError by remember { mutableStateOf<String?>(null) }

    // Validate form
    fun validateForm(): Boolean {
        weightError = when {
            weight.isEmpty() -> "Weight is required"
            weight.toDoubleOrNull() == null -> "Please enter a valid number"
            weight.toDouble() <= 0 -> "Weight must be greater than 0"
            else -> null
        }

        dimensionsError = when {
            dimensions.isEmpty() -> "Dimensions are required"
            !dimensions.matches(Regex("""^\d+\s*x\s*\d+\s*x\s*\d+$""")) ->
                "Format should be: length x width x height"
            else -> null
        }

        val isValid = selectedCategory != null &&
                weightError == null &&
                dimensionsError == null &&
                selectedTruckType != null

        onFormValidityChanged(isValid)
        return isValid
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        // Update the card colors to use a light grey background
        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surface,
             containerColor = Color(0xFFF5F5F5) // Light grey
        )
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            selected = category == selectedCategory,
                            onSelect = { selectedCategory = category }
                        )
                    }
                }
            }

            // Weight and Dimensions Section
            FormSection(title = "Item Details") {
                OutlinedTextField(
                    value = weight,
                    onValueChange = {
                        weight = it
                        validateForm()
                    },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = weightError != null,
                    supportingText = weightError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.weight),
                            contentDescription = null
                        )
                    }
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = dimensions,
                    onValueChange = {
                        dimensions = it
                        validateForm()
                    },
                    label = { Text("Dimensions (L x W x H) cm") },
                    placeholder = { Text("100 x 50 x 75") },
                    isError = dimensionsError != null,
                    supportingText = dimensionsError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.dimensions),
                            contentDescription = null
                        )
                    }
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
                            selected = truckType == selectedTruckType,
                            onSelect = { selectedTruckType = truckType }
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
                        checked = specialHandling,
                        onCheckedChange = { specialHandling = it }
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Additional Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            Button(
                onClick = {
                    if (validateForm()) {
                        onDetailsSubmitted(
                            TransportItemDetails(
                                category = selectedCategory!!,
                                weight = weight.toDouble(),
                                dimensions = dimensions,
                                truckType = selectedTruckType!!,
                                specialHandling = specialHandling,
                                description = description
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = validateForm()
            ) {
                Text("Continue to Location Selection")
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
private fun VehicleTypeChip(
    type: TruckType,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .padding(8.dp)
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
        Spacer(Modifier.height(4.dp))
        Text(
            text = type.name.replace("_", " "),
            style = MaterialTheme.typography.bodySmall,
            color = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

// Add these helper components and enums
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

@Serializable
data class TransportItemDetails(
    val category: String,
    val weight: Double,
    val dimensions: String,
    val truckType: TruckType,
    val specialHandling: Boolean = false,
    val description: String = ""
)
@Serializable
enum class TruckType {
    PICKUP,
    LORRY,
    SIXTEEN_WHEELER,
    TRACTOR
}