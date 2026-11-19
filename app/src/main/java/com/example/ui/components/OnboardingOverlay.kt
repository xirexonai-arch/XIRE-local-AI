package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LocalXireColors
import com.example.ui.theme.LoraFontFamily

@Composable
fun OnboardingOverlay(
    onComplete: (name: String, age: Int) -> Unit
) {
    val xireColors = LocalXireColors.current
    val focusManager = LocalFocusManager.current
    var name by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var ageError by remember { mutableStateOf(false) }

    fun submit() {
        val trimmed = name.trim()
        val parsedAge = ageText.toIntOrNull() ?: 0
        val badName = trimmed.isEmpty()
        val badAge = parsedAge !in 1..120
        nameError = badName
        ageError = badAge
        if (!badName && !badAge) {
            focusManager.clearFocus()
            onComplete(trimmed, parsedAge)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "XIRE AI",
                fontFamily = LoraFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp,
                letterSpacing = 0.4.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            OnboardingAnimationArt(size = 216.dp)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Your private, on-device AI",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        if (it.length <= 40) {
                            name = it
                            nameError = false
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Enter your name",
                            fontFamily = InterFontFamily,
                            color = xireColors.muted
                        )
                    },
                    supportingText = if (nameError) {
                        {
                            Text(
                                text = "Please enter your name",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        null
                    },
                    singleLine = true,
                    isError = nameError,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = xireColors.card,
                        unfocusedContainerColor = xireColors.card,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = xireColors.border,
                        errorContainerColor = xireColors.card
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_name_input")
                )

                OutlinedTextField(
                    value = ageText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(3)
                        ageText = digits
                        ageError = false
                    },
                    placeholder = {
                        Text(
                            text = "Enter your age",
                            fontFamily = InterFontFamily,
                            color = xireColors.muted
                        )
                    },
                    supportingText = if (ageError) {
                        {
                            Text(
                                text = "Please enter a valid age (1–120)",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        null
                    },
                    singleLine = true,
                    isError = ageError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submit() }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = xireColors.card,
                        unfocusedContainerColor = xireColors.card,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = xireColors.border,
                        errorContainerColor = xireColors.card
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_age_input")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { submit() },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .height(52.dp)
                    .testTag("onboarding_start_button")
            ) {
                Text(
                    text = "Continue",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.5.sp
                )
            }
        }
    }
}
