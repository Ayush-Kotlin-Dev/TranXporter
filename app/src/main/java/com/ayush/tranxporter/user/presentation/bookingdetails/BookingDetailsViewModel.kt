package com.ayush.tranxporter.user.presentation.bookingdetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent

class BookingDetailsViewModel : ViewModel(), KoinComponent {

    var state by mutableStateOf(BookingDetailsState())
        private set

    // Validation states
    private var weightError by mutableStateOf<String?>(null)
    private var dimensionsError by mutableStateOf<String?>(null)

    fun onEvent(event: BookingDetailsEvent) {
        when (event) {
            is BookingDetailsEvent.CategorySelected -> {
                state = state.copy(selectedCategory = event.category)
                validateForm()
            }
            is BookingDetailsEvent.WeightChanged -> {
                state = state.copy(weight = event.weight)
                validateWeight(event.weight)
                validateForm()
            }
            is BookingDetailsEvent.DimensionsChanged -> {
                state = state.copy(dimensions = event.dimensions)
                validateDimensions(event.dimensions)
                validateForm()
            }
            is BookingDetailsEvent.TruckTypeSelected -> {
                state = state.copy(selectedTruckType = event.truckType)
                validateForm()
            }
            is BookingDetailsEvent.SpecialHandlingChanged -> {
                state = state.copy(specialHandling = event.enabled)
            }
            is BookingDetailsEvent.DescriptionChanged -> {
                state = state.copy(description = event.description)
            }
            BookingDetailsEvent.Submit -> submitForm()
            BookingDetailsEvent.Reset -> resetForm()
        }
    }

    private fun validateWeight(weight: String) {
        weightError = when {
            weight.isEmpty() -> "Weight is required"
            weight.toDoubleOrNull() == null -> "Please enter a valid number"
            weight.toDouble() <= 0 -> "Weight must be greater than 0"
            else -> null
        }
        state = state.copy(weightError = weightError)
    }

    private fun validateDimensions(dimensions: String) {
        dimensionsError = when {
            dimensions.isEmpty() -> "Dimensions are required"
            !dimensions.matches(Regex("""^\d+\s*x\s*\d+\s*x\s*\d+$""")) ->
                "Format should be: length x width x height"
            else -> null
        }
        state = state.copy(dimensionsError = dimensionsError)
    }

    private fun validateForm() {
        val isValid = state.selectedCategory != null &&
                state.weightError == null &&
                state.dimensionsError == null &&
                state.selectedTruckType != null &&
                state.weight.isNotEmpty() &&
                state.dimensions.isNotEmpty()

        state = state.copy(isFormValid = isValid)
    }

    private fun submitForm() {
        if (!state.isFormValid) return

        state = state.copy(isLoading = true)

        val details = TransportItemDetails(
            category = state.selectedCategory!!,
            weight = state.weight.toDouble(),
            dimensions = state.dimensions,
            truckType = state.selectedTruckType!!,
            specialHandling = state.specialHandling,
            description = state.description
        )

        state = state.copy(
            submittedDetails = details,
            isLoading = false
        )
    }

    private fun resetForm() {
        state = BookingDetailsState()
        weightError = null
        dimensionsError = null
    }
}

data class BookingDetailsState(
    val selectedCategory: String? = null,
    val weight: String = "",
    val dimensions: String = "",
    val selectedTruckType: TruckType? = null,
    val specialHandling: Boolean = false,
    val description: String = "",
    val weightError: String? = null,
    val dimensionsError: String? = null,
    val isFormValid: Boolean = false,
    val isLoading: Boolean = false,
    val submittedDetails: TransportItemDetails? = null,
    val error: String? = null
)

sealed class BookingDetailsEvent {
    data class CategorySelected(val category: String) : BookingDetailsEvent()
    data class WeightChanged(val weight: String) : BookingDetailsEvent()
    data class DimensionsChanged(val dimensions: String) : BookingDetailsEvent()
    data class TruckTypeSelected(val truckType: TruckType) : BookingDetailsEvent()
    data class SpecialHandlingChanged(val enabled: Boolean) : BookingDetailsEvent()
    data class DescriptionChanged(val description: String) : BookingDetailsEvent()
    data object Submit : BookingDetailsEvent()
    data object Reset : BookingDetailsEvent()
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