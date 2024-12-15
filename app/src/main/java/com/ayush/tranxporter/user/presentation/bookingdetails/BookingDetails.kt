package com.ayush.tranxporter.user.presentation.bookingdetails

import androidx.compose.runtime.Composable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
@Composable
fun ItemDetailsCard(
    onDetailsSubmitted: (TransportItemDetails) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var weight by remember { mutableStateOf("") }
    var dimensions by remember { mutableStateOf("") }
    var specialHandling by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "What are you transporting?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Category Selection
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transportCategories) { category ->
                    CategoryChip(
                        category = category,
                        selected = category == selectedCategory,
                        onSelect = { selectedCategory = category }
                    )
                }
            }

            // Weight Input
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Approximate Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Dimensions Input
            OutlinedTextField(
                value = dimensions,
                onValueChange = { dimensions = it },
                label = { Text("Dimensions (L x W x H) in cm") }
            )

            // Special Handling Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Requires Special Handling")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = specialHandling,
                    onCheckedChange = { specialHandling = it }
                )
            }

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Additional Details") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (selectedCategory != null && weight.isNotBlank() && dimensions.isNotBlank()) {
                        onDetailsSubmitted(
                            TransportItemDetails(
                                category = selectedCategory!!,
                                weight = weight.toDoubleOrNull() ?: 0.0,
                                dimensions = dimensions,
                                specialHandling = specialHandling,
                                description = description
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedCategory != null && weight.isNotBlank() && dimensions.isNotBlank()
            ) {
                Text("Continue")
            }
        }
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

data class TransportItemDetails(
    val category: String,
    val weight: Double,
    val dimensions: String,
    val specialHandling: Boolean = false,
    val description: String = ""
)
