package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LocalXireColors

@Composable
fun XirePromptDialog(
    title: String,
    initialValue: String? = null,
    confirmLabel: String = "Save",
    isDestructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val xireColors = LocalXireColors.current
    var value by remember(initialValue) { mutableStateOf(initialValue.orEmpty()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, xireColors.border)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (initialValue != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prompt_dialog_input")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = androidx.compose.ui.Alignment.End)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, xireColors.border)
                    ) {
                        Text("Cancel", fontFamily = InterFontFamily)
                    }
                    Button(
                        onClick = {
                            if (initialValue == null || value.trim().isNotEmpty()) {
                                onConfirm(value.trim())
                            }
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDestructive) xireColors.card else MaterialTheme.colorScheme.primary,
                            contentColor = if (isDestructive) xireColors.danger else MaterialTheme.colorScheme.onPrimary
                        ),
                        border = if (isDestructive) BorderStroke(1.dp, xireColors.border) else null,
                        modifier = Modifier.testTag("prompt_dialog_confirm")
                    ) {
                        Text(confirmLabel, fontFamily = InterFontFamily)
                    }
                }
            }
        }
    }
}
