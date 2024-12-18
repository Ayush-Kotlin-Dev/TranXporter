package com.ayush.tranxporter.auth.presentation.service_selection


import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ayush.tranxporter.HomeScreen
import com.ayush.tranxporter.R
import com.ayush.tranxporter.core.components.UserType
import org.koin.androidx.compose.koinViewModel

data class UserDetailsScreen(
    val userType: UserType,
) : Screen {
    @Composable
    override fun Content() {


        val viewModel = koinViewModel<UserDetailsViewModel>()
        val state by viewModel.state.collectAsState()
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val nameFocus = remember { FocusRequester() }
        val emailFocus = remember { FocusRequester() }
        val phoneFocus = remember { FocusRequester() }
        val vehicleNumberFocus = remember { FocusRequester() }
        val licenseFocus = remember { FocusRequester() }

        val scrollState = rememberScrollState()
        // Format vehicle number
        val vehicleNumberTransformation = remember {
            VisualTransformation { text ->
                val trimmed = text.text.replace(" ", "").uppercase()
                if (trimmed.isEmpty()) return@VisualTransformation TransformedText(
                    AnnotatedString(""),
                    OffsetMapping.Identity
                )

                val formatted = buildString {
                    trimmed.forEachIndexed { index, c ->
                        if (index == 2 || index == 4 || index == 6) append(" ")
                        append(c)
                    }
                }
                TransformedText(
                    AnnotatedString(formatted),
                    object : OffsetMapping {
                        override fun originalToTransformed(offset: Int): Int =
                            when {
                                offset <= 2 -> offset
                                offset <= 4 -> offset + 1
                                offset <= 6 -> offset + 2
                                else -> offset + 3
                            }

                        override fun transformedToOriginal(offset: Int): Int =
                            when {
                                offset <= 2 -> offset
                                offset <= 5 -> offset - 1
                                offset <= 8 -> offset - 2
                                else -> offset - 3
                            }
                    }
                )
            }
        }

        // Handle error messages and success
        LaunchedEffect(state.error) {
            state.error?.let { error ->
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        }

        LaunchedEffect(state.isSuccess) {
            if (state.isSuccess) {
                Toast.makeText(context, "Details saved successfully", Toast.LENGTH_LONG).show()
                navigator.replaceAll(HomeScreen())

            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                FormProgressIndicator(
                    currentStep = state.completedFields,
                    totalSteps = if (userType == UserType.TRUCK_OWNER) 7 else 4
                )

                Text(
                    text = if (userType == UserType.CONSUMER)
                        "Complete Your Profile"
                    else
                        "Vehicle Details",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                FormField(
                    value = state.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = "Full Name",
                    icon = R.drawable.ic_person,
                    error = state.nameError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { emailFocus.requestFocus() }),
                    focusRequester = nameFocus
                )

                Spacer(modifier = Modifier.height(16.dp))

                FormField(
                    value = state.email,
                    onValueChange = { viewModel.updateEmail(it) },
                    label = "Email",
                    icon = R.drawable.ic_email,
                    error = state.emailError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { phoneFocus.requestFocus() }
                    ),
                    focusRequester = emailFocus
                )

                Spacer(modifier = Modifier.height(16.dp))

                FormField(
                    value = state.phone,
                    onValueChange = { if (it.length <= 10) viewModel.updatePhone(it) },
                    label = "Phone Number",
                    icon = R.drawable.ic_phone,
                    error = state.phoneError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = if (userType == UserType.TRUCK_OWNER) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { if (userType == UserType.TRUCK_OWNER) vehicleNumberFocus.requestFocus() }
                    ),
                    focusRequester = phoneFocus
                )

                if (userType == UserType.TRUCK_OWNER) {
                    Spacer(modifier = Modifier.height(16.dp))

                    FormField(
                        value = state.vehicleNumber,
                        onValueChange = { if (it.length <= 10) viewModel.updateVehicleNumber(it.uppercase()) },
                        label = "Vehicle Number (e.g., CG10 AB 1234)",
                        icon = R.drawable.ic_car,
                        error = state.vehicleNumberError,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { licenseFocus.requestFocus() }
                        ),
                        visualTransformation = vehicleNumberTransformation,
                        focusRequester = vehicleNumberFocus
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FormField(
                        value = state.driverLicense,
                        onValueChange = { viewModel.updateDriverLicense(it.uppercase()) },
                        label = "Driving License Number",
                        icon = R.drawable.ic_car,
                        error = state.driverLicenseError,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        focusRequester = licenseFocus
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val vehicleTypes = listOf("Truck", "Van", "Pickup", "Other")

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Vehicle Type",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // First row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                vehicleTypes.take(2).forEach { type ->
                                    Button(
                                        onClick = {
                                            viewModel.updateVehicleType(type)
                                            if (type != "Other") {
                                                viewModel.updateCustomVehicleType("")
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = if (state.vehicleType == type) {
                                            MaterialTheme.colorScheme.primary.let { primaryColor ->
                                                ButtonDefaults.buttonColors(containerColor = primaryColor)
                                            }
                                        } else {
                                            ButtonDefaults.outlinedButtonColors()
                                        }
                                    ) {
                                        Text(text = type)
                                    }
                                }
                            }

                            // Second row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                vehicleTypes.drop(2).forEach { type ->
                                    Button(
                                        onClick = {
                                            viewModel.updateVehicleType(type)
                                            if (type != "Other") {
                                                viewModel.updateCustomVehicleType("")
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = if (state.vehicleType == type) {
                                            MaterialTheme.colorScheme.primary.let { primaryColor ->
                                                ButtonDefaults.buttonColors(containerColor = primaryColor)
                                            }
                                        } else {
                                            ButtonDefaults.outlinedButtonColors()
                                        }
                                    ) {
                                        Text(text = type)
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = state.vehicleType == "Other") {
                            FormField(
                                value = state.customVehicleType,
                                onValueChange = { viewModel.updateCustomVehicleType(it) },
                                label = "Specify Vehicle Type",
                                icon = R.drawable.ic_car,
                                error = state.customVehicleTypeError,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done
                                ),
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (viewModel.validateFields(userType)) {
                            viewModel.saveUserDetails(userType)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Submit")
                    }
                }
            }

            AnimatedVisibility(
                visible = state.isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .clickable(enabled = false) { /* Prevent clicks while loading */ },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Saving details...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            nameFocus.requestFocus()
        }
    }


    @Composable
    private fun FormProgressIndicator(
        currentStep: Int,
        totalSteps: Int,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(totalSteps) { step ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            color = if (step <= currentStep)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }

    @Composable
    private fun FormField(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        @DrawableRes icon: Int,
        modifier: Modifier = Modifier,
        error: String? = null,
        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
        keyboardActions: KeyboardActions = KeyboardActions.Default,
        visualTransformation: VisualTransformation = VisualTransformation.None,
        focusRequester: FocusRequester? = null
    ) {
        Column(modifier = modifier) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        tint = if (error != null)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                isError = error != null,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = visualTransformation,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                )
            )
            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
    }
}