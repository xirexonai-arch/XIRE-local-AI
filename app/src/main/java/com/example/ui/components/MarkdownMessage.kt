package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalXireColors
import com.example.ui.theme.XireBronze
import kotlinx.coroutines.delay

private val CODE_LANG_REGEX = Regex("^[\\w+#.-]*$")

private sealed interface MarkdownSegment {
    data class Prose(val text: String) : MarkdownSegment
    data class CodeBlock(val language: String, val code: String) : MarkdownSegment
}

@Composable
fun MarkdownMessage(
    text: String,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false
) {
    val segments = remember(text) { parseMarkdownSegments(text) }

    if (isStreaming) {
        MarkdownSegmentsColumn(segments = segments, modifier = modifier)
    } else {
        SelectionContainer(modifier = modifier) {
            MarkdownSegmentsColumn(segments = segments)
        }
    }
}

@Composable
private fun MarkdownSegmentsColumn(
    segments: List<MarkdownSegment>,
    modifier: Modifier = Modifier
) {
    val xireColors = LocalXireColors.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        segments.forEach { segment ->
            when (segment) {
                is MarkdownSegment.Prose -> {
                    val styled = remember(segment.text, xireColors.sidebar) {
                        buildInlineAnnotatedString(segment.text, xireColors.sidebar)
                    }
                    Text(
                        text = styled,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                is MarkdownSegment.CodeBlock -> {
                    CodeBlockCard(
                        language = segment.language,
                        code = segment.code
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeBlockCard(
    language: String,
    code: String
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(xireColors.sidebar)
            .border(1.dp, xireColors.border, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = language.ifBlank { "code" },
                fontFamily = InterFontFamily,
                fontSize = 12.sp,
                color = xireColors.muted
            )
            Surface(
                onClick = {
                    clipboard.setText(AnnotatedString(code))
                    copied = true
                },
                shape = RoundedCornerShape(999.dp),
                color = xireColors.card,
                modifier = Modifier.border(1.dp, xireColors.border, RoundedCornerShape(999.dp))
            ) {
                Text(
                    text = if (copied) "Copied ✓" else "Copy",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        HorizontalDivider(color = xireColors.border, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            Text(
                text = code,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

private fun parseMarkdownSegments(input: String): List<MarkdownSegment> {
    val parts = input.split("```")
    val result = mutableListOf<MarkdownSegment>()
    parts.forEachIndexed { index, raw ->
        if (index % 2 == 0) {
            val trimmed = raw.trim('\n')
            if (trimmed.isNotBlank()) {
                result += MarkdownSegment.Prose(trimmed)
            }
        } else {
            val nl = raw.indexOf('\n')
            var lang = ""
            var code = raw
            if (nl >= 0) {
                val firstLine = raw.substring(0, nl).trim()
                if (firstLine.matches(CODE_LANG_REGEX)) {
                    lang = firstLine
                    code = raw.substring(nl + 1)
                }
            } else if (raw.matches(CODE_LANG_REGEX)) {
                lang = raw
                code = ""
            }
            result += MarkdownSegment.CodeBlock(
                language = lang,
                code = code.trimEnd('\n')
            )
        }
    }
    if (result.isEmpty() && input.isNotBlank()) {
        result += MarkdownSegment.Prose(input.trim())
    }
    return result
}

private fun buildInlineAnnotatedString(
    text: String,
    codeBgColor: androidx.compose.ui.graphics.Color
): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split('\n')
        lines.forEachIndexed { lineIdx, rawLine ->
            if (lineIdx > 0) append('\n')
            val trimmed = rawLine.trimStart()
            val leadingSpaces = rawLine.length - trimmed.length

            when {
                trimmed.startsWith("### ") -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp))
                    appendInlineFormatted(trimmed.removePrefix("### "), codeBgColor)
                    pop()
                }
                trimmed.startsWith("## ") -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.5.sp))
                    appendInlineFormatted(trimmed.removePrefix("## "), codeBgColor)
                    pop()
                }
                trimmed.startsWith("# ") -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp))
                    appendInlineFormatted(trimmed.removePrefix("# "), codeBgColor)
                    pop()
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val indent = " ".repeat((leadingSpaces / 2).coerceAtMost(4) * 2)
                    append(indent)
                    pushStyle(SpanStyle(color = XireBronze, fontWeight = FontWeight.Bold))
                    append("• ")
                    pop()
                    appendInlineFormatted(trimmed.substring(2), codeBgColor)
                }
                else -> {
                    appendInlineFormatted(rawLine, codeBgColor)
                }
            }
        }
    }
}

private fun AnnotatedString.Builder.appendInlineFormatted(
    text: String,
    codeBgColor: androidx.compose.ui.graphics.Color
) {
    var i = 0
    while (i < text.length) {
        if (text[i] == '`') {
            val end = text.indexOf('`', i + 1)
            if (end > i + 1) {
                val codeContent = text.substring(i + 1, end)
                pushStyle(
                    SpanStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 14.sp,
                        background = codeBgColor
                    )
                )
                append(" $codeContent ")
                pop()
                i = end + 1
                continue
            }
        }
        if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
            val end = text.indexOf("**", i + 2)
            if (end > i + 2) {
                val boldContent = text.substring(i + 2, end)
                pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold))
                append(boldContent)
                pop()
                i = end + 2
                continue
            }
        }
        if (text[i] == '*' && (i + 1 < text.length && text[i + 1] != ' ')) {
            val end = text.indexOf('*', i + 1)
            if (end > i + 1 && text[end - 1] != ' ') {
                val italicContent = text.substring(i + 1, end)
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(italicContent)
                pop()
                i = end + 1
                continue
            }
        }
        append(text[i])
        i++
    }
}
