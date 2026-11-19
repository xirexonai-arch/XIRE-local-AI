package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LocalModelEntity
import com.example.data.model.DownloadProgress
import com.example.data.model.EngineStatus
import com.example.data.model.StarterModels
import com.example.ui.components.XirePromptDialog
import com.example.ui.components.formatSizeMb
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LocalXireColors
import com.example.ui.theme.XireBronze

@Composable
fun DownloadedModelsScreen(
    installedModels: List<LocalModelEntity>,
    selectedModelName: String,
    engineStatus: EngineStatus,
    downloadProgress: DownloadProgress?,
    onBack: () -> Unit,
    onSelectModel: (String) -> Unit,
    onRemoveModel: (String) -> Unit,
    onOpenDownloadModels: () -> Unit
) {
    val xireColors = LocalXireColors.current
    var modelToDelete by remember { mutableStateOf<LocalModelEntity?>(null) }

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
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = xireColors.card,
                    border = BorderStroke(1.dp, xireColors.border),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                        .testTag("downloaded_models_back_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back to chat",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Downloaded Models",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onOpenDownloadModels,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, xireColors.border),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("open_download_models_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Download",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (downloadProgress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = xireColors.card,
                    border = BorderStroke(1.dp, xireColors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Downloading ${downloadProgress.filename}",
                            fontFamily = InterFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (downloadProgress.totalBytes > 0L) {
                            LinearProgressIndicator(
                                progress = { downloadProgress.fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(9.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = xireColors.border
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${formatSizeMb(downloadProgress.bytesRead)} / ${formatSizeMb(downloadProgress.totalBytes)} · ${downloadProgress.percent}%",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = xireColors.muted
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(9.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = xireColors.border
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (installedModels.isEmpty()) {
                // Empty state when no downloaded models are stored in the app
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = xireColors.card,
                        border = BorderStroke(1.dp, xireColors.border),
                        modifier = Modifier
                            .widthIn(max = 440.dp)
                            .fillMaxWidth()
                            .testTag("downloaded_models_empty_state")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(XireBronze.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudDownload,
                                    contentDescription = "No downloaded models",
                                    tint = XireBronze,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Empty — please download a model",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Models you download or import are stored privately inside the app and appear here for selection.",
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = xireColors.muted,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            Button(
                                onClick = onOpenDownloadModels,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .height(50.dp)
                                    .testTag("empty_download_model_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Download a model",
                                    fontFamily = InterFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Stored in app (${installedModels.size}) · Tap to select for chat",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = xireColors.muted,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(installedModels, key = { it.name }) { model ->
                        val isSelected = model.name == selectedModelName
                        val starterMatch = remember(model.name) {
                            StarterModels.recommended.firstOrNull { it.filename == model.name }
                        }
                        val statusLabel = when {
                            isSelected && engineStatus is EngineStatus.Loading && engineStatus.modelName == model.name ->
                                "Selected · Loading"
                            isSelected && engineStatus is EngineStatus.Ready && engineStatus.modelName == model.name ->
                                "Selected · Ready"
                            isSelected -> "Selected"
                            else -> "Stored in app"
                        }

                        Surface(
                            onClick = { onSelectModel(model.name) },
                            shape = RoundedCornerShape(20.dp),
                            color = xireColors.card,
                            border = BorderStroke(
                                width = if (isSelected) 1.8.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else xireColors.border
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("downloaded_model_card_${model.name}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            } else {
                                                xireColors.sidebar
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Memory,
                                        contentDescription = null,
                                        tint = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = starterMatch?.displayName ?: model.name,
                                        fontFamily = InterFontFamily,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (starterMatch != null) {
                                        Text(
                                            text = model.name,
                                            fontFamily = InterFontFamily,
                                            fontSize = 12.sp,
                                            color = xireColors.muted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "${formatSizeMb(model.sizeBytes)} · $statusLabel · File view",
                                        fontFamily = InterFontFamily,
                                        fontSize = 12.5.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else xireColors.muted
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = "Selected model",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { modelToDelete = model },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("remove_downloaded_model_${model.name}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Delete ${model.name}",
                                        tint = xireColors.danger,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    item(key = "download_more_footer") {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onOpenDownloadModels,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, xireColors.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .height(48.dp)
                                .testTag("download_more_models_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download another model",
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    modelToDelete?.let { target ->
        XirePromptDialog(
            title = "Remove ${target.name}?",
            initialValue = null,
            confirmLabel = "Delete",
            isDestructive = true,
            onDismiss = { modelToDelete = null },
            onConfirm = {
                onRemoveModel(target.name)
                modelToDelete = null
            }
        )
    }
}
