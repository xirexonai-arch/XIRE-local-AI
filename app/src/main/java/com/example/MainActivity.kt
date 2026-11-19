package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.ModelsDialog
import com.example.ui.components.OnboardingOverlay
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DownloadedModelsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.XireTheme
import com.example.ui.viewmodel.XireViewModel

private const val ROUTE_CHAT = "chat"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_DOWNLOADED_MODELS = "downloaded_models"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: XireViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            var scrollToGuide by remember { mutableStateOf(false) }

            val modelFilePicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    viewModel.importModelUri(uri)
                }
            }

            XireTheme(themeMode = uiState.settings.themeMode) {
                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_CHAT
                    ) {
                        composable(ROUTE_CHAT) {
                            ChatScreen(
                                uiState = uiState,
                                onNewChat = viewModel::createNewChat,
                                onSelectChat = viewModel::selectChat,
                                onRenameChat = viewModel::renameChat,
                                onDeleteChat = viewModel::deleteChat,
                                onOpenDownloadedModels = {
                                    navController.navigate(ROUTE_DOWNLOADED_MODELS) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenModels = viewModel::openModelsDialog,
                                onOpenSettings = {
                                    scrollToGuide = false
                                    navController.navigate(ROUTE_SETTINGS) {
                                        launchSingleTop = true
                                    }
                                },
                                onAttachFileUri = viewModel::attachFileUri,
                                onClearPendingAttachment = viewModel::clearPendingAttachment,
                                onDismissAttachmentError = viewModel::dismissAttachmentError,
                                onSendMessage = viewModel::sendMessage,
                                onStopGeneration = viewModel::stopGeneration,
                                onRegenerateLast = viewModel::regenerateLastResponse
                            )
                        }

                        composable(ROUTE_DOWNLOADED_MODELS) {
                            DownloadedModelsScreen(
                                installedModels = uiState.installedModels,
                                selectedModelName = uiState.settings.selectedModelName,
                                engineStatus = uiState.engineStatus,
                                downloadProgress = uiState.downloadProgress,
                                onBack = { navController.popBackStack() },
                                onSelectModel = { modelName ->
                                    viewModel.selectModel(modelName)
                                    navController.popBackStack()
                                },
                                onRemoveModel = viewModel::removeModel,
                                onOpenDownloadModels = viewModel::openModelsDialog
                            )
                        }

                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                uiState = uiState,
                                scrollToInstallGuide = scrollToGuide,
                                onBack = { navController.popBackStack() },
                                onSaveProfile = viewModel::saveSettings,
                                onSelectTheme = viewModel::setThemeMode,
                                onManageModels = {
                                    navController.navigate(ROUTE_DOWNLOADED_MODELS) {
                                        launchSingleTop = true
                                    }
                                },
                                onDeleteAllChats = viewModel::deleteAllChats
                            )
                        }
                    }

                    if (uiState.showModelsDialog) {
                        ModelsDialog(
                            installedModels = uiState.installedModels,
                            selectedModelName = uiState.settings.selectedModelName,
                            importProgress = uiState.importProgress,
                            downloadProgress = uiState.downloadProgress,
                            partialDownloadBytes = uiState.partialDownloadBytes,
                            notice = uiState.modelNotice,
                            onChooseFile = {
                                modelFilePicker.launch(arrayOf("*/*"))
                            },
                            onSelectModel = viewModel::selectModel,
                            onRemoveModel = viewModel::removeModel,
                            onDownloadStarter = viewModel::downloadStarterModel,
                            onCancelDownload = viewModel::cancelDownload,
                            onOpenInstallGuide = {
                                scrollToGuide = true
                                navController.navigate(ROUTE_SETTINGS) {
                                    launchSingleTop = true
                                }
                            },
                            onDismiss = viewModel::dismissModelsDialog
                        )
                    }

                    AnimatedVisibility(
                        visible = uiState.isPrefsLoaded && !uiState.userProfile.isOnboarded,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        OnboardingOverlay(onComplete = viewModel::completeOnboarding)
                    }
                }
            }
        }
    }
}
