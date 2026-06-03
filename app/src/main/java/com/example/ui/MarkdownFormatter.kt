package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

fun parseMarkdown(text: String, baseColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val length = text.length

        while (currentIndex < length) {
            val codeBlockStart = text.indexOf("```", currentIndex)
            val boldStart = text.indexOf("**", currentIndex)
            val italicStart = text.indexOf("*", currentIndex)
            val inlineCodeStart = text.indexOf("`", currentIndex)

            // Find the earliest matching markdown symbol
            val nextMark = listOf(codeBlockStart, boldStart, italicStart, inlineCodeStart)
                .filter { it >= 0 }
                .minOrNull()

            if (nextMark == null) {
                // No more marks, append rest of text
                append(text.substring(currentIndex, length))
                break
            }

            // Append plain text leading up to the mark
            if (nextMark > currentIndex) {
                append(text.substring(currentIndex, nextMark))
            }

            when (nextMark) {
                codeBlockStart -> {
                    val codeEnd = text.indexOf("```", nextMark + 3)
                    if (codeEnd >= 0) {
                        val codeContent = text.substring(nextMark + 3, codeEnd)
                        // Hacky way to skip first newline if it's there indicating language specifier
                        val cleanCode = if (codeContent.startsWith("\n")) codeContent.substring(1) 
                                        else if (codeContent.contains("\n")) codeContent.substring(codeContent.indexOf("\n") + 1)
                                        else codeContent

                        withStyle(style = SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x33000000), // Darker translucent background for block
                            color = Color(0xFFA67CFF) // Purple highlighting
                        )) {
                            append(cleanCode)
                        }
                        currentIndex = codeEnd + 3
                    } else {
                        append("```")
                        currentIndex = nextMark + 3
                    }
                }
                boldStart -> {
                    val endMark = text.indexOf("**", nextMark + 2)
                    if (endMark >= 0) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = baseColor)) {
                            append(text.substring(nextMark + 2, endMark))
                        }
                        currentIndex = endMark + 2
                    } else {
                        append("**")
                        currentIndex = nextMark + 2
                    }
                }
                inlineCodeStart -> {
                    val endMark = text.indexOf("`", nextMark + 1)
                    if (endMark >= 0 && endMark != italicStart) { // Quick check to avoid conflict
                        withStyle(style = SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x22000000), color = Color(0xFFFF7CE9))) {
                            append(text.substring(nextMark + 1, endMark))
                        }
                        currentIndex = endMark + 1
                    } else {
                        append("`")
                        currentIndex = nextMark + 1
                    }
                }
                italicStart -> {
                    // To handle single asterisk italic 
                    // Make sure it's not a list item (space after *)
                    if (nextMark + 1 < length && text[nextMark + 1] == ' ') {
                        append("• ")
                        currentIndex = nextMark + 2
                    } else {
                        val endMark = text.indexOf("*", nextMark + 1)
                        if (endMark >= 0 && endMark != boldStart) {
                            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, color = baseColor)) {
                                append(text.substring(nextMark + 1, endMark))
                            }
                            currentIndex = endMark + 1
                        } else {
                            append("*")
                            currentIndex = nextMark + 1
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownViewer(text: String, color: Color, fontSize: TextUnit, lineHeight: TextUnit) {
    Text(
        text = parseMarkdown(text, color),
        color = color,
        fontSize = fontSize,
        lineHeight = lineHeight
    )
}
