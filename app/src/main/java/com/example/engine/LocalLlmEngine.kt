package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.util.Log
import com.example.data.local.MessageEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.nehuatl.llamacpp.LlamaContext
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

data class GenerationResult(
    val text: String,
    val tokenCount: Int,
    val durationMs: Long,
    val isStopped: Boolean = false
)

/**
 * Thread-safe local GGUF inference engine powered by llama.cpp (llamacpp-kotlin).
 * Optimized for low time-to-first-token (TTFT) and high token/s throughput on Android devices:
 * compact KV-cache sizing, single-batch prefill, fast history budgeting, boosted inference
 * thread priority, and throttled incremental streaming.
 */
class LocalLlmEngine(private val context: Context) {

    private val engineExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread({
            runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE) }
            runnable.run()
        }, "xire-local-llm").apply {
            isDaemon = true
            priority = Thread.MAX_PRIORITY
        }
    }
    private val engineDispatcher = engineExecutor.asCoroutineDispatcher()
    private val mutex = Mutex()
    private val contextIdSeq = AtomicInteger(1)

    @Volatile
    private var activeContext: LlamaContext? = null

    @Volatile
    private var activeContextSize: Int = 1024

    @Volatile
    private var activeThreadCount: Int = 4

    @Volatile
    private var loadedModelPath: String? = null

    @Volatile
    private var loadedModelDetails: Map<String, Any> = emptyMap()

    @Volatile
    private var activeTokenSink: ((String) -> Unit)? = null

    @Volatile
    private var stopRequested: Boolean = false

    fun isModelLoaded(modelPath: String): Boolean {
        return activeContext != null && loadedModelPath == modelPath
    }

    fun resetStopFlag() {
        stopRequested = false
    }

    suspend fun ensureModelLoaded(modelPath: String): Boolean = mutex.withLock {
        withContext(engineDispatcher) {
            loadModelInternal(modelPath)
        }
    }

    suspend fun generateReply(
        modelPath: String,
        modelName: String,
        userName: String,
        history: List<MessageEntity>,
        maxTokens: Int,
        onPartialText: (String) -> Unit
    ): GenerationResult = mutex.withLock {
        withContext(engineDispatcher) {
            if (stopRequested) {
                return@withContext GenerationResult(
                    text = "Generation stopped.",
                    tokenCount = 0,
                    durationMs = 0L
                )
            }

            val loaded = loadModelInternal(modelPath)
            val llamaCtx = activeContext
            if (!loaded || llamaCtx == null) {
                return@withContext GenerationResult(
                    text = "Unable to load this model on this device. Try a smaller quantized GGUF (such as Q4_K_M 0.3B–1.5B).",
                    tokenCount = 0,
                    durationMs = 0L
                )
            }

            val trimmedUserName = userName.trim()
            val systemPrompt = if (trimmedUserName.isNotEmpty()) {
                "You are XIRE AI, a fast on-device assistant for $trimmedUserName. Give clear, direct, accurate answers with fenced markdown code blocks when sharing code."
            } else {
                "You are XIRE AI, a fast on-device assistant. Give clear, direct, accurate answers with fenced markdown code blocks when sharing code."
            }

            val trimmedMessages = trimHistoryToFitContext(
                systemPrompt = systemPrompt,
                history = history.takeLast(4),
                maxContextTokens = activeContextSize
            )

            val primaryPrompt = buildFormattedPrompt(
                ctx = llamaCtx,
                modelName = modelName,
                systemPrompt = systemPrompt,
                messages = trimmedMessages
            )

            val startTime = System.currentTimeMillis()
            var attempt = runSingleCompletion(
                ctx = llamaCtx,
                prompt = primaryPrompt,
                maxTokens = maxTokens,
                onPartialText = onPartialText
            )

            if (attempt.text.isBlank() && !stopRequested) {
                val fallbackPrompt = fallbackTemplate(
                    modelName = modelName,
                    modelDetails = loadedModelDetails,
                    systemPrompt = systemPrompt,
                    messages = trimmedMessages
                )
                if (fallbackPrompt != primaryPrompt) {
                    attempt = runSingleCompletion(
                        ctx = llamaCtx,
                        prompt = fallbackPrompt,
                        maxTokens = maxTokens,
                        onPartialText = onPartialText
                    )
                }
            }

            if (attempt.text.isBlank() && !stopRequested) {
                val simplePrompt = fallbackTemplate(
                    modelName = modelName,
                    modelDetails = loadedModelDetails,
                    systemPrompt = systemPrompt,
                    messages = history.takeLast(1)
                )
                attempt = runSingleCompletion(
                    ctx = llamaCtx,
                    prompt = simplePrompt,
                    maxTokens = maxTokens,
                    onPartialText = onPartialText
                )
            }

            val durationMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
            val stoppedWithNoText = stopRequested && attempt.text.isBlank()
            val finalText = when {
                attempt.text.isNotBlank() -> attempt.text
                stoppedWithNoText -> ""
                attempt.errorMessage != null ->
                    "Could not generate a response with this model (${attempt.errorMessage}). Try a shorter prompt or a Q4_K_M instruct model."
                else ->
                    "The model finished without producing text. Try rephrasing or selecting another GGUF instruct model."
            }

            GenerationResult(
                text = finalText,
                tokenCount = if (attempt.text.isNotBlank()) {
                    attempt.tokenCount.coerceAtLeast(estimateTokens(finalText))
                } else {
                    0
                },
                durationMs = durationMs,
                isStopped = stoppedWithNoText
            )
        }
    }

    private fun runSingleCompletion(
        ctx: LlamaContext,
        prompt: String,
        maxTokens: Int,
        onPartialText: (String) -> Unit
    ): CompletionAttempt {
        val buffer = StringBuilder(512)
        var streamedTokens = 0
        var lastEmitMs = 0L
        val promptTokenCount = if (prompt.length > 900) {
            runCatching { ctx.tokenize(prompt).size }.getOrDefault(prompt.length / 3)
        } else {
            (prompt.length / 3).coerceAtLeast(16)
        }
        val remainingBudget = (activeContextSize - promptTokenCount - 16).coerceAtLeast(128)
        val targetPredict = maxTokens.coerceIn(64, 4096).coerceAtMost(remainingBudget)

        activeTokenSink = { chunk ->
            if (!stopRequested && chunk.isNotEmpty()) {
                buffer.append(chunk)
                streamedTokens++
                val now = SystemClock.uptimeMillis()
                if (streamedTokens <= 2 || now - lastEmitMs >= 70L) {
                    lastEmitMs = now
                    onPartialText(cleanModelOutput(buffer.toString(), isStreaming = true))
                }
            }
        }

        return try {
            val completionParams = mapOf<String, Any>(
                "prompt" to prompt,
                "n_threads" to activeThreadCount,
                "n_predict" to targetPredict,
                "temperature" to 0.5,
                "top_p" to 0.88,
                "top_k" to 32,
                "min_p" to 0.08,
                "penalty_last_n" to 64,
                "penalty_repeat" to 1.1,
                "emit_partial_completion" to true,
                "ignore_eos" to false,
                "stop" to STOP_TOKENS
            )

            val result = ctx.completion(completionParams)
            val mapText = (result["text"] as? String)
                ?: (result["content"] as? String)
                ?: ""
            val raw = if (buffer.isNotEmpty()) buffer.toString() else mapText
            val cleaned = cleanModelOutput(raw, isStreaming = false)
            val reportedTokens = (result["tokens_predicted"] as? Number)?.toInt()
                ?: streamedTokens

            CompletionAttempt(
                text = cleaned,
                tokenCount = reportedTokens,
                errorMessage = null
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.e(TAG, "Completion error", t)
            val partial = cleanModelOutput(buffer.toString(), isStreaming = false)
            CompletionAttempt(
                text = partial,
                tokenCount = streamedTokens,
                errorMessage = t.message?.take(120)
            )
        } finally {
            activeTokenSink = null
        }
    }

    suspend fun stopGeneration() {
        stopRequested = true
        runCatching {
            activeContext?.stopCompletion()
        }
    }

    suspend fun unloadModel() = mutex.withLock {
        withContext(engineDispatcher) {
            releaseInternal()
        }
    }

    fun releaseImmediate() {
        stopRequested = true
        runCatching { activeContext?.stopCompletion() }
        engineExecutor.execute {
            releaseInternal()
        }
        engineExecutor.shutdown()
    }

    private fun loadModelInternal(modelPath: String): Boolean {
        if (activeContext != null && loadedModelPath == modelPath) {
            return true
        }
        releaseInternal()

        val file = File(modelPath)
        if (!file.exists() || !file.canRead() || file.length() < 64L || !isValidGgufHeader(file)) {
            Log.e(TAG, "Invalid or missing GGUF file: $modelPath")
            return false
        }

        val modelUri = Uri.fromFile(file).toString()
        val (baseCtx, baseBatch, baseThreads) = computeHardwareBudget(file.length())

        val attempts = buildList {
            // Prefer memory-mapped loading first: 5-10x faster startup and lower RAM spikes.
            add(LoadConfig(nCtx = baseCtx, nBatch = baseBatch, threads = baseThreads, useMmap = true))
            if (baseCtx > 1024) {
                add(LoadConfig(nCtx = 1024, nBatch = 512, threads = baseThreads, useMmap = true))
            }
            add(LoadConfig(nCtx = 1024, nBatch = 256, threads = baseThreads, useMmap = false))
        }

        for (cfg in attempts) {
            var pfd: ParcelFileDescriptor? = null
            var detachedFd = -1
            try {
                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                detachedFd = pfd.detachFd()

                val engineParams = mapOf<String, Any>(
                    "model" to modelUri,
                    "model_fd" to detachedFd,
                    "n_ctx" to cfg.nCtx,
                    "n_batch" to cfg.nBatch,
                    "n_threads" to cfg.threads,
                    "n_gpu_layers" to 0,
                    "use_mmap" to cfg.useMmap,
                    "use_mlock" to false,
                    "embedding" to false,
                    "vocab_only" to false,
                    "lora" to "",
                    "lora_scaled" to 1.0,
                    "rope_freq_base" to 0.0,
                    "rope_freq_scale" to 0.0
                )

                val ctx = LlamaContext(contextIdSeq.getAndIncrement(), engineParams)
                if (ctx.context == 0L) {
                    runCatching { ctx.release() }
                    continue
                }
                ctx.setTokenCallback { tokenChunk ->
                    activeTokenSink?.invoke(tokenChunk)
                }

                activeContext = ctx
                activeContextSize = cfg.nCtx
                activeThreadCount = cfg.threads
                loadedModelPath = modelPath
                loadedModelDetails = ctx.modelDetails
                return true
            } catch (t: Throwable) {
                Log.w(TAG, "Model load attempt failed (${cfg.nCtx}, mmap=${cfg.useMmap}): ${t.message}")
                if (detachedFd >= 0) {
                    runCatching { ParcelFileDescriptor.adoptFd(detachedFd).close() }
                } else {
                    runCatching { pfd?.close() }
                }
            }
        }
        return false
    }

    private fun releaseInternal() {
        val ctx = activeContext
        activeContext = null
        loadedModelPath = null
        loadedModelDetails = emptyMap()
        activeTokenSink = null
        if (ctx != null) {
            runCatching { ctx.stopCompletion() }
            runCatching { ctx.release() }
        }
    }

    private fun trimHistoryToFitContext(
        systemPrompt: String,
        history: List<MessageEntity>,
        maxContextTokens: Int
    ): List<MessageEntity> {
        if (history.size <= 1) return history
        // Reserve generous room for fast, complete responses without bloated prefill overhead.
        val reservedForReply = (maxContextTokens / 2).coerceAtLeast(384)
        val maxPromptChars = ((maxContextTokens - reservedForReply).coerceAtLeast(256)) * 3

        var startIdx = 0
        while (startIdx < history.lastIndex) {
            if (history[startIdx].role == "user") {
                val slice = history.subList(startIdx, history.size)
                val approxChars = systemPrompt.length + slice.sumOf {
                    it.content.length + (it.attachmentContent?.length?.coerceAtMost(1400) ?: 0) + 32
                }
                if (approxChars <= maxPromptChars) {
                    return slice
                }
            }
            startIdx++
        }
        return history.takeLast(1)
    }

    private fun formatMessageContentForPrompt(
        msg: MessageEntity,
        isLatestMessage: Boolean
    ): String {
        val fileName = msg.attachmentName
        val fileBody = msg.attachmentContent
        if (fileName.isNullOrBlank() || fileBody.isNullOrBlank()) {
            return msg.content
        }
        val charCap = if (isLatestMessage) {
            ((activeContextSize - 384).coerceAtLeast(384) * 2).coerceAtMost(2400)
        } else {
            420
        }
        val snippet = if (fileBody.length > charCap) {
            fileBody.take(charCap) + "\n… [truncated to fit model context]"
        } else {
            fileBody
        }
        val promptBody = msg.content.trim()
        return buildString {
            append("[Attached file: ").append(fileName).append("]\n")
            append("```\n").append(snippet).append("\n```")
            if (promptBody.isNotEmpty()) {
                append("\n\n").append(promptBody)
            }
        }
    }

    private fun buildFormattedPrompt(
        ctx: LlamaContext,
        modelName: String,
        systemPrompt: String,
        messages: List<MessageEntity>
    ): String {
        val chatWithSystem = buildList {
            add(mapOf("role" to "system", "content" to systemPrompt))
            messages.forEachIndexed { index, msg ->
                val role = if (msg.role == "user") "user" else "assistant"
                val content = formatMessageContentForPrompt(msg, isLatestMessage = index == messages.lastIndex)
                add(mapOf("role" to role, "content" to content))
            }
        }

        val nativeWithSystem = runCatching {
            ctx.getFormattedChat(chatWithSystem, "")
        }.getOrNull()

        if (!nativeWithSystem.isNullOrBlank()) {
            return ensureGenerationPromptSuffix(nativeWithSystem, modelName, loadedModelDetails)
        }

        // Some templates (e.g. Gemma) reject the "system" role: merge into first user message.
        val chatMergedSystem = buildList {
            var systemInserted = false
            messages.forEachIndexed { index, msg ->
                val role = if (msg.role == "user") "user" else "assistant"
                val formatted = formatMessageContentForPrompt(msg, isLatestMessage = index == messages.lastIndex)
                val content = if (!systemInserted && role == "user") {
                    systemInserted = true
                    "$systemPrompt\n\n$formatted"
                } else {
                    formatted
                }
                add(mapOf("role" to role, "content" to content))
            }
        }

        val nativeMerged = runCatching {
            ctx.getFormattedChat(chatMergedSystem, "")
        }.getOrNull()

        if (!nativeMerged.isNullOrBlank()) {
            return ensureGenerationPromptSuffix(nativeMerged, modelName, loadedModelDetails)
        }

        return fallbackTemplate(modelName, loadedModelDetails, systemPrompt, messages)
    }

    private fun ensureGenerationPromptSuffix(
        formatted: String,
        modelName: String,
        modelDetails: Map<String, Any>
    ): String {
        val trimmed = formatted.trimEnd()
        // If native template already placed an assistant turn header without trailing newline, normalize it.
        when {
            trimmed.endsWith("<|im_start|>assistant") -> return "$trimmed\n"
            trimmed.endsWith("<|start_header_id|>assistant<|end_header_id|>") -> return "$trimmed\n\n"
            trimmed.endsWith("<start_of_turn>model") -> return "$trimmed\n"
            trimmed.endsWith("<|assistant|>") -> return "$trimmed\n"
        }

        val endsWithTurnCloser = listOf(
            "<|im_end|>",
            "<|eot_id|>",
            "<end_of_turn>",
            "<|end|>",
            "</s>",
            "<|endoftext|>",
            "<turn|>"
        ).any { trimmed.endsWith(it) }

        if (!endsWithTurnCloser) {
            return formatted
        }

        return when {
            "<|im_start|>" in formatted || trimmed.endsWith("<|im_end|>") ->
                "$formatted<|im_start|>assistant\n"
            "<|start_header_id|>" in formatted || trimmed.endsWith("<|eot_id|>") ->
                "$formatted<|start_header_id|>assistant<|end_header_id|>\n\n"
            "<start_of_turn>" in formatted || trimmed.endsWith("<end_of_turn>") ->
                "$formatted<start_of_turn>model\n"
            "<|user|>" in formatted || trimmed.endsWith("<|end|>") ->
                "$formatted<|assistant|>\n"
            isLlama3Model(modelName, modelDetails) ->
                "$formatted<|start_header_id|>assistant<|end_header_id|>\n\n"
            isGemmaModel(modelName, modelDetails) ->
                "$formatted<start_of_turn>model\n"
            isPhiModel(modelName, modelDetails) ->
                "$formatted<|assistant|>\n"
            else ->
                "$formatted<|im_start|>assistant\n"
        }
    }

    private fun fallbackTemplate(
        modelName: String,
        modelDetails: Map<String, Any>,
        systemPrompt: String,
        messages: List<MessageEntity>
    ): String {
        return when {
            isLlama3Model(modelName, modelDetails) -> buildString {
                append("<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n")
                append(systemPrompt).append("<|eot_id|>")
                messages.forEachIndexed { index, m ->
                    val role = if (m.role == "user") "user" else "assistant"
                    val formatted = formatMessageContentForPrompt(m, isLatestMessage = index == messages.lastIndex)
                    append("<|start_header_id|>").append(role).append("<|end_header_id|>\n\n")
                    append(formatted).append("<|eot_id|>")
                }
                append("<|start_header_id|>assistant<|end_header_id|>\n\n")
            }

            isGemmaModel(modelName, modelDetails) -> buildString {
                var systemInserted = false
                messages.forEachIndexed { index, m ->
                    val role = if (m.role == "user") "user" else "model"
                    val formatted = formatMessageContentForPrompt(m, isLatestMessage = index == messages.lastIndex)
                    append("<start_of_turn>").append(role).append("\n")
                    if (!systemInserted && role == "user") {
                        systemInserted = true
                        append(systemPrompt).append("\n\n")
                    }
                    append(formatted).append("<end_of_turn>\n")
                }
                append("<start_of_turn>model\n")
            }

            isPhiModel(modelName, modelDetails) -> buildString {
                append("<|system|>\n").append(systemPrompt).append("<|end|>\n")
                messages.forEachIndexed { index, m ->
                    val tag = if (m.role == "user") "<|user|>" else "<|assistant|>"
                    val formatted = formatMessageContentForPrompt(m, isLatestMessage = index == messages.lastIndex)
                    append(tag).append("\n").append(formatted).append("<|end|>\n")
                }
                append("<|assistant|>\n")
            }

            else -> buildString {
                // Standard ChatML (Qwen2.5, SmolLM2, TinyLlama-Chat, Mistral/DeepSeek Instruct variants)
                append("<|im_start|>system\n").append(systemPrompt).append("<|im_end|>\n")
                messages.forEachIndexed { index, m ->
                    val role = if (m.role == "user") "user" else "assistant"
                    val formatted = formatMessageContentForPrompt(m, isLatestMessage = index == messages.lastIndex)
                    append("<|im_start|>").append(role).append("\n")
                    append(formatted).append("<|im_end|>\n")
                }
                append("<|im_start|>assistant\n")
            }
        }
    }

    private fun isLlama3Model(modelName: String, details: Map<String, Any>): Boolean {
        val hint = "${modelName.lowercase()} ${details.values.joinToString(" ").lowercase()}"
        return "llama-3" in hint || "llama3" in hint || "<|eot_id|>" in hint
    }

    private fun isGemmaModel(modelName: String, details: Map<String, Any>): Boolean {
        val hint = "${modelName.lowercase()} ${details.values.joinToString(" ").lowercase()}"
        return "gemma" in hint || "<start_of_turn>" in hint
    }

    private fun isPhiModel(modelName: String, details: Map<String, Any>): Boolean {
        val hint = "${modelName.lowercase()} ${details.values.joinToString(" ").lowercase()}"
        return "phi-" in hint || "phi3" in hint || "phi4" in hint || "<|assistant|>" in hint
    }

    private fun computeHardwareBudget(modelSizeBytes: Long): Triple<Int, Int, Int> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        val totalRam = memInfo.totalMem
        val availRam = memInfo.availMem

        val nCtx = when {
            totalRam >= 5_500_000_000L && availRam >= 1_200_000_000L && modelSizeBytes < 2_500_000_000L -> 1536
            else -> 1024
        }
        // 512 batch allows single-pass prompt prefill on mobile CPUs.
        val nBatch = 512
        val cores = Runtime.getRuntime().availableProcessors()
        val threads = when {
            cores >= 8 -> 6
            cores >= 4 -> 4
            else -> cores.coerceIn(2, 4)
        }
        return Triple(nCtx, nBatch, threads)
    }

    private fun isValidGgufHeader(file: File): Boolean {
        return runCatching {
            RandomAccessFile(file, "r").use { raf ->
                if (raf.length() < 4L) return@use false
                val magic = ByteArray(4)
                raf.readFully(magic)
                magic[0] == 'G'.code.toByte() &&
                    magic[1] == 'G'.code.toByte() &&
                    magic[2] == 'U'.code.toByte() &&
                    magic[3] == 'F'.code.toByte()
            }
        }.getOrDefault(false)
    }

    private fun cleanModelOutput(raw: String, isStreaming: Boolean): String {
        if (raw.isEmpty()) return ""
        var result = raw
        if ('<' in result) {
            for (token in STOP_TOKENS) {
                if (token in result) {
                    result = result.replace(token, "")
                }
            }
            // Handle reasoning <think>...</think> tags if emitted by a reasoning GGUF model
            val thinkEndIdx = result.indexOf("</think>")
            if (thinkEndIdx >= 0) {
                val afterThink = result.substring(thinkEndIdx + "</think>".length).trimStart()
                if (afterThink.isNotBlank()) {
                    result = afterThink
                } else {
                    result = result.replace("<think>", "").replace("</think>", "")
                }
            } else if (isStreaming && result.trimStart().startsWith("<think>")) {
                val body = result.trimStart().removePrefix("<think>").trim()
                if (body.isEmpty()) return ""
                result = body
            }
            if (isStreaming) {
                // Hide partial special-token prefixes like "<|" or "</s" at the tail during streaming
                val lastLt = result.lastIndexOf('<')
                if (lastLt >= 0 && result.length - lastLt <= 16) {
                    val tail = result.substring(lastLt)
                    if (tail.startsWith("<|") || tail == "<" || tail.startsWith("</")) {
                        result = result.substring(0, lastLt)
                    }
                }
            }
        }
        return result.trimStart('\n').let { if (isStreaming) it else it.trimEnd() }
    }

    private fun estimateTokens(text: String): Int {
        return (text.length / 4).coerceAtLeast(1)
    }

    private data class LoadConfig(
        val nCtx: Int,
        val nBatch: Int,
        val threads: Int,
        val useMmap: Boolean
    )

    private data class CompletionAttempt(
        val text: String,
        val tokenCount: Int,
        val errorMessage: String?
    )

    private companion object {
        const val TAG = "XireLocalLlm"
        val STOP_TOKENS = listOf(
            "</s>",
            "<|im_end|>",
            "<|im_start|>",
            "<|eot_id|>",
            "<|eot|>",
            "<|end_of_text|>",
            "<end_of_turn>",
            "<|end|>",
            "<|endoftext|>",
            "<|return|>",
            "<turn|>",
            "<|end_of_sentence|>",
            "<｜end▁of▁sentence｜>"
        )
    }
}
