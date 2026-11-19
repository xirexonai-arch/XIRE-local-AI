package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AttachmentReader
import com.example.data.local.ChatEntity
import com.example.data.local.LocalModelEntity
import com.example.data.local.MessageEntity
import com.example.data.local.PreferencesRepository
import com.example.data.local.XireDatabase
import com.example.data.model.AppSettings
import com.example.data.model.AttachedFile
import com.example.data.model.DownloadProgress
import com.example.data.model.EngineStatus
import com.example.data.model.ModelCapabilities
import com.example.data.model.StarterModelSpec
import com.example.data.model.ThemeMode
import com.example.data.model.UserProfile
import com.example.data.model.evaluateModelCapabilities
import com.example.data.repository.XireRepository
import com.example.engine.LocalLlmEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class XireUiState(
    val isPrefsLoaded: Boolean = false,
    val userProfile: UserProfile = UserProfile(),
    val settings: AppSettings = AppSettings(),
    val modelCapabilities: ModelCapabilities = evaluateModelCapabilities(""),
    val chats: List<ChatEntity> = emptyList(),
    val activeChatId: Long? = null,
    val messages: List<MessageEntity> = emptyList(),
    val installedModels: List<LocalModelEntity> = emptyList(),
    val engineStatus: EngineStatus = EngineStatus.Idle,
    val isGenerating: Boolean = false,
    val streamingText: String = "",
    val streamingStatusText: String = "",
    val pendingAttachment: AttachedFile? = null,
    val attachmentError: String? = null,
    val importProgress: Float? = null,
    val downloadProgress: DownloadProgress? = null,
    val partialDownloadBytes: Map<String, Long> = emptyMap(),
    val modelNotice: String? = null,
    val showModelsDialog: Boolean = false,
    val settingsSavedFeedback: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class XireViewModel(application: Application) : AndroidViewModel(application) {

    private val database = XireDatabase.getInstance(application)
    private val prefs = PreferencesRepository(application)
    private val attachmentReader = AttachmentReader(application)
    private val repository = XireRepository(
        context = application,
        chatDao = database.chatDao(),
        messageDao = database.messageDao(),
        localModelDao = database.localModelDao()
    )
    private val engine = LocalLlmEngine(application)

    private val activeChatIdFlow = MutableStateFlow<Long?>(null)
    private val engineStatusFlow = MutableStateFlow<EngineStatus>(EngineStatus.Idle)
    private val isGeneratingFlow = MutableStateFlow(false)
    private val streamingTextFlow = MutableStateFlow("")
    private val streamingStatusFlow = MutableStateFlow("")
    private val pendingAttachmentFlow = MutableStateFlow<AttachedFile?>(null)
    private val attachmentErrorFlow = MutableStateFlow<String?>(null)
    private val importProgressFlow = MutableStateFlow<Float?>(null)
    private val downloadProgressFlow = MutableStateFlow<DownloadProgress?>(null)
    private val partialDownloadsFlow = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val modelNoticeFlow = MutableStateFlow<String?>(null)
    private val showModelsDialogFlow = MutableStateFlow(false)
    private val settingsSavedFlow = MutableStateFlow(false)

    private val messagesFlow = activeChatIdFlow.flatMapLatest { chatId ->
        if (chatId == null) flowOf(emptyList()) else repository.observeMessages(chatId)
    }

    private var generationJob: Job? = null
    private var downloadJob: Job? = null
    private var preloadJob: Job? = null

    init {

        viewModelScope.launch {
            combine(
                prefs.appSettings,
                repository.localModels
            ) { settings, models ->
                val targetName = when {
                    models.any { it.name == settings.selectedModelName } -> settings.selectedModelName
                    models.isNotEmpty() -> models.first().name
                    else -> ""
                }
                models.firstOrNull { it.name == targetName }
            }.collect { model ->
                if (model != null && !isGeneratingFlow.value && !engine.isModelLoaded(model.filePath)) {
                    schedulePreload(model.name, model.filePath)
                }
            }
        }
    }

    private val baseDataFlow = combine(
        prefs.userProfile,
        prefs.appSettings,
        repository.chats,
        activeChatIdFlow,
        messagesFlow
    ) { profile, settings, chats, activeId, messages ->
        BaseData(profile, settings, chats, activeId, messages)
    }

    private val modelDataFlow = combine(
        repository.localModels,
        engineStatusFlow,
        isGeneratingFlow,
        streamingTextFlow,
        streamingStatusFlow
    ) { models, status, generating, streamText, streamStatus ->
        ModelRuntimeData(models, status, generating, streamText, streamStatus)
    }

    private val transferProgressFlow = combine(
        importProgressFlow,
        downloadProgressFlow,
        partialDownloadsFlow
    ) { importProg, downloadProg, partialBytes ->
        Triple(importProg, downloadProg, partialBytes)
    }

    private val attachmentDataFlow = combine(
        pendingAttachmentFlow,
        attachmentErrorFlow
    ) { pending, err ->
        pending to err
    }

    private val transferDataFlow = combine(
        transferProgressFlow,
        modelNoticeFlow,
        showModelsDialogFlow,
        settingsSavedFlow,
        attachmentDataFlow
    ) { progressTriple, notice, showModels, saved, attachmentPair ->
        TransferUiData(
            importProg = progressTriple.first,
            downloadProg = progressTriple.second,
            partialBytes = progressTriple.third,
            notice = notice,
            showModels = showModels,
            saved = saved,
            pendingAttachment = attachmentPair.first,
            attachmentError = attachmentPair.second
        )
    }

    val uiState: StateFlow<XireUiState> = combine(
        baseDataFlow,
        modelDataFlow,
        transferDataFlow
    ) { base, runtime, transfer ->
        val validModelName = when {
            runtime.models.any { it.name == base.settings.selectedModelName } -> base.settings.selectedModelName
            runtime.models.isNotEmpty() -> runtime.models.first().name
            else -> ""
        }
        XireUiState(
            isPrefsLoaded = true,
            userProfile = base.profile,
            settings = base.settings.copy(selectedModelName = validModelName),
            modelCapabilities = evaluateModelCapabilities(validModelName),
            chats = base.chats,
            activeChatId = base.activeId,
            messages = base.messages,
            installedModels = runtime.models,
            engineStatus = runtime.status,
            isGenerating = runtime.generating,
            streamingText = runtime.streamText,
            streamingStatusText = runtime.streamStatus,
            pendingAttachment = transfer.pendingAttachment,
            attachmentError = transfer.attachmentError,
            importProgress = transfer.importProg,
            downloadProgress = transfer.downloadProg,
            partialDownloadBytes = transfer.partialBytes,
            modelNotice = transfer.notice,
            showModelsDialog = transfer.showModels,
            settingsSavedFeedback = transfer.saved
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = XireUiState()
    )

    fun completeOnboarding(name: String, age: Int) {
        viewModelScope.launch {
            prefs.saveProfile(name, age)
        }
    }

    fun saveSettings(name: String, age: Int, maxTokens: Int) {
        viewModelScope.launch {
            prefs.saveSettings(name, age, maxTokens)
            settingsSavedFlow.value = true
            kotlinx.coroutines.delay(1400)
            settingsSavedFlow.value = false
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefs.setThemeMode(mode)
        }
    }

    fun createNewChat() {
        if (isGeneratingFlow.value) return
        activeChatIdFlow.value = null
        streamingTextFlow.value = ""
    }

    fun selectChat(chatId: Long) {
        if (isGeneratingFlow.value) return
        activeChatIdFlow.value = chatId
        streamingTextFlow.value = ""
    }

    fun renameChat(chatId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.renameChat(chatId, newTitle)
        }
    }

    fun deleteChat(chatId: Long) {
        viewModelScope.launch {
            if (activeChatIdFlow.value == chatId) {
                if (isGeneratingFlow.value) {
                    engine.stopGeneration()
                    generationJob?.cancel()
                }
                activeChatIdFlow.value = null
            }
            repository.deleteChat(chatId)
        }
    }

    fun deleteAllChats() {
        viewModelScope.launch {
            if (isGeneratingFlow.value) {
                engine.stopGeneration()
                generationJob?.cancel()
            }
            activeChatIdFlow.value = null
            repository.deleteAllChats()
        }
    }

    fun openModelsDialog() {
        modelNoticeFlow.value = null
        showModelsDialogFlow.value = true
        viewModelScope.launch {
            partialDownloadsFlow.value = repository.getPartialDownloadSizes()
        }
    }

    fun dismissModelsDialog() {
        showModelsDialogFlow.value = false
    }

    fun selectModel(modelName: String) {
        viewModelScope.launch {
            prefs.setSelectedModelName(modelName)
            val entity = repository.getModelByName(modelName) ?: return@launch
            if (engine.isModelLoaded(entity.filePath)) {
                engineStatusFlow.value = EngineStatus.Ready(modelName)
            } else {
                schedulePreload(modelName, entity.filePath)
            }
        }
    }

    private fun schedulePreload(modelName: String, filePath: String) {
        preloadJob?.cancel()
        preloadJob = viewModelScope.launch {
            if (isGeneratingFlow.value) return@launch
            engineStatusFlow.value = EngineStatus.Loading(modelName)
            val loaded = engine.ensureModelLoaded(filePath)
            if (!isGeneratingFlow.value) {
                engineStatusFlow.value = if (loaded) {
                    EngineStatus.Ready(modelName)
                } else {
                    EngineStatus.Idle
                }
            }
        }
    }

    fun importModelUri(uri: Uri) {
        viewModelScope.launch {
            modelNoticeFlow.value = null
            importProgressFlow.value = 0.02f
            val result = repository.importModelFromUri(uri) { progress ->
                importProgressFlow.value = progress
            }
            importProgressFlow.value = null
            result.onSuccess { model ->
                prefs.setSelectedModelName(model.name)
                modelNoticeFlow.value = "${model.name} saved to Downloaded Models."
            }.onFailure {
                modelNoticeFlow.value = "Please choose a valid .gguf model file."
            }
        }
    }

    fun downloadStarterModel(spec: StarterModelSpec) {
        if (downloadJob?.isActive == true) return
        modelNoticeFlow.value = null
        downloadJob = viewModelScope.launch {
            val initialBytes = partialDownloadsFlow.value[spec.filename] ?: 0L
            downloadProgressFlow.value = DownloadProgress(
                modelId = spec.id,
                filename = spec.filename,
                bytesRead = initialBytes,
                totalBytes = spec.estimatedBytes
            )
            val result = repository.downloadStarterModel(spec) { progress ->
                downloadProgressFlow.value = progress
            }
            downloadProgressFlow.value = null
            partialDownloadsFlow.value = repository.getPartialDownloadSizes()
            result.onSuccess { model ->
                prefs.setSelectedModelName(model.name)
                modelNoticeFlow.value = "${spec.displayName} saved to Downloaded Models."
            }.onFailure { err ->
                if (err !is kotlinx.coroutines.CancellationException) {
                    modelNoticeFlow.value = err.message?.takeIf { it.isNotBlank() }
                        ?: "Download paused. Tap Resume to continue."
                }
            }
        }
    }

    fun cancelDownload() {
        val activeFilename = downloadProgressFlow.value?.filename
        downloadJob?.cancel()
        downloadJob = null
        downloadProgressFlow.value = null
        if (!activeFilename.isNullOrBlank()) {
            viewModelScope.launch {
                repository.clearPartialDownload(activeFilename)
                partialDownloadsFlow.value = repository.getPartialDownloadSizes()
            }
        }
    }

    fun removeModel(name: String) {
        viewModelScope.launch {
            val currentSelected = uiState.value.settings.selectedModelName
            if (currentSelected == name) {
                preloadJob?.cancel()
                if (isGeneratingFlow.value) {
                    engine.stopGeneration()
                    generationJob?.cancel()
                }
                engine.unloadModel()
                engineStatusFlow.value = EngineStatus.Idle
            }
            repository.removeModel(name)
            val remaining = uiState.value.installedModels.filter { it.name != name }
            prefs.setSelectedModelName(remaining.firstOrNull()?.name.orEmpty())
        }
    }

    fun attachFileUri(uri: Uri) {
        val currentState = uiState.value
        val capabilities = currentState.modelCapabilities
        if (!capabilities.hasSelectedModel) {
            attachmentErrorFlow.value = "Select or download an AI model first to attach files."
            return
        }
        if (!capabilities.supportsFileView) {
            attachmentErrorFlow.value = capabilities.fileViewReason
            return
        }
        viewModelScope.launch {
            attachmentErrorFlow.value = null
            attachmentReader.readTextAttachment(uri)
                .onSuccess { attached ->
                    pendingAttachmentFlow.value = attached
                }
                .onFailure { err ->
                    attachmentErrorFlow.value = err.message ?: "Unable to read the selected file."
                }
        }
    }

    fun clearPendingAttachment() {
        pendingAttachmentFlow.value = null
    }

    fun dismissAttachmentError() {
        attachmentErrorFlow.value = null
    }

    fun sendMessage(rawText: String) {
        val currentState = uiState.value
        val attachment = currentState.pendingAttachment
        val text = rawText.trim()
        if ((text.isEmpty() && attachment == null) || isGeneratingFlow.value || generationJob?.isActive == true) {
            return
        }

        val modelName = currentState.settings.selectedModelName
        if (modelName.isBlank()) {
            openModelsDialog()
            return
        }

        val effectivePrompt = text.ifEmpty {
            "Please inspect and explain the attached file."
        }
        val chatTitleSeed = text.ifEmpty {
            attachment?.name?.let { "File: $it" } ?: "New chat"
        }

        pendingAttachmentFlow.value = null
        attachmentErrorFlow.value = null
        isGeneratingFlow.value = true
        streamingTextFlow.value = ""
        streamingStatusFlow.value = "Preparing…"
        engine.resetStopFlag()

        generationJob = viewModelScope.launch {
            try {
                val chatId = currentState.activeChatId ?: repository.createChat(chatTitleSeed).also {
                    activeChatIdFlow.value = it
                }
                repository.addMessage(
                    chatId = chatId,
                    role = "user",
                    content = effectivePrompt,
                    attachmentName = attachment?.name,
                    attachmentSizeBytes = attachment?.sizeBytes,
                    attachmentContent = attachment?.content
                )
                val liveUserName = prefs.userProfile.first().name.trim()
                    .ifEmpty { currentState.userProfile.name.trim() }
                runInference(
                    chatId = chatId,
                    modelName = modelName,
                    userName = liveUserName,
                    maxTokens = currentState.settings.maxTokens
                )
            } finally {
                streamingTextFlow.value = ""
                streamingStatusFlow.value = ""
                isGeneratingFlow.value = false
            }
        }
    }

    fun regenerateLastResponse() {
        if (isGeneratingFlow.value || generationJob?.isActive == true) return
        val currentState = uiState.value
        val chatId = currentState.activeChatId ?: return
        val modelName = currentState.settings.selectedModelName
        if (modelName.isBlank()) {
            openModelsDialog()
            return
        }

        isGeneratingFlow.value = true
        streamingTextFlow.value = ""
        streamingStatusFlow.value = "Preparing…"
        engine.resetStopFlag()

        generationJob = viewModelScope.launch {
            try {
                val existing = repository.getMessagesOnce(chatId)
                val last = existing.lastOrNull() ?: return@launch
                if (last.role == "assistant") {
                    repository.deleteMessage(last.id)
                }
                val liveUserName = prefs.userProfile.first().name.trim()
                    .ifEmpty { currentState.userProfile.name.trim() }
                runInference(
                    chatId = chatId,
                    modelName = modelName,
                    userName = liveUserName,
                    maxTokens = currentState.settings.maxTokens
                )
            } finally {
                streamingTextFlow.value = ""
                streamingStatusFlow.value = ""
                isGeneratingFlow.value = false
            }
        }
    }

    fun stopGeneration() {
        viewModelScope.launch {
            streamingStatusFlow.value = "Stopping…"
            engine.stopGeneration()
        }
    }

    private suspend fun runInference(
        chatId: Long,
        modelName: String,
        userName: String,
        maxTokens: Int
    ) {
        val modelEntity = repository.getModelByName(modelName)
        if (modelEntity == null) {
            engineStatusFlow.value = EngineStatus.Idle
            openModelsDialog()
            return
        }

        val alreadyLoaded = engine.isModelLoaded(modelEntity.filePath)
        streamingStatusFlow.value = if (alreadyLoaded) "Thinking…" else "Loading model into memory…"
        engineStatusFlow.value = if (alreadyLoaded) {
            EngineStatus.Generating(modelName)
        } else {
            EngineStatus.Loading(modelName)
        }

        val history = repository.getMessagesOnce(chatId)
        if (!alreadyLoaded) {
            val loaded = engine.ensureModelLoaded(modelEntity.filePath)
            if (!loaded) {
                repository.addMessage(
                    chatId = chatId,
                    role = "assistant",
                    content = "Unable to open $modelName on this device. Try a smaller quantized GGUF model (such as Q4_K_M 0.3B–1.5B)."
                )
                engineStatusFlow.value = EngineStatus.Idle
                return
            }
        }
        engineStatusFlow.value = EngineStatus.Generating(modelName)
        streamingStatusFlow.value = "Thinking…"

        val result = engine.generateReply(
            modelPath = modelEntity.filePath,
            modelName = modelEntity.name,
            userName = userName,
            history = history,
            maxTokens = maxTokens,
            onPartialText = { partial ->
                streamingTextFlow.value = partial
            }
        )

        if (!(result.isStopped && result.text.isBlank())) {
            repository.addMessage(
                chatId = chatId,
                role = "assistant",
                content = result.text,
                tokens = result.tokenCount,
                durationMs = result.durationMs
            )
        }
        engineStatusFlow.value = if (engine.isModelLoaded(modelEntity.filePath)) {
            EngineStatus.Ready(modelName)
        } else {
            EngineStatus.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.releaseImmediate()
    }

    private data class BaseData(
        val profile: UserProfile,
        val settings: AppSettings,
        val chats: List<ChatEntity>,
        val activeId: Long?,
        val messages: List<MessageEntity>
    )

    private data class ModelRuntimeData(
        val models: List<LocalModelEntity>,
        val status: EngineStatus,
        val generating: Boolean,
        val streamText: String,
        val streamStatus: String
    )

    private data class TransferUiData(
        val importProg: Float?,
        val downloadProg: DownloadProgress?,
        val partialBytes: Map<String, Long>,
        val notice: String?,
        val showModels: Boolean,
        val saved: Boolean,
        val pendingAttachment: AttachedFile?,
        val attachmentError: String?
    )
}
