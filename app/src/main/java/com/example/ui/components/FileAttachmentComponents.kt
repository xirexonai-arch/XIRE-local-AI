package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ModelCapabilities
import com.example.data.model.formatAttachmentSize
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalXireColors
import com.example.ui.theme.XireBronze
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachFileSheet(
    capabilities: ModelCapabilities,
    onDismiss: () -> Unit,
    onPickFile: () -> Unit,
    onPickImage: () -> Unit,
    onSelectModel: () -> Unit
) {
    val xireColors = LocalXireColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add file to chat",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (capabilities.hasSelectedModel) {
                            "Active model: ${capabilities.modelName}"
                        } else {
                            "No AI model selected"
                        },
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = xireColors.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close attach sheet",
                        tint = xireColors.muted
                    )
                }
            }

            CapabilityAttachmentRow(
                icon = Icons.Outlined.Description,
                title = "File view",
                description = capabilities.fileViewReason,
                isAvailable = capabilities.supportsFileView,
                badgeLabel = if (capabilities.supportsFileView) "Available" else "Unavailable",
                testTag = "attach_document_option",
                onClick = {
                    onDismiss()
                    onPickFile()
                }
            )

            CapabilityAttachmentRow(
                icon = Icons.Outlined.Image,
                title = "Image recognition",
                description = capabilities.imageRecognitionReason,
                isAvailable = capabilities.supportsImageRecognition,
                badgeLabel = if (capabilities.supportsImageRecognition) "Available" else "Unavailable",
                testTag = "attach_image_option",
                onClick = {
                    if (capabilities.supportsImageRecognition) {
                        onDismiss()
                        onPickImage()
                    }
                }
            )

            if (!capabilities.hasSelectedModel) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onSelectModel()
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("attach_sheet_select_model_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Memory,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select or download a model",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CapabilityAttachmentRow(
    icon: ImageVector,
    title: String,
    description: String,
    isAvailable: Boolean,
    badgeLabel: String,
    testTag: String,
    onClick: () -> Unit
) {
    val xireColors = LocalXireColors.current
    Surface(
        onClick = onClick,
        enabled = isAvailable,
        shape = RoundedCornerShape(20.dp),
        color = xireColors.card,
        border = BorderStroke(
            width = if (isAvailable) 1.25.dp else 1.dp,
            color = if (isAvailable) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f) else xireColors.border
        ),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isAvailable) 1f else 0.62f)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAvailable) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        } else {
                            xireColors.sidebar
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isAvailable) MaterialTheme.colorScheme.onSurface else xireColors.muted,
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontFamily = InterFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isAvailable) {
                            XireBronze.copy(alpha = 0.18f)
                        } else {
                            xireColors.sidebar
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isAvailable) XireBronze.copy(alpha = 0.45f) else xireColors.border
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            if (!isAvailable) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = xireColors.muted,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Text(
                                text = badgeLabel,
                                fontFamily = InterFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isAvailable) MaterialTheme.colorScheme.onSurface else xireColors.muted
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontFamily = InterFontFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = xireColors.muted
                )
            }
        }
    }
}

@Composable
fun FileViewerDialog(
    fileName: String,
    sizeBytes: Long?,
    content: String,
    onDismiss: () -> Unit
) {
    val xireColors = LocalXireColors.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val lineCount = remember(content) { content.count { it == '\n' } + 1 }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1300)
            copied = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, xireColors.border),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("file_viewer_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fileName,
                            fontFamily = InterFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val sizePart = sizeBytes?.let { "${formatAttachmentSize(it)} · " }.orEmpty()
                        Text(
                            text = "${sizePart}$lineCount lines",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = xireColors.muted
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = xireColors.card,
                        border = BorderStroke(1.dp, xireColors.border),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                clipboard.setText(AnnotatedString(content))
                                copied = true
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                                contentDescription = "Copy file text",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (copied) "Copied" else "Copy",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close file viewer",
                            tint = xireColors.muted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = xireColors.border, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = xireColors.sidebar,
                    border = BorderStroke(1.dp, xireColors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 380.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = content,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.5.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}
