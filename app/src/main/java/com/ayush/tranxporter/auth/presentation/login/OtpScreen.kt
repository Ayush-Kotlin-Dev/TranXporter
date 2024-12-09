package com.ayush.tranxporter.auth.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OtpScreen(
    state: OtpState,
    focusRequesters: List<FocusRequester>,
    onAction: (OtpAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.code.forEachIndexed { index, number ->
                OtpInputField(
                    number = number,
                    focusRequester = focusRequesters[index],
                    onFocusChanged = { isFocused ->
                        if(isFocused) {
                            onAction(OtpAction.OnChangeFieldFocused(index))
                        }
                    },
                    onNumberChanged = { newNumber ->
                        onAction(OtpAction.OnEnterNumber(newNumber, index))
                    },
                    onKeyboardBack = {
                        onAction(OtpAction.OnKeyboardBack)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                )
            }
        }

        state.isValid?.let { isValid ->
            Text(
                text = if(isValid) "OTP is valid!" else "OTP is invalid!",
                color = if(isValid) Color(0xFF4CAF50) else Color(0xFFE53935),
                fontSize = 16.sp
            )
        }
    }
}