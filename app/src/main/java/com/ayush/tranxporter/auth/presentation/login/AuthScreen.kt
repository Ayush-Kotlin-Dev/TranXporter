package com.ayush.tranxporter.auth.presentation.login

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.google.android.gms.auth.api.Auth
import org.koin.androidx.compose.koinViewModel

data class AuthScreen(
    val onAuthenticated: () -> Unit,
) : Screen {

    @Composable
    override fun Content() {
        val viewModel = koinViewModel<AuthViewModel>()
        val state by viewModel.state.collectAsState()
        val context = LocalContext.current
        val activity = remember { context as Activity }
        val focusRequesters = remember { List(6) { FocusRequester() } }
        val focusManager = LocalFocusManager.current
        val keyboardManager = LocalSoftwareKeyboardController.current

        // Handle focus changes
        LaunchedEffect(state.otpState.focusedIndex) {
            state.otpState.focusedIndex?.let { index ->
                focusRequesters.getOrNull(index)?.requestFocus()
            }
        }

        // Handle keyboard and focus when OTP is complete
        LaunchedEffect(state.otpState.code, keyboardManager) {
            val allNumbersEntered = state.otpState.code.none { it == null }
            if(allNumbersEntered) {
                focusRequesters.forEach { it.freeFocus() }
                focusManager.clearFocus()
                keyboardManager?.hide()
            }
        }

        // Handle authentication success
        LaunchedEffect(state.isAuthenticated) {
            if (state.isAuthenticated) {
                onAuthenticated()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedVisibility(
                    visible = !state.showOtpInput,
                    enter = fadeIn() + slideInHorizontally(),
                    exit = fadeOut() + slideOutHorizontally()
                ) {
                    PhoneNumberInput(
                        phoneNumber = state.phoneNumber,
                        isValid = state.isPhoneValid,
                        onPhoneNumberChange = { viewModel.onEvent(AuthEvent.OnPhoneNumberChange(it)) },
                        onSubmit = { viewModel.onEvent(AuthEvent.OnSubmitPhone, activity) }
                    )
                }
                AnimatedVisibility(
                    visible = state.showOtpInput,
                    enter = fadeIn() + slideInHorizontally(),
                    exit = fadeOut() + slideOutHorizontally()
                ) {
                    OtpInput(
                        state = state,
                        focusRequesters = focusRequesters,
                        onAction = { viewModel.onEvent(AuthEvent.OnOtpAction(it)) },
                        viewModel = viewModel
                    )
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Text(error)
                }
            }
        }
    }
}
@Composable
private fun PhoneNumberInput(
    phoneNumber: String,
    isValid: Boolean,
    onPhoneNumberChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Enter your phone number",
            style = MaterialTheme.typography.headlineSmall
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+91",
                style = MaterialTheme.typography.bodyLarge
            )
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = onSubmit,
            enabled = isValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send Code")
        }
    }
}

@Composable
fun OtpInput(
    state: AuthState,
    focusRequesters: List<FocusRequester>,
    onAction: (OtpAction) -> Unit,
    viewModel: AuthViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)  // Space between elements
    ) {
        Text(
            text = "Enter verification code",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Code sent to +91 ${state.phoneNumber}",
            style = MaterialTheme.typography.bodyMedium
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            OtpScreen(
                state = state.otpState,
                focusRequesters = focusRequesters,
                onAction = { action ->
                    when(action) {
                        is OtpAction.OnEnterNumber -> {
                            if(action.number != null) {
                                focusRequesters[action.index].freeFocus()
                            }
                        }
                        else -> Unit
                    }
                    viewModel.onEvent(AuthEvent.OnOtpAction(action))
                }
            )
        }

        AnimatedVisibility(
            visible = state.otpState.isValid == true,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally()
        ) {
            Button(
                onClick = { viewModel.onEvent(AuthEvent.OnVerifyOtp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)  // Add some space above button
            ) {
                Text("Verify")
            }
        }
    }
}