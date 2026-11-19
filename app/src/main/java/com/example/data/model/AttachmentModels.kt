package com.example.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class AttachedFile(
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val content: String,
    val lineCount: Int,
    val isTruncated: Boolean = false
)

@Immutable
data class ModelCapabilities(
    val hasSelectedModel: Boolean = false,
    val modelName: String = "",
    val supportsFileView: Boolean = false,
    val fileViewReason: String = "",
    val supportsImageRecognition: Boolean = false,
    val imageRecognitionReason: String = ""
)

fun evaluateModelCapabilities(selectedModelName: String): ModelCapabilities {
    val trimmed = selectedModelName.trim()
    if (trimmed.isEmpty()) {
        return ModelCapabilities(
            hasSelectedModel = false,
            modelName = "",
            supportsFileView = false,
            fileViewReason = "Select or download a model first to view and attach files.",
            supportsImageRecognition = false,
            imageRecognitionReason = "Select or download a model first to check image recognition."
        )
    }

    val isVisionNamed = looksLikeVisionModelName(trimmed)
    return ModelCapabilities(
        hasSelectedModel = true,
        modelName = trimmed,
        supportsFileView = true,
        fileViewReason = "Supported by $trimmed for readable text, code, Markdown, JSON, CSV, and log files.",
        // llamacpp-kotlin 0.4.0 runs text-only GGUF completions without multimodal mmproj image encoding.
        supportsImageRecognition = false,
        imageRecognitionReason = if (isVisionNamed) {
            "On-device runner does not support vision projector (mmproj) image recognition."
        } else {
            "$trimmed is a text-only model and does not support image recognition."
        }
    )
}

private fun looksLikeVisionModelName(modelName: String): Boolean {
    val lower = modelName.lowercase()
    return listOf(
        "llava",
        "vision",
        "-vl",
        "_vl",
        "qwen2-vl",
        "qwen2.5-vl",
        "minicpm-v",
        "internvl",
        "pixtral",
        "paligemma",
        "moondream",
        "smolvlm",
        "mmproj"
    ).any { it in lower }
}

fun formatAttachmentSize(bytes: Long): String {
    return when {
        bytes < 1024L -> "${bytes.coerceAtLeast(0L)} B"
        bytes < 1_048_576L -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
        else -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1_048_576.0)
    }
}
