package com.ayush.tranxporter.auth.presentation.login

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ayush.tranxporter.auth.presentation.service_selection.ServiceSelectionScreen
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

class AuthScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = koinViewModel<AuthViewModel>()
        val state by viewModel.state.collectAsState()
        val context = LocalContext.current
        val activity = remember { context as Activity }
        val focusRequesters = remember { List(6) { FocusRequester() } }
        val focusManager = LocalFocusManager.current
        val keyboardManager = LocalSoftwareKeyboardController.current
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(state.otpState.focusedIndex) {
            state.otpState.focusedIndex?.let { index ->
                focusRequesters.getOrNull(index)?.requestFocus()
            }
        }

        LaunchedEffect(state.otpState.code, keyboardManager) {
            val allNumbersEntered = state.otpState.code.none { it == null }
            if (allNumbersEntered) {
                focusRequesters.forEach { it.freeFocus() }
                focusManager.clearFocus()
                keyboardManager?.hide()
            }
        }

        // Handle authentication success
        LaunchedEffect(state.isAuthenticated) {
            if (state.isAuthenticated) {
                navigator.replaceAll(
                    ServiceSelectionScreen()
                )

            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedVisibility(
                visible = state.showOtpInput,
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut() + slideOutHorizontally()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.onEvent(AuthEvent.ShowPhoneInput) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                // Header Section
                if (!state.showOtpInput) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Welcome",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Enter your mobile number to continue",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Phone Input Section
                AnimatedVisibility(
                    visible = !state.showOtpInput,
                    enter = fadeIn() + slideInHorizontally(),
                    exit = fadeOut() + slideOutHorizontally()
                ) {
                    PhoneNumberInput(
                        phoneNumber = state.phoneNumber,
                        isValid = state.isPhoneValid,
                        onPhoneNumberChange = { viewModel.onEvent(AuthEvent.OnPhoneNumberChange(it)) },
                        onSubmit = {
                            keyboardManager?.hide()
                            focusManager.clearFocus()
                            viewModel.onEvent(AuthEvent.OnSubmitPhone, activity)
                        },
                        keyboardManager = keyboardManager,  // Add this
                        focusManager = focusManager  // Add this
                    )
                }

                // OTP Input Section
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

            // Loading Indicator
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            // Do nothing
                        }
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            state.error?.let { error ->
                LaunchedEffect(error) {
                    delay(3000)
                    viewModel.onEvent(AuthEvent.DismissError)
                }
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Text(error)
                }
            }
        }
    }

    @Composable
    private fun PhoneNumberInput(
        phoneNumber: String,
        isValid: Boolean,
        onPhoneNumberChange: (String) -> Unit,
        onSubmit: () -> Unit,
        keyboardManager: SoftwareKeyboardController?,
        focusManager: FocusManager
    ) {
        var termsAccepted by remember { mutableStateOf(false) }
        val context = LocalContext.current
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "+91",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Divider(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { newValue ->
                            if (newValue.length <= 10) {
                                onPhoneNumberChange(newValue)
                                // If length reaches 10, hide keyboard
                                if (newValue.length == 10) {
                                    keyboardManager?.hide()
                                    focusManager.clearFocus()
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { if (isValid) onSubmit() }
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }

            // Terms and Conditions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.Start
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { termsAccepted = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )

                val annotatedString = buildAnnotatedString {
                    append("I agree to the ")
                    pushStringAnnotation(
                        tag = "terms",
                        annotation = "https://www.example.com/terms"
                    )
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("Terms & Conditions")
                    }
                    pop()
                }

                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(top = 12.dp),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(
                            tag = "terms",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let { annotation ->
                            // Open URL in browser
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                            context.startActivity(intent)
                        }
                    }
                )
            }

            Button(
                onClick = onSubmit,
                enabled = isValid && termsAccepted,  // Enable only if terms are accepted
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Continue",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
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
    var remainingSeconds by remember { mutableStateOf(30) }
    var canResend by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
        canResend = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Enter verification code",
            style = MaterialTheme.typography.headlineSmall
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+91 ${state.phoneNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(
                onClick = { viewModel.onEvent(AuthEvent.ShowPhoneInput) },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "Wrong number?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            OtpScreen(
                state = state.otpState,
                focusRequesters = focusRequesters,
                onAction = { action ->
                    when (action) {
                        is OtpAction.OnEnterNumber -> {
                            if (action.number != null) {
                                focusRequesters[action.index].freeFocus()
                            }
                        }

                        else -> Unit
                    }
                    viewModel.onEvent(AuthEvent.OnOtpAction(action))
                }
            )
        }

        if (canResend) {
            TextButton(
                onClick = {
                    viewModel.onEvent(AuthEvent.OnResendOtp)
                    remainingSeconds = 30
                    canResend = false
                }
            ) {
                Text(
                    text = "Resend OTP",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Text(
                text = "Resend OTP in ${remainingSeconds}s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    .height(56.dp)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    "Verify",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}