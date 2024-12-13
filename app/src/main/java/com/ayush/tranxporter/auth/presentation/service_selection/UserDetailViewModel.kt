package com.ayush.tranxporter.auth.presentation.service_selection


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.tranxporter.core.components.UserType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserDetailsViewModel : ViewModel() {
    private val _state = MutableStateFlow(UserDetailsState())
    val state = _state.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var userType: UserType = UserType.CONSUMER


    fun updateName(name: String) {
        _state.update { it.copy(
            name = name,
            nameError = null,
            completedFields = it.copy(name = name).calculateCompletedFields(userType)
        ) }
    }

    fun updateEmail(email: String) {
        _state.update { it.copy(
            email = email,
            emailError = null,
            completedFields = it.copy(email = email).calculateCompletedFields(userType)
        ) }
    }

    fun updatePhone(phone: String) {
        _state.update { it.copy(
            phone = phone,
            phoneError = null,
            completedFields = it.copy(phone = phone).calculateCompletedFields(userType)
        ) }
    }

    fun updateVehicleNumber(number: String) {
        _state.update { it.copy(
            vehicleNumber = number,
            vehicleNumberError = null,
            completedFields = it.copy(vehicleNumber = number).calculateCompletedFields(userType)
        ) }
    }

    fun updateDriverLicense(license: String) {
        _state.update { it.copy(
            driverLicense = license,
            driverLicenseError = null,
            completedFields = it.copy(driverLicense = license).calculateCompletedFields(userType)
        ) }
    }


    fun validateFields(userType: UserType): Boolean {
        var isValid = true
        _state.update { state ->
            state.copy(
                nameError = if (state.name.isBlank()) "Name is required" else null,
                emailError = if (!state.email.isValidEmail()) "Invalid email" else null,
                phoneError = if (!state.phone.isValidPhone()) "Invalid phone number" else null,
                vehicleNumberError = if (userType == UserType.TRUCK_OWNER &&
                    state.vehicleNumber.isBlank()
                ) "Vehicle number is required" else null,
                driverLicenseError = if (userType == UserType.TRUCK_OWNER &&
                    state.driverLicense.isBlank()
                ) "License number is required" else null
            )
        }

        _state.value.run {
            isValid = nameError == null && emailError == null && phoneError == null &&
                    (userType != UserType.TRUCK_OWNER ||
                            (vehicleNumberError == null && driverLicenseError == null))
        }

        return isValid
    }

    fun saveUserDetails(userType: UserType) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }

                auth.currentUser?.let { user ->
                    // Create user document
                    val userData = buildMap {
                        put("userType", userType.name)
                        put("name", _state.value.name)
                        put("email", _state.value.email)
                        put("phone", _state.value.phone)
                        put("createdAt", System.currentTimeMillis())

                        if (userType == UserType.TRUCK_OWNER) {
                            put("vehicleNumber", _state.value.vehicleNumber)
                            put("driverLicense", _state.value.driverLicense)
                        }
                    }

                    firestore.collection("users")
                        .document(user.uid)
                        .set(userData)
                        .await()

                    _state.update { it.copy(
                        isLoading = false,
                        isSuccess = true
                    ) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred"
                ) }
            }
        }
    }

}

data class UserDetailsState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val vehicleNumber: String = "",
    val driverLicense: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val vehicleNumberError: String? = null,
    val driverLicenseError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val completedFields: Int = 0  // Add this field
) {
    // Add a function to calculate completed fields
    fun calculateCompletedFields(userType: UserType): Int {
        var completed = 0
        if (name.isNotBlank()) completed++
        if (email.isValidEmail()) completed++
        if (phone.isValidPhone()) completed++

        if (userType == UserType.TRUCK_OWNER) {
            if (vehicleNumber.isNotBlank()) completed++
            if (driverLicense.isNotBlank()) completed++
        }
        return completed
    }
}

private fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

private fun String.isValidPhone(): Boolean {
    return this.length == 10 && this.all { it.isDigit() }
}