package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import com.example.data.local.ChatDao
import com.example.data.local.ChatEntity
import com.example.data.local.LocalModelDao
import com.example.data.local.LocalModelEntity
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import com.example.data.model.DownloadProgress
import com.example.data.model.StarterModelSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

class XireRepository(
    private val context: Context,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val localModelDao: LocalModelDao
) {
    private val modelsDir: File by lazy {
        File(context.filesDir, "models").apply { mkdirs() }
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectionPool(ConnectionPool(4, 2, TimeUnit.MINUTES))
            .pingInterval(20, TimeUnit.SECONDS)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val chats: Flow<List<ChatEntity>> = chatDao.observeChats()
    val localModels: Flow<List<LocalModelEntity>> = localModelDao.observeModels()

    fun observeMessages(chatId: Long): Flow<List<MessageEntity>> = messageDao.observeMessages(chatId)

    suspend fun getMessagesOnce(chatId: Long): List<MessageEntity> = messageDao.getMessagesOnce(chatId)

    suspend fun createChat(firstPrompt: String): Long {
        val title = firstPrompt.trim().replace("\n", " ").take(36).ifBlank { "New chat" }
        return chatDao.insertChat(ChatEntity(title = title))
    }

    suspend fun renameChat(chatId: Long, newTitle: String) {
        val trimmed = newTitle.trim().take(60)
        if (trimmed.isNotEmpty()) {
            chatDao.updateTitle(chatId, trimmed)
        }
    }

    suspend fun deleteChat(chatId: Long) {
        chatDao.deleteChatById(chatId)
    }

    suspend fun deleteAllChats() {
        chatDao.deleteAllChats()
    }

    suspend fun addMessage(
        chatId: Long,
        role: String,
        content: String,
        tokens: Int? = null,
        durationMs: Long? = null,
        attachmentName: String? = null,
        attachmentSizeBytes: Long? = null,
        attachmentContent: String? = null
    ): Long {
        chatDao.touchChat(chatId)
        return messageDao.insertMessage(
            MessageEntity(
                chatId = chatId,
                role = role,
                content = content,
                tokens = tokens,
                durationMs = durationMs,
                attachmentName = attachmentName,
                attachmentSizeBytes = attachmentSizeBytes,
                attachmentContent = attachmentContent
            )
        )
    }

    suspend fun deleteMessage(messageId: Long) {
        messageDao.deleteMessageById(messageId)
    }

    suspend fun getModelByName(name: String): LocalModelEntity? = localModelDao.getModelByName(name)

    suspend fun getPartialDownloadSizes(): Map<String, Long> = withContext(Dispatchers.IO) {
        modelsDir.listFiles { file -> file.isFile && file.name.endsWith(".part") }
            ?.mapNotNull { partFile ->
                val baseName = partFile.name.removeSuffix(".part")
                val size = partFile.length()
                if (size > 4L && hasValidGgufMagic(partFile)) {
                    baseName to size
                } else {
                    partFile.delete()
                    null
                }
            }
            ?.toMap()
            ?: emptyMap()
    }

    suspend fun clearPartialDownload(filename: String) = withContext(Dispatchers.IO) {
        if (filename.isNotBlank()) {
            File(modelsDir, "$filename.part").delete()
        }
    }

    suspend fun importModelFromUri(
        uri: Uri,
        onProgress: (Float) -> Unit
    ): Result<LocalModelEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val (displayName, reportedSize) = queryFileMeta(uri)
            val safeName = sanitizeFilename(displayName)

            if (reportedSize > 0L) {
                ensureStorageAvailable(reportedSize)
            }

            resolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(4)
                val readHeader = readFully(stream, header)
                if (!isGgufMagic(header, readHeader)) {
                    throw IllegalArgumentException("Selected file is not a valid GGUF model.")
                }

                val targetFile = File(modelsDir, safeName)
                val tempFile = File(modelsDir, "$safeName.part")
                if (tempFile.exists()) tempFile.delete()

                BufferedOutputStream(FileOutputStream(tempFile), IO_WRITE_BUFFER_BYTES).use { out ->
                    out.write(header, 0, readHeader)
                    val buffer = ByteArray(IO_READ_BUFFER_BYTES)
                    var copied = readHeader.toLong()
                    var lastReport = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = stream.read(buffer)
                        if (count <= 0) break
                        out.write(buffer, 0, count)
                        copied += count
                        if (reportedSize > 0L) {
                            val now = System.currentTimeMillis()
                            if (now - lastReport >= 80L || copied >= reportedSize) {
                                lastReport = now
                                onProgress((copied.toFloat() / reportedSize.toFloat()).coerceIn(0f, 0.98f))
                            }
                        }
                    }
                    out.flush()
                }

                promoteTempFile(tempFile, targetFile)
                onProgress(1f)

                val entity = LocalModelEntity(
                    name = safeName,
                    filePath = targetFile.absolutePath,
                    sizeBytes = targetFile.length()
                )
                localModelDao.upsertModel(entity)
                entity
            } ?: throw IllegalStateException("Could not open the selected file.")
        }
    }

    suspend fun downloadStarterModel(
        spec: StarterModelSpec,
        onProgress: (DownloadProgress) -> Unit
    ): Result<LocalModelEntity> = withContext(Dispatchers.IO) {
        val targetFile = File(modelsDir, spec.filename)
        val tempFile = File(modelsDir, "${spec.filename}.part")

        try {
            if (!hasActiveNetwork()) {
                throw IllegalStateException("No internet connection. Connect to Wi-Fi or mobile data and try again.")
            }

            val validExisting = if (tempFile.exists() && tempFile.length() > 4L && hasValidGgufMagic(tempFile)) {
                tempFile.length()
            } else {
                if (tempFile.exists()) tempFile.delete()
                0L
            }

            val estimatedRemaining = (spec.estimatedBytes - validExisting).coerceAtLeast(32L * 1024L * 1024L)
            ensureStorageAvailable(estimatedRemaining)

            var lastError: Throwable? = null
            val maxAttempts = 3

            for (attempt in 1..maxAttempts) {
                currentCoroutineContext().ensureActive()

                val existingBytes = if (tempFile.exists() && tempFile.length() > 4L && hasValidGgufMagic(tempFile)) {
                    tempFile.length()
                } else {
                    if (tempFile.exists()) tempFile.delete()
                    0L
                }

                val requestBuilder = Request.Builder()
                    .url(spec.downloadUrl)
                    .header("User-Agent", "XIRE-AI-Android/1.0")
                    .header("Accept-Encoding", "identity")

                if (existingBytes > 0L) {
                    requestBuilder.header("Range", "bytes=$existingBytes-")
                }

                val call = httpClient.newCall(requestBuilder.build())
                val job = currentCoroutineContext()[kotlinx.coroutines.Job]
                val handle = job?.invokeOnCompletion { call.cancel() }

                try {
                    call.execute().use { response ->
                        if (response.code == 416) {
                            // Partial file size mismatched remote range; restart cleanly.
                            tempFile.delete()
                            lastError = IOException("Server requested a fresh download.")
                            continue
                        }

                        if (!response.isSuccessful) {
                            val httpErr = IllegalStateException("Download failed (HTTP ${response.code}).")
                            if (response.code in 500..599 || response.code == 429) {
                                lastError = httpErr
                                continue
                            } else {
                                throw httpErr
                            }
                        }

                        val body = response.body ?: throw IOException("Empty download response.")
                        val isPartial = response.code == 206 && existingBytes > 0L
                        val bodyLength = body.contentLength()
                        val totalBytes = if (isPartial) {
                            parseContentRangeTotal(response.header("Content-Range"))
                                ?: if (bodyLength > 0L) existingBytes + bodyLength else spec.estimatedBytes
                        } else {
                            if (bodyLength > 0L) bodyLength else spec.estimatedBytes
                        }

                        val remainingNeeded = (totalBytes - if (isPartial) existingBytes else 0L)
                        if (remainingNeeded > 0L) {
                            ensureStorageAvailable(remainingNeeded)
                        }

                        val input: InputStream = body.byteStream()
                        input.use { stream ->
                            var bytesRead = if (isPartial) existingBytes else 0L
                            var initialHeader: ByteArray? = null
                            var initialHeaderCount = 0

                            if (!isPartial) {
                                if (tempFile.exists()) tempFile.delete()
                                val header = ByteArray(4)
                                initialHeaderCount = readFully(stream, header)
                                if (!isGgufMagic(header, initialHeaderCount)) {
                                    throw IllegalStateException("Downloaded file is not a valid GGUF model.")
                                }
                                initialHeader = header
                                bytesRead = initialHeaderCount.toLong()
                            }

                            BufferedOutputStream(
                                FileOutputStream(tempFile, isPartial),
                                IO_WRITE_BUFFER_BYTES
                            ).use { out ->
                                if (initialHeader != null && initialHeaderCount > 0) {
                                    out.write(initialHeader, 0, initialHeaderCount)
                                }

                                val buffer = ByteArray(IO_READ_BUFFER_BYTES)
                                var lastReportTime = System.currentTimeMillis()
                                var windowStartTime = lastReportTime
                                var windowBytes = 0L
                                var smoothedBps = 0L

                                onProgress(
                                    DownloadProgress(
                                        modelId = spec.id,
                                        filename = spec.filename,
                                        bytesRead = bytesRead,
                                        totalBytes = totalBytes,
                                        bytesPerSecond = 0L
                                    )
                                )

                                while (true) {
                                    currentCoroutineContext().ensureActive()
                                    val count = stream.read(buffer)
                                    if (count <= 0) break
                                    out.write(buffer, 0, count)
                                    bytesRead += count
                                    windowBytes += count

                                    val now = System.currentTimeMillis()
                                    val sinceReport = now - lastReportTime
                                    if (sinceReport >= 150L || (totalBytes > 0L && bytesRead >= totalBytes)) {
                                        val windowElapsed = (now - windowStartTime).coerceAtLeast(1L)
                                        if (windowElapsed >= 250L) {
                                            val instantBps = (windowBytes * 1000L) / windowElapsed
                                            smoothedBps = if (smoothedBps == 0L) {
                                                instantBps
                                            } else {
                                                (smoothedBps * 3 + instantBps) / 4
                                            }
                                            windowStartTime = now
                                            windowBytes = 0L
                                        }
                                        lastReportTime = now
                                        onProgress(
                                            DownloadProgress(
                                                modelId = spec.id,
                                                filename = spec.filename,
                                                bytesRead = bytesRead,
                                                totalBytes = totalBytes,
                                                bytesPerSecond = smoothedBps
                                            )
                                        )
                                    }
                                }
                                out.flush()
                            }
                        }

                        if (totalBytes > 0L && tempFile.length() < totalBytes) {
                            throw IOException("Connection closed at ${tempFile.length() / 1_048_576} MB.")
                        }
                    }

                    if (!hasValidGgufMagic(tempFile)) {
                        tempFile.delete()
                        throw IllegalStateException("Downloaded file is not a valid GGUF model.")
                    }

                    promoteTempFile(tempFile, targetFile)

                    val entity = LocalModelEntity(
                        name = spec.filename,
                        filePath = targetFile.absolutePath,
                        sizeBytes = targetFile.length()
                    )
                    localModelDao.upsertModel(entity)
                    return@withContext Result.success(entity)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (ioe: IOException) {
                    lastError = ioe
                    if (attempt < maxAttempts && hasActiveNetwork()) {
                        delay(1200L * attempt)
                    } else if (!hasActiveNetwork()) {
                        throw IllegalStateException("Network connection lost. Tap Resume when back online.")
                    }
                } finally {
                    handle?.dispose()
                }
            }

            Result.failure(lastError ?: IOException("Download paused before completion."))
        } catch (ce: CancellationException) {
            tempFile.delete()
            throw ce
        } catch (t: Throwable) {
            // Keep partial .part file on transient/network errors so the user can resume without losing data.
            if (tempFile.exists() && !hasValidGgufMagic(tempFile)) {
                tempFile.delete()
            }
            Result.failure(t)
        }
    }

    suspend fun removeModel(name: String) = withContext(Dispatchers.IO) {
        val existing = localModelDao.getModelByName(name)
        if (existing != null) {
            File(existing.filePath).delete()
            File(modelsDir, "${existing.name}.part").delete()
        }
        localModelDao.deleteModelByName(name)
    }

    private fun hasActiveNetwork(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun ensureStorageAvailable(requiredBytes: Long) {
        val availableBytes = runCatching {
            StatFs(modelsDir.absolutePath).availableBytes
        }.getOrDefault(Long.MAX_VALUE)

        val headroomBytes = 32L * 1024L * 1024L
        if (availableBytes < requiredBytes + headroomBytes) {
            val needMb = ((requiredBytes + headroomBytes) / 1_048_576L).coerceAtLeast(1L)
            val freeMb = (availableBytes / 1_048_576L).coerceAtLeast(0L)
            throw IllegalStateException("Not enough free storage (${freeMb} MB free, need ~${needMb} MB).")
        }
    }

    private fun promoteTempFile(tempFile: File, targetFile: File) {
        if (targetFile.exists()) targetFile.delete()
        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }
    }

    private fun parseContentRangeTotal(header: String?): Long? {
        if (header.isNullOrBlank()) return null
        val totalPart = header.substringAfter('/', "").trim()
        return totalPart.toLongOrNull()?.takeIf { it > 0L }
    }

    private fun hasValidGgufMagic(file: File): Boolean {
        return runCatching {
            if (!file.exists() || file.length() < 4L) return@runCatching false
            RandomAccessFile(file, "r").use { raf ->
                val magic = ByteArray(4)
                raf.readFully(magic)
                isGgufMagic(magic, 4)
            }
        }.getOrDefault(false)
    }

    private fun isGgufMagic(bytes: ByteArray, count: Int): Boolean {
        return count == 4 &&
            bytes[0] == 'G'.code.toByte() &&
            bytes[1] == 'G'.code.toByte() &&
            bytes[2] == 'U'.code.toByte() &&
            bytes[3] == 'F'.code.toByte()
    }

    private fun queryFileMeta(uri: Uri): Pair<String, Long> {
        var name = "model_${System.currentTimeMillis()}.gguf"
        var size = -1L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) {
                    val raw = cursor.getString(nameIdx)
                    if (!raw.isNullOrBlank()) name = raw
                }
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                    size = cursor.getLong(sizeIdx)
                }
            }
        }
        return name to size
    }

    private fun sanitizeFilename(raw: String): String {
        val base = raw.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        return if (base.endsWith(".gguf", ignoreCase = true) || base.endsWith(".bin", ignoreCase = true)) {
            base
        } else {
            "$base.gguf"
        }
    }

    private fun readFully(stream: InputStream, dest: ByteArray): Int {
        var offset = 0
        while (offset < dest.size) {
            val read = stream.read(dest, offset, dest.size - offset)
            if (read <= 0) break
            offset += read
        }
        return offset
    }

    private companion object {
        const val IO_READ_BUFFER_BYTES = 256 * 1024
        const val IO_WRITE_BUFFER_BYTES = 512 * 1024
    }
}
