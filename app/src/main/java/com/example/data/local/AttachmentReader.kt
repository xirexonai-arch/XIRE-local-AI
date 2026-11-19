package com.example.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.AttachedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class AttachmentReader(private val context: Context) {

    suspend fun readTextAttachment(uri: Uri): Result<AttachedFile> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri).orEmpty().lowercase()
            val (displayName, reportedSize) = queryMeta(uri)
            val ext = displayName.substringAfterLast('.', "").lowercase()

            if (mimeType.startsWith("image/") || ext in IMAGE_EXTENSIONS) {
                throw IllegalArgumentException(
                    "Image recognition is not supported by the active model."
                )
            }

            if (ext in BINARY_EXTENSIONS || mimeType == "application/pdf") {
                throw IllegalArgumentException(
                    "Binary or PDF files cannot be viewed by this text model. Try .txt, .md, .csv, .json, .xml, .log, or source code files."
                )
            }

            val rawBytes = resolver.openInputStream(uri)?.use { stream ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (total < MAX_READ_BYTES) {
                    val toRead = minOf(buffer.size, MAX_READ_BYTES - total)
                    val read = stream.read(buffer, 0, toRead)
                    if (read <= 0) break
                    out.write(buffer, 0, read)
                    total += read
                }
                out.toByteArray()
            } ?: throw IllegalStateException("Could not open the selected file.")

            if (rawBytes.isEmpty()) {
                throw IllegalArgumentException("The selected file is empty.")
            }

            if (!looksLikeReadableText(rawBytes)) {
                throw IllegalArgumentException(
                    "This file appears to be binary. Only readable text, code, Markdown, CSV, JSON, or log files can be viewed."
                )
            }

            val decoded = rawBytes.toString(Charsets.UTF_8)
                .replace("\r\n", "\n")
                .replace('\r', '\n')

            val clipped = decoded.length > MAX_PREVIEW_CHARS || rawBytes.size >= MAX_READ_BYTES ||
                (reportedSize > MAX_READ_BYTES)
            val content = if (decoded.length > MAX_PREVIEW_CHARS) {
                decoded.substring(0, MAX_PREVIEW_CHARS)
            } else {
                decoded
            }

            val finalSize = if (reportedSize > 0L) reportedSize else rawBytes.size.toLong()
            val lines = content.count { it == '\n' } + 1

            AttachedFile(
                name = displayName,
                mimeType = mimeType.ifBlank { "text/plain" },
                sizeBytes = finalSize,
                content = content,
                lineCount = lines,
                isTruncated = clipped
            )
        }
    }

    private fun queryMeta(uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "attachment.txt"
        var size = -1L
        runCatching {
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
        }
        if (size < 0L && uri.scheme == "file") {
            uri.path?.let { path ->
                runCatching { size = java.io.File(path).length() }
            }
        }
        return name to size
    }

    companion object {
        private const val MAX_READ_BYTES = 128 * 1024
        private const val MAX_PREVIEW_CHARS = 12_000

        private val IMAGE_EXTENSIONS = setOf(
            "png", "jpg", "jpeg", "webp", "gif", "bmp", "heic", "heif", "avif", "ico"
        )

        private val BINARY_EXTENSIONS = setOf(
            "pdf", "zip", "rar", "7z", "tar", "gz", "apk", "aab", "exe", "dll",
            "so", "jar", "dex", "class", "gguf", "bin", "mp3", "wav", "ogg",
            "m4a", "mp4", "mkv", "mov", "avi", "webm", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
        )

        internal fun looksLikeReadableText(bytes: ByteArray): Boolean {
            val sampleLen = minOf(bytes.size, 4096)
            var controlCount = 0
            for (i in 0 until sampleLen) {
                val b = bytes[i].toInt() and 0xFF
                if (b == 0) return false
                if (b < 0x20 && b != '\n'.code && b != '\r'.code && b != '\t'.code) {
                    controlCount++
                }
            }
            return controlCount * 20 <= sampleLen
        }
    }
}
