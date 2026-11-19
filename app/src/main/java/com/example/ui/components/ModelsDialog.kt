package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.LocalModelEntity
import com.example.data.model.DownloadProgress
import com.example.data.model.StarterModelSpec
import com.example.data.model.StarterModels
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LocalXireColors

@Composable
fun ModelsDialog(
    installedModels: List<LocalModelEntity>,
    selectedModelName: String,
    importProgress: Float?,
    downloadProgress: DownloadProgress?,
    partialDownloadBytes: Map<String, Long> = emptyMap(),
    notice: String?,
    onChooseFile: () -> Unit,
    onSelectModel: (String) -> Unit,
    onRemoveModel: (String) -> Unit,
    onDownloadStarter: (StarterModelSpec) -> Unit,
    onCancelDownload: () -> Unit,
    onOpenInstallGuide: () -> Unit,
    onDismiss: () -> Unit
) {
    val xireColors = LocalXireColors.current
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, xireColors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "Download models",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Download a starter GGUF model or choose a .gguf file. All downloaded models are saved to your Downloaded Models page.",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = xireColors.muted
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onChooseFile,
                    enabled = importProgress == null && downloadProgress == null,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("choose_model_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose model file", fontFamily = InterFontFamily)
                }

                if (importProgress != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { importProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(9.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = xireColors.border
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Copying into local storage… ${(importProgress * 100).toInt()}%",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = xireColors.muted
                    )
                }

                if (downloadProgress != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = xireColors.sidebar,
                        border = BorderStroke(1.dp, xireColors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Downloading ${downloadProgress.filename}",
                                    fontFamily = InterFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Cancel",
                                    fontFamily = InterFontFamily,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = xireColors.danger,
                                    modifier = Modifier
                                        .clickable(onClick = onCancelDownload)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (downloadProgress.totalBytes > 0) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress.fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(9.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = xireColors.border
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val speedSuffix = formatSpeedSuffix(downloadProgress.bytesPerSecond)
                                Text(
                                    text = "${formatSizeMb(downloadProgress.bytesRead)} / ${formatSizeMb(downloadProgress.totalBytes)} · ${downloadProgress.percent}%$speedSuffix",
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

                if (!notice.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = notice,
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = xireColors.border, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Quick install (lightweight GGUF)",
                    fontFamily = InterFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = xireColors.muted
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StarterModels.recommended.forEach { spec ->
                        val alreadyInstalled = installedModels.any { it.name == spec.filename }
                        val partialBytes = partialDownloadBytes[spec.filename] ?: 0L
                        val canResume = !alreadyInstalled && partialBytes > 0L
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(xireColors.card)
                                .border(1.dp, xireColors.border, RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = spec.displayName,
                                        fontFamily = InterFontFamily,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (canResume) {
                                            "${spec.sizeLabel} · ${formatSizeMb(partialBytes)} downloaded"
                                        } else {
                                            "${spec.sizeLabel} · ${spec.summary}"
                                        },
                                        fontFamily = InterFontFamily,
                                        fontSize = 11.5.sp,
                                        color = xireColors.muted
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        if (alreadyInstalled) {
                                            onSelectModel(spec.filename)
                                            onDismiss()
                                        } else {
                                            onDownloadStarter(spec)
                                        }
                                    },
                                    enabled = downloadProgress == null && importProgress == null,
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(1.dp, xireColors.border),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 4.dp
                                    ),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    if (!alreadyInstalled) {
                                        Icon(
                                            imageVector = Icons.Outlined.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = when {
                                            alreadyInstalled -> "Saved ✓"
                                            canResume -> "Resume"
                                            else -> "Get"
                                        },
                                        fontFamily = InterFontFamily,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "No model yet?",
                        fontFamily = InterFontFamily,
                        fontSize = 12.5.sp,
                        color = xireColors.muted
                    )
                    Text(
                        text = "Get one on Hugging Face",
                        fontFamily = InterFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://huggingface.co/models?library=gguf&sort=trending")
                                    )
                                )
                            }
                        }
                    )
                    Text("·", fontSize = 12.5.sp, color = xireColors.muted)
                    Text(
                        text = "Install guide",
                        fontFamily = InterFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.clickable {
                            onDismiss()
                            onOpenInstallGuide()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, xireColors.border),
                        modifier = Modifier.testTag("models_done_button")
                    ) {
                        Text("Done", fontFamily = InterFontFamily)
                    }
                }
            }
        }
    }
}

fun formatSizeMb(bytes: Long): String {
    val mb = bytes / 1_048_576.0
    return if (bytes >= 1_000_000_000L) {
        String.format("%.0f MB", mb)
    } else {
        String.format("%.1f MB", mb)
    }
}

private fun formatSpeedSuffix(bytesPerSecond: Long): String {
    if (bytesPerSecond <= 0L) return ""
    val mbPerSec = bytesPerSecond / 1_048_576.0
    return if (mbPerSec >= 1.0) {
        String.format(" · %.1f MB/s", mbPerSec)
    } else {
        val kbPerSec = (bytesPerSecond / 1024L).coerceAtLeast(1L)
        " · $kbPerSec KB/s"
    }
}
