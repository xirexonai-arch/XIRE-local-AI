package com.example.data.model

import androidx.compose.runtime.Immutable

enum class ThemeMode(val key: String, val label: String) {
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    SYSTEM("system", "System");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

@Immutable
data class UserProfile(
    val name: String = "",
    val age: Int = 0
) {
    val isOnboarded: Boolean get() = name.isNotBlank() && age in 1..120
    val initial: String get() = name.trim().firstOrNull()?.uppercase() ?: "X"
}

@Immutable
data class AppSettings(
    val maxTokens: Int = 512,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val selectedModelName: String = ""
)

@Immutable
sealed interface EngineStatus {
    data object Idle : EngineStatus
    data class Loading(val modelName: String) : EngineStatus
    data class Ready(val modelName: String) : EngineStatus
    data class Generating(val modelName: String) : EngineStatus
}

@Immutable
data class DownloadProgress(
    val modelId: String,
    val filename: String,
    val bytesRead: Long,
    val totalBytes: Long,
    val bytesPerSecond: Long = 0L
) {
    val fraction: Float
        get() = if (totalBytes > 0L) (bytesRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val percent: Int
        get() = (fraction * 100f).toInt().coerceIn(0, 100)
}

@Immutable
data class StarterModelSpec(
    val id: String,
    val displayName: String,
    val filename: String,
    val sizeLabel: String,
    val estimatedBytes: Long,
    val summary: String,
    val downloadUrl: String
)

object StarterModels {
    val recommended: List<StarterModelSpec> = listOf(
        StarterModelSpec(
            id = "smollm2-360m",
            displayName = "SmolLM2 360M Instruct",
            filename = "SmolLM2-360M-Instruct-Q4_K_M.gguf",
            sizeLabel = "~271 MB",
            estimatedBytes = 285_000_000L,
            summary = "Ultra-lightweight and fast on any Android device",
            downloadUrl = "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q4_K_M.gguf"
        ),
        StarterModelSpec(
            id = "qwen2.5-0.5b",
            displayName = "Qwen2.5 0.5B Instruct",
            filename = "Qwen2.5-0.5B-Instruct-Q4_K_M.gguf",
            sizeLabel = "~491 MB",
            estimatedBytes = 515_000_000L,
            summary = "Compact general & coding assistant",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
        ),
        StarterModelSpec(
            id = "llama-3.2-1b",
            displayName = "Llama 3.2 1B Instruct",
            filename = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            sizeLabel = "~808 MB",
            estimatedBytes = 850_000_000L,
            summary = "Strong everyday reasoning & writing",
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf"
        ),
        StarterModelSpec(
            id = "qwen2.5-1.5b",
            displayName = "Qwen2.5 1.5B Instruct",
            filename = "Qwen2.5-1.5B-Instruct-Q4_K_M.gguf",
            sizeLabel = "~1.12 GB",
            estimatedBytes = 1_200_000_000L,
            summary = "Higher quality code and detailed answers",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
        )
    )
}
