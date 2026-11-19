package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatEntity
import com.example.data.local.MessageEntity
import com.example.data.model.AttachedFile
import com.example.data.model.EngineStatus
import com.example.data.model.formatAttachmentSize
import com.example.ui.components.AttachFileSheet
import com.example.ui.components.ChatDrawerContent
import com.example.ui.components.FileViewerDialog
import com.example.ui.components.MarkdownMessage
import com.example.ui.components.ThinkingTriangleDots
import com.example.ui.components.XireLogo
import com.example.ui.components.XirePromptDialog
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LocalXireColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.XireBronze
import com.example.ui.theme.XireBronzeLight
import com.example.ui.viewmodel.XireUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    uiState: XireUiState,
    onNewChat: () -> Unit,
    onSelectChat: (Long) -> Unit,
    onRenameChat: (Long, String) -> Unit,
    onDeleteChat: (Long) -> Unit,
    onOpenDownloadedModels: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenSettings: () -> Unit,
    onAttachFileUri: (Uri) -> Unit,
    onClearPendingAttachment: () -> Unit,
    onDismissAttachmentError: () -> Unit,
    onSendMessage: (String) -> Unit,
    onStopGeneration: () -> Unit,
    onRegenerateLast: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var desktopDrawerOpen by remember { mutableStateOf(true) }
    var composerText by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    var chatToRename by remember { mutableStateOf<ChatEntity?>(null) }
    var chatToDelete by remember { mutableStateOf<ChatEntity?>(null) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var filePreview by remember { mutableStateOf<Triple<String, Long?, String>?>(null) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onAttachFileUri(uri)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onAttachFileUri(uri)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 840.dp

        if (isExpandedScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = desktopDrawerOpen) {
                    ChatDrawerContent(
                        chats = uiState.chats,
                        activeChatId = uiState.activeChatId,
                        userProfile = uiState.userProfile,
                        onCloseDrawer = { desktopDrawerOpen = false },
                        onNewChat = onNewChat,
                        onSelectChat = onSelectChat,
                        onRenameChat = { chatToRename = it },
                        onDeleteChat = { chatToDelete = it },
                        onOpenModels = onOpenModels,
                        onOpenSettings = onOpenSettings
                    )
                }
                ChatMainContent(
                    uiState = uiState,
                    composerText = composerText,
                    onComposerTextChange = { composerText = it },
                    onToggleDrawer = { desktopDrawerOpen = !desktopDrawerOpen },
                    onOpenDownloadedModels = onOpenDownloadedModels,
                    onOpenModels = onOpenModels,
                    onOpenAttachSheet = { showAttachSheet = true },
                    onClearPendingAttachment = onClearPendingAttachment,
                    onDismissAttachmentError = onDismissAttachmentError,
                    onViewAttachment = { name, size, content ->
                        filePreview = Triple(name, size, content)
                    },
                    onSendMessage = { prompt ->
                        onSendMessage(prompt)
                        composerText = ""
                    },
                    onStopGeneration = onStopGeneration,
                    onRegenerateLast = onRegenerateLast,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ChatDrawerContent(
                        chats = uiState.chats,
                        activeChatId = uiState.activeChatId,
                        userProfile = uiState.userProfile,
                        onCloseDrawer = { scope.launch { drawerState.close() } },
                        onNewChat = {
                            onNewChat()
                            scope.launch { drawerState.close() }
                        },
                        onSelectChat = { chatId ->
                            onSelectChat(chatId)
                            scope.launch { drawerState.close() }
                        },
                        onRenameChat = { chatToRename = it },
                        onDeleteChat = { chatToDelete = it },
                        onOpenModels = {
                            scope.launch { drawerState.close() }
                            onOpenModels()
                        },
                        onOpenSettings = {
                            scope.launch { drawerState.close() }
                            onOpenSettings()
                        }
                    )
                }
            ) {
                ChatMainContent(
                    uiState = uiState,
                    composerText = composerText,
                    onComposerTextChange = { composerText = it },
                    onToggleDrawer = { scope.launch { drawerState.open() } },
                    onOpenDownloadedModels = onOpenDownloadedModels,
                    onOpenModels = onOpenModels,
                    onOpenAttachSheet = { showAttachSheet = true },
                    onClearPendingAttachment = onClearPendingAttachment,
                    onDismissAttachmentError = onDismissAttachmentError,
                    onViewAttachment = { name, size, content ->
                        filePreview = Triple(name, size, content)
                    },
                    onSendMessage = { prompt ->
                        onSendMessage(prompt)
                        composerText = ""
                    },
                    onStopGeneration = onStopGeneration,
                    onRegenerateLast = onRegenerateLast,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showAttachSheet) {
        AttachFileSheet(
            capabilities = uiState.modelCapabilities,
            onDismiss = { showAttachSheet = false },
            onPickFile = {
                documentPickerLauncher.launch(arrayOf("*/*"))
            },
            onPickImage = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onSelectModel = onOpenDownloadedModels
        )
    }

    filePreview?.let { (name, size, content) ->
        FileViewerDialog(
            fileName = name,
            sizeBytes = size,
            content = content,
            onDismiss = { filePreview = null }
        )
    }

    chatToRename?.let { target ->
        XirePromptDialog(
            title = "Rename chat",
            initialValue = target.title,
            confirmLabel = "Save",
            onDismiss = { chatToRename = null },
            onConfirm = { newTitle ->
                onRenameChat(target.id, newTitle)
                chatToRename = null
            }
        )
    }

    chatToDelete?.let { target ->
        XirePromptDialog(
            title = "Delete this chat?",
            initialValue = null,
            confirmLabel = "Delete",
            isDestructive = true,
            onDismiss = { chatToDelete = null },
            onConfirm = {
                onDeleteChat(target.id)
                chatToDelete = null
            }
        )
    }
}

@Composable
private fun ChatMainContent(
    uiState: XireUiState,
    composerText: String,
    onComposerTextChange: (String) -> Unit,
    onToggleDrawer: () -> Unit,
    onOpenDownloadedModels: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenAttachSheet: () -> Unit,
    onClearPendingAttachment: () -> Unit,
    onDismissAttachmentError: () -> Unit,
    onViewAttachment: (String, Long?, String) -> Unit,
    onSendMessage: (String) -> Unit,
    onStopGeneration: () -> Unit,
    onRegenerateLast: () -> Unit,
    modifier: Modifier = Modifier
) {
    val xireColors = LocalXireColors.current
    val listState = rememberLazyListState()

    val itemCount = uiState.messages.size + if (uiState.isGenerating) 1 else 0
    LaunchedEffect(itemCount) {
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }
    LaunchedEffect(uiState.streamingText) {
        if (uiState.isGenerating && uiState.streamingText.isNotEmpty() && itemCount > 0) {
            listState.scrollToItem(itemCount - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top bar (#top)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onToggleDrawer,
                shape = CircleShape,
                color = xireColors.card,
                border = BorderStroke(1.dp, xireColors.border),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
                    .testTag("menu_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Open sidebar",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (uiState.settings.selectedModelName.isNotBlank()) {
                val statusDotColor = when (uiState.engineStatus) {
                    is EngineStatus.Ready -> XireBronze
                    is EngineStatus.Loading, is EngineStatus.Generating -> XireBronzeLight
                    EngineStatus.Idle -> xireColors.muted.copy(alpha = 0.45f)
                }
                Surface(
                    onClick = onOpenDownloadedModels,
                    shape = RoundedCornerShape(999.dp),
                    color = xireColors.card,
                    border = BorderStroke(1.dp, xireColors.border)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusDotColor)
                        )
                        Text(
                            text = uiState.settings.selectedModelName,
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = xireColors.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 190.dp)
                        )
                    }
                }
            }
        }

        // Messages or Hero empty state (#msgs)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (uiState.messages.isEmpty() && !uiState.isGenerating) {
                HeroEmptyState(
                    userName = uiState.userProfile.name,
                    selectedModelName = uiState.settings.selectedModelName,
                    onOpenModels = onOpenModels,
                    onQuickPrompt = onSendMessage
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    itemsIndexed(
                        items = uiState.messages,
                        key = { _, msg -> msg.id }
                    ) { index, message ->
                        if (message.role == "user") {
                            UserBubble(
                                message = message,
                                onViewAttachment = onViewAttachment
                            )
                        } else {
                            val isLatestAssistant = index == uiState.messages.lastIndex && !uiState.isGenerating
                            AssistantBubble(
                                message = message,
                                showRegenerate = isLatestAssistant,
                                onRegenerate = onRegenerateLast
                            )
                        }
                    }

                    if (uiState.isGenerating) {
                        item(key = "streaming_bubble") {
                            StreamingAssistantBubble(
                                text = uiState.streamingText,
                                statusText = uiState.streamingStatusText
                            )
                        }
                    }
                }
            }
        }

        // Bottom composer (#cw / #comp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = !uiState.attachmentError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = xireColors.card,
                    border = BorderStroke(1.dp, xireColors.danger.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.attachmentError.orEmpty(),
                            fontFamily = InterFontFamily,
                            fontSize = 12.5.sp,
                            color = xireColors.danger,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissAttachmentError,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Dismiss file error",
                                tint = xireColors.muted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            ComposerCard(
                inputText = composerText,
                onInputChange = onComposerTextChange,
                selectedModelName = uiState.settings.selectedModelName,
                hasDownloadedModels = uiState.installedModels.isNotEmpty(),
                hasAttachAvailability = uiState.modelCapabilities.supportsFileView ||
                    uiState.modelCapabilities.supportsImageRecognition,
                pendingAttachment = uiState.pendingAttachment,
                isGenerating = uiState.isGenerating,
                onOpenAttachSheet = onOpenAttachSheet,
                onViewPendingAttachment = {
                    uiState.pendingAttachment?.let { file ->
                        onViewAttachment(file.name, file.sizeBytes, file.content)
                    }
                },
                onClearPendingAttachment = onClearPendingAttachment,
                onOpenDownloadedModels = onOpenDownloadedModels,
                onSend = {
                    val toSend = composerText.trim()
                    if (toSend.isNotEmpty() || uiState.pendingAttachment != null) {
                        onSendMessage(toSend)
                    }
                },
                onStop = onStopGeneration
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Runs fully on your device. No data leaves it.",
                fontFamily = InterFontFamily,
                fontSize = 12.sp,
                color = xireColors.muted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroEmptyState(
    userName: String,
    selectedModelName: String,
    onOpenModels: () -> Unit,
    onQuickPrompt: (String) -> Unit
) {
    val xireColors = LocalXireColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        XireLogo(size = 64.dp, spin = true)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Back at it, ${userName.ifBlank { "friend" }}",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (selectedModelName.isBlank()) {
                "Add an on-device model to start chatting privately."
            } else {
                "Ready on device with $selectedModelName."
            },
            fontFamily = InterFontFamily,
            fontSize = 14.sp,
            color = xireColors.muted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        if (selectedModelName.isBlank()) {
            Surface(
                onClick = onOpenModels,
                shape = RoundedCornerShape(999.dp),
                color = xireColors.card,
                border = BorderStroke(1.dp, xireColors.border)
            ) {
                Text(
                    text = "＋ Add a model",
                    fontFamily = InterFontFamily,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }
        } else {
            val suggestions = remember {
                listOf(
                    "Explain a tough topic simply",
                    "Write clean code with steps",
                    "Help me plan my day"
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.widthIn(max = 520.dp)
            ) {
                suggestions.forEach { prompt ->
                    Surface(
                        onClick = { onQuickPrompt(prompt) },
                        shape = RoundedCornerShape(999.dp),
                        color = xireColors.card,
                        border = BorderStroke(1.dp, xireColors.border)
                    ) {
                        Text(
                            text = prompt,
                            fontFamily = InterFontFamily,
                            fontSize = 12.5.sp,
                            color = xireColors.muted,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun UserBubble(
    message: MessageEntity,
    onViewAttachment: (String, Long?, String) -> Unit
) {
    val xireColors = LocalXireColors.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val text = message.content

    LaunchedEffect(copied) {
        if (copied) {
            delay(1300)
            copied = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        val fileName = message.attachmentName
        val fileContent = message.attachmentContent
        if (!fileName.isNullOrBlank()) {
            Surface(
                onClick = {
                    if (!fileContent.isNullOrBlank()) {
                        onViewAttachment(fileName, message.attachmentSizeBytes, fileContent)
                    }
                },
                enabled = !fileContent.isNullOrBlank(),
                shape = RoundedCornerShape(16.dp),
                color = xireColors.sidebar,
                border = BorderStroke(1.dp, xireColors.border),
                modifier = Modifier
                    .widthIn(max = 580.dp)
                    .padding(bottom = 6.dp)
                    .testTag("message_attachment_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = fileName,
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val sizePart = message.attachmentSizeBytes?.let { "${formatAttachmentSize(it)} · " }.orEmpty()
                        Text(
                            text = "${sizePart}Tap to view",
                            fontFamily = InterFontFamily,
                            fontSize = 11.5.sp,
                            color = xireColors.muted
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = xireColors.card,
            border = BorderStroke(1.dp, xireColors.border),
            modifier = Modifier.widthIn(max = 580.dp)
        ) {
            SelectionContainer {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable {
                    clipboard.setText(AnnotatedString(text))
                    copied = true
                }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("copy_prompt_button")
        ) {
            Icon(
                imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                contentDescription = "Copy prompt",
                tint = if (copied) MaterialTheme.colorScheme.primary else xireColors.muted,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (copied) "Copied ✓" else "Copy",
                fontFamily = InterFontFamily,
                fontSize = 11.5.sp,
                color = if (copied) MaterialTheme.colorScheme.primary else xireColors.muted
            )
        }
    }
}

@Composable
private fun AssistantBubble(
    message: MessageEntity,
    showRegenerate: Boolean,
    onRegenerate: () -> Unit
) {
    val xireColors = LocalXireColors.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1300)
            copied = false
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        XireLogo(
            size = 26.dp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            MarkdownMessage(text = message.content)

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable {
                            clipboard.setText(AnnotatedString(message.content))
                            copied = true
                        }
                        .padding(horizontal = 6.dp, vertical = 5.dp)
                        .testTag("copy_ai_button")
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = "Copy response",
                        tint = if (copied) MaterialTheme.colorScheme.primary else xireColors.muted,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (copied) "Copied ✓" else "Copy",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = if (copied) MaterialTheme.colorScheme.primary else xireColors.muted
                    )
                }

                if (showRegenerate) {
                    IconButton(
                        onClick = onRegenerate,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Write again",
                            tint = xireColors.muted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (message.tokens != null && message.durationMs != null) {
                    val secs = message.durationMs / 1000.0
                    val tokPerSec = if (secs > 0.05 && message.tokens > 0) {
                        String.format(java.util.Locale.US, " · %.1f tok/s", message.tokens / secs)
                    } else {
                        ""
                    }
                    Text(
                        text = "${message.tokens} tokens · ${String.format(java.util.Locale.US, "%.1fs", secs)}$tokPerSec",
                        fontFamily = InterFontFamily,
                        fontSize = 12.5.sp,
                        color = xireColors.muted,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamingAssistantBubble(
    text: String,
    statusText: String
) {
    val xireColors = LocalXireColors.current
    AnimatedContent(
        targetState = text.isBlank(),
        transitionSpec = {
            fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(140))
        },
        label = "thinking_to_stream"
    ) { isThinkingOnly ->
        if (isThinkingOnly) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThinkingTriangleDots(
                    size = 24.dp,
                    statusLabel = statusText.ifBlank { "Thinking" }
                )
                if (statusText.contains("Loading", ignoreCase = true) ||
                    statusText.contains("Stopping", ignoreCase = true)
                ) {
                    Text(
                        text = statusText,
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = xireColors.muted
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                XireLogo(
                    size = 26.dp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MarkdownMessage(text = text, isStreaming = true)
                    ThinkingTriangleDots(
                        size = 18.dp,
                        statusLabel = "Generating response"
                    )
                }
            }
        }
    }
}

@Composable
private fun ComposerCard(
    inputText: String,
    onInputChange: (String) -> Unit,
    selectedModelName: String,
    hasDownloadedModels: Boolean,
    hasAttachAvailability: Boolean,
    pendingAttachment: AttachedFile?,
    isGenerating: Boolean,
    onOpenAttachSheet: () -> Unit,
    onViewPendingAttachment: () -> Unit,
    onClearPendingAttachment: () -> Unit,
    onOpenDownloadedModels: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    val xireColors = LocalXireColors.current

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = xireColors.card,
        border = BorderStroke(1.15.dp, xireColors.border),
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxWidth()
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                spotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                clip = false
            )
    ) {
        Column(
            modifier = Modifier
                .background(xireColors.card)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            AnimatedVisibility(visible = pendingAttachment != null) {
                pendingAttachment?.let { file ->
                    Surface(
                        onClick = onViewPendingAttachment,
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, xireColors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("pending_attachment_chip")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    fontFamily = InterFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val truncNote = if (file.isTruncated) " · Preview" else ""
                                Text(
                                    text = "${formatAttachmentSize(file.sizeBytes)} · ${file.lineCount} lines$truncNote · Tap to view",
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.5.sp,
                                    color = xireColors.muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = onClearPendingAttachment,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("remove_attachment_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Remove attached file",
                                    tint = xireColors.muted,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp, max = 160.dp)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (inputText.isEmpty()) {
                    Text(
                        text = "Message XIRE AI",
                        fontFamily = LoraFontFamily,
                        fontSize = 16.sp,
                        color = xireColors.muted
                    )
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    textStyle = TextStyle(
                        fontFamily = LoraFontFamily,
                        fontSize = 16.sp,
                        lineHeight = 23.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("message_input")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // File selection button in crisp Black & White
                    Surface(
                        onClick = onOpenAttachSheet,
                        enabled = !isGenerating,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, xireColors.border),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(40.dp)
                            .testTag("attach_file_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.AttachFile,
                                contentDescription = "Attach file",
                                tint = if (!isGenerating && hasAttachAvailability) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    xireColors.muted
                                },
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Downloaded-model selection button in crisp Black & White
                    Surface(
                        onClick = onOpenDownloadedModels,
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, xireColors.border),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(min = 40.dp)
                            .testTag("model_selector_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Memory,
                                contentDescription = null,
                                tint = if (hasDownloadedModels && selectedModelName.isNotBlank()) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    xireColors.muted
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasDownloadedModels && selectedModelName.isNotBlank()) {
                                    selectedModelName
                                } else {
                                    "Select model"
                                },
                                fontFamily = InterFontFamily,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = "Open downloaded models",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Send or Stop button in crisp Black & White
                val canSend = inputText.trim().isNotEmpty() || pendingAttachment != null
                Surface(
                    onClick = {
                        if (isGenerating) onStop() else if (canSend) onSend()
                    },
                    enabled = isGenerating || canSend,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(
                        alpha = if (isGenerating || canSend) 1f else 0.30f
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                        .testTag("send_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isGenerating) Icons.Outlined.Stop else Icons.Outlined.ArrowUpward,
                            contentDescription = if (isGenerating) "Stop generation" else "Send message",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}
