package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewSidebar
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatEntity
import com.example.data.model.UserProfile
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LocalXireColors

@Composable
fun ChatDrawerContent(
    chats: List<ChatEntity>,
    activeChatId: Long?,
    userProfile: UserProfile,
    onCloseDrawer: () -> Unit,
    onNewChat: () -> Unit,
    onSelectChat: (Long) -> Unit,
    onRenameChat: (ChatEntity) -> Unit,
    onDeleteChat: (ChatEntity) -> Unit,
    onOpenModels: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val xireColors = LocalXireColors.current

    Column(
        modifier = modifier
            .width(276.dp)
            .fillMaxHeight()
            .background(xireColors.sidebar)
            .systemBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Brand row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 2.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            XireLogo(size = 28.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "XIRE AI",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onCloseDrawer,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("close_sidebar_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ViewSidebar,
                    contentDescription = "Close sidebar",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // New chat button
        DrawerNavRow(
            label = "New chat",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
            },
            selected = false,
            onClick = onNewChat,
            modifier = Modifier.testTag("new_chat_button")
        )

        Text(
            text = "Chats",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = xireColors.muted,
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp)
        )

        // Chat history list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(chats, key = { it.id }) { chat ->
                ChatHistoryItem(
                    chat = chat,
                    selected = chat.id == activeChatId,
                    onSelect = { onSelectChat(chat.id) },
                    onRename = { onRenameChat(chat) },
                    onDelete = { onDeleteChat(chat) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        DrawerNavRow(
            label = "Download models",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Memory,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            selected = false,
            onClick = onOpenModels,
            modifier = Modifier.testTag("drawer_models_button")
        )

        DrawerNavRow(
            label = "Settings",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            selected = false,
            onClick = onOpenSettings,
            modifier = Modifier.testTag("drawer_settings_button")
        )

        Spacer(modifier = Modifier.height(4.dp))

        // User profile footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onOpenSettings)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userProfile.initial,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = userProfile.name.ifBlank { "Local User" },
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Local · private",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = xireColors.muted
                )
            }
        }
    }
}

@Composable
private fun DrawerNavRow(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontSize = 14.5.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ChatHistoryItem(
    chat: ChatEntity,
    selected: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val xireColors = LocalXireColors.current
    var menuExpanded by remember { mutableStateOf(false) }

    val bgColor = if (selected) {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onSelect)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = chat.title,
            fontFamily = InterFontFamily,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreHoriz,
                    contentDescription = "Chat options",
                    tint = xireColors.muted,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier
                    .background(xireColors.card)
                    .border(1.dp, xireColors.border, RoundedCornerShape(16.dp))
            ) {
                DropdownMenuItem(
                    text = { Text("Rename", fontFamily = InterFontFamily) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Delete",
                            fontFamily = InterFontFamily,
                            color = xireColors.danger
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = xireColors.danger,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}
