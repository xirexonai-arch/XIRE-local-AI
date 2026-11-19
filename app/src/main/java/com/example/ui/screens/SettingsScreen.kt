package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ThemeMode
import com.example.ui.components.XireLogo
import com.example.ui.components.XirePromptDialog
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LocalXireColors
import com.example.ui.viewmodel.XireUiState

@Composable
fun SettingsScreen(
    uiState: XireUiState,
    scrollToInstallGuide: Boolean,
    onBack: () -> Unit,
    onSaveProfile: (name: String, age: Int, maxTokens: Int) -> Unit,
    onSelectTheme: (ThemeMode) -> Unit,
    onManageModels: () -> Unit,
    onDeleteAllChats: () -> Unit
) {
    val xireColors = LocalXireColors.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var name by remember(uiState.userProfile.name) { mutableStateOf(uiState.userProfile.name) }
    var ageText by remember(uiState.userProfile.age) {
        mutableStateOf(if (uiState.userProfile.age > 0) uiState.userProfile.age.toString() else "")
    }
    var maxTokensText by remember(uiState.settings.maxTokens) {
        mutableStateOf(uiState.settings.maxTokens.toString())
    }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var ageError by remember { mutableStateOf(false) }

    var didAutoScroll by remember(scrollToInstallGuide) { mutableStateOf(false) }

    LaunchedEffect(scrollToInstallGuide, scrollState.maxValue) {
        if (scrollToInstallGuide && !didAutoScroll && scrollState.maxValue > 0) {
            didAutoScroll = true
            scrollState.animateScrollTo(scrollState.maxValue / 2)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = xireColors.card,
                    border = BorderStroke(1.dp, xireColors.border),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                        .testTag("settings_back_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Profile card
            SettingsCard {
                Text("Profile", fontFamily = InterFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Saved only on this device.", fontFamily = InterFontFamily, fontSize = 14.sp, color = xireColors.muted)

                SettingsFieldLabel("Name")
                SettingsTextField(
                    value = name,
                    onValueChange = {
                        if (it.length <= 40) {
                            name = it
                            nameError = false
                        }
                    },
                    keyboardType = KeyboardType.Text,
                    isError = nameError
                )

                SettingsFieldLabel("Age")
                SettingsTextField(
                    value = ageText,
                    onValueChange = {
                        ageText = it.filter { c -> c.isDigit() }.take(3)
                        ageError = false
                    },
                    keyboardType = KeyboardType.Number,
                    isError = ageError
                )

                SettingsFieldLabel("Max reply length (tokens)")
                SettingsTextField(
                    value = maxTokensText,
                    onValueChange = { maxTokensText = it.filter { c -> c.isDigit() }.take(4) },
                    keyboardType = KeyboardType.Number
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val trimmed = name.trim()
                        val parsedAge = ageText.toIntOrNull() ?: 0
                        val parsedTokens = (maxTokensText.toIntOrNull() ?: 512).coerceIn(32, 4096)
                        val badName = trimmed.isEmpty()
                        val badAge = parsedAge !in 1..120
                        nameError = badName
                        ageError = badAge
                        if (!badName && !badAge) {
                            onSaveProfile(trimmed, parsedAge, parsedTokens)
                        }
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("save_settings_button")
                ) {
                    Text(
                        text = if (uiState.settingsSavedFeedback) "Saved ✓" else "Save changes",
                        fontFamily = InterFontFamily
                    )
                }
            }

            // Appearance card
            SettingsCard {
                Text("Appearance", fontFamily = InterFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Choose how XIRE AI looks.", fontFamily = InterFontFamily, fontSize = 14.sp, color = xireColors.muted)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeOptionButton(
                        label = "Light",
                        icon = Icons.Outlined.LightMode,
                        selected = uiState.settings.themeMode == ThemeMode.LIGHT,
                        onClick = { onSelectTheme(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionButton(
                        label = "Dark",
                        icon = Icons.Outlined.DarkMode,
                        selected = uiState.settings.themeMode == ThemeMode.DARK,
                        onClick = { onSelectTheme(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionButton(
                        label = "System",
                        icon = Icons.Outlined.Computer,
                        selected = uiState.settings.themeMode == ThemeMode.SYSTEM,
                        onClick = { onSelectTheme(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Your models card
            SettingsCard {
                Text("Your models", fontFamily = InterFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Add, switch or remove AI models stored on this device.",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    color = xireColors.muted
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onManageModels,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("manage_models_button")
                ) {
                    Text("Manage models", fontFamily = InterFontFamily)
                }
            }

            // How to get a model card
            SettingsCard {
                Text("How to get a model", fontFamily = InterFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                OrderedStep(1, "Tap Manage models above to quick-install a lightweight starter model, or open Hugging Face GGUF models in your browser.")
                OrderedStep(2, "Search for a small instruct model in GGUF format, such as Qwen2.5 0.5B Instruct, Llama 3.2 1B Instruct, or SmolLM2 360M Instruct.")
                OrderedStep(3, "Open Files and versions and download a single file ending in .gguf (Q4_K_M or Q4_0 gives the best balance of size and quality).")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Which size? Phones work smoothly with 0.3B to 1.5B Q4_K_M models. Larger files take more RAM.",
                    fontFamily = InterFontFamily,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = xireColors.muted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExternalLinkText("Hugging Face · GGUF") {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://huggingface.co/models?library=gguf&sort=trending")
                                )
                            )
                        }
                    }
                    ExternalLinkText("llama.cpp engine") {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/ggml-org/llama.cpp")
                                )
                            )
                        }
                    }
                }
            }

            // How to install a model card
            SettingsCard {
                Text("How to install a model", fontFamily = InterFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                OrderedStep(1, "Tap the model selector button in the message bar (or Manage models above) and choose Download.")
                OrderedStep(2, "Tap Choose model file to pick a downloaded .gguf file from your device, or tap Get on a recommended starter model.")
                OrderedStep(3, "Wait for the progress bar to finish. The model is saved privately inside the app.")
                OrderedStep(4, "Pick your downloaded model from the Downloaded Models page and send your message.")
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "You can delete the original download afterwards. Tap Remove on any model to free its space.",
                    fontFamily = InterFontFamily,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = xireColors.muted
                )
            }

            // Data card
            SettingsCard {
                Text("Data", fontFamily = InterFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Chats are stored only on this device.", fontFamily = InterFontFamily, fontSize = 14.sp, color = xireColors.muted)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showDeleteAllConfirm = true },
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, xireColors.border),
                    modifier = Modifier.testTag("delete_all_chats_button")
                ) {
                    Text("Delete all chats", fontFamily = InterFontFamily, color = xireColors.danger)
                }
            }

            // About card
            SettingsCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    XireLogo(size = 72.dp, spin = true)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "XIRE AI",
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, letterSpacing = 1.4.sp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Private AI that runs on your device.",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        color = xireColors.muted
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                AboutRow("Version", "1.0")
                AboutRow("Developer", "XIREXON team")
                AboutRow("Models", "GGUF (llama.cpp)")
                AboutRow("Privacy", "Chats never leave your device")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDeleteAllConfirm) {
        XirePromptDialog(
            title = "Delete all chats?",
            initialValue = null,
            confirmLabel = "Delete all",
            isDestructive = true,
            onDismiss = { showDeleteAllConfirm = false },
            onConfirm = {
                showDeleteAllConfirm = false
                onDeleteAllChats()
            }
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val xireColors = LocalXireColors.current
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = xireColors.card,
        border = BorderStroke(1.dp, xireColors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsFieldLabel(text: String) {
    val xireColors = LocalXireColors.current
    Text(
        text = text,
        fontFamily = InterFontFamily,
        fontSize = 13.sp,
        color = xireColors.muted,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    isError: Boolean = false
) {
    val xireColors = LocalXireColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = xireColors.card,
            unfocusedContainerColor = xireColors.card,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = xireColors.border
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ThemeOptionButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val xireColors = LocalXireColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else xireColors.border
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                fontFamily = InterFontFamily,
                fontSize = 13.5.sp
            )
        }
    }
}

@Composable
private fun OrderedStep(number: Int, text: String) {
    val xireColors = LocalXireColors.current
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$number.",
            fontFamily = InterFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = text,
            fontFamily = InterFontFamily,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = xireColors.muted
        )
    }
}

@Composable
private fun ExternalLinkText(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        fontFamily = InterFontFamily,
        fontSize = 13.5.sp,
        fontWeight = FontWeight.SemiBold,
        textDecoration = TextDecoration.Underline,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    )
}

@Composable
private fun AboutRow(label: String, value: String) {
    val xireColors = LocalXireColors.current
    HorizontalDivider(color = xireColors.border, thickness = 1.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontFamily = InterFontFamily, fontSize = 14.sp, color = xireColors.muted)
        Text(value, fontFamily = InterFontFamily, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
