package com.ayush.tranxporter.user.presentation.bookingdetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ayush.tranxporter.user.data.BookingStateHolder
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent

class BookingDetailsViewModel(
    private val stateHolder: BookingStateHolder

) : ViewModel(), KoinComponent {

    var state by mutableStateOf(BookingDetailsState())
        private set

    fun onEvent(event: BookingDetailsEvent) {
        when (event) {
            is BookingDetailsEvent.CategorySelected -> {
                updateState { copy(selectedCategory = event.category) }
                validateForm()
            }

            is BookingDetailsEvent.WeightChanged -> {
                updateState {
                    copy(
                        weight = event.weight,
                        weightError = validateWeight(event.weight)
                    )
                }
                validateForm()
            }

            is BookingDetailsEvent.DimensionsChanged -> {
                updateState {
                    copy(
                        dimensions = event.dimensions,
                        dimensionsError = validateDimensions(event.dimensions)
                    )
                }
                validateForm()
            }

            is BookingDetailsEvent.TruckTypeSelected -> {
                updateState { copy(selectedTruckType = event.truckType) }
                validateForm()
            }

            is BookingDetailsEvent.SpecialHandlingChanged -> {
                updateState { copy(specialHandling = event.enabled) }
            }

            is BookingDetailsEvent.DescriptionChanged -> {
                updateState { copy(description = event.description) }
            }

            BookingDetailsEvent.Submit -> submitForm()
            BookingDetailsEvent.Reset -> resetForm()
            BookingDetailsEvent.ResetSubmission -> {
                updateState { copy(submittedDetails = null) }
            }
        }
    }

    // Helper function to update state
    private fun updateState(update: BookingDetailsState.() -> BookingDetailsState) {
        state = state.update()
    }

    private fun validateWeight(weight: String): String? =
        when {
            weight.isEmpty() -> "Weight is required"
            weight.toDoubleOrNull() == null -> "Please enter a valid number"
            weight.toDouble() <= 0 -> "Weight must be greater than 0"
            weight.toDouble() > 10000 -> "Weight cannot exceed 10,000 kg" // Added reasonable limit
            else -> null
        }

    private fun validateDimensions(dimensions: String): String? =
        when {
            dimensions.isEmpty() -> "Dimensions are required"
            !dimensions.matches(Regex("""^\d+\s*x\s*\d+\s*x\s*\d+$""")) ->
                "Format should be: length x width x height"

            else -> {
                try {
                    val (l, w, h) = dimensions.split("x").map { it.trim().toInt() }
                    when {
                        l <= 0 || w <= 0 || h <= 0 -> "Dimensions must be greater than 0"
                        l > 1000 || w > 1000 || h > 1000 -> "Dimensions cannot exceed 1000 cm"
                        else -> null
                    }
                } catch (e: Exception) {
                    "Invalid dimensions format"
                }
            }
        }

    private fun validateForm() {
        val isValid = with(state) {
            selectedCategory != null &&
                    weightError == null &&
                    dimensionsError == null &&
                    selectedTruckType != null &&
                    weight.isNotEmpty() &&
                    dimensions.isNotEmpty()
        }
        updateState { copy(isFormValid = isValid) }
    }

    private fun submitForm() {
        if (!state.isFormValid) return

        updateState { copy(isLoading = true) }

        try {
            val details = TransportItemDetails(
                category = state.selectedCategory!!,
                weight = state.weight.toDouble(),
                dimensions = state.dimensions,
                truckType = state.selectedTruckType!!,
                specialHandling = state.specialHandling,
                description = state.description
            )

            stateHolder.updateBookingDetails(details)

            updateState {
                copy(
                    submittedDetails = details,
                    isLoading = false,
                    error = null
                )
            }
        } catch (e: Exception) {
            updateState {
                copy(
                    isLoading = false,
                    error = "Failed to submit form: ${e.message}"
                )
            }
        }
    }

    private fun resetForm() {
        state = BookingDetailsState()
    }


    fun hasSubmittedDetails(): Boolean = state.submittedDetails != null

    fun getSubmittedDetails(): TransportItemDetails? = stateHolder.bookingDetails

    fun clearError() {
        updateState { copy(error = null) }
    }
}

// Improved state class with better organization
data class BookingDetailsState(
    // Form inputs
    val selectedCategory: String? = null,
    val weight: String = "",
    val dimensions: String = "",
    val selectedTruckType: TruckType? = null,
    val specialHandling: Boolean = false,
    val description: String = "",

    // Validation states
    val weightError: String? = null,
    val dimensionsError: String? = null,
    val isFormValid: Boolean = false,

    // UI states
    val isLoading: Boolean = false,
    val error: String? = null,

    // Submission state
    val submittedDetails: TransportItemDetails? = null
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
    data object ResetSubmission : BookingDetailsEvent() // Add this
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