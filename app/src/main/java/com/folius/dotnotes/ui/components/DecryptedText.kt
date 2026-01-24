package com.folius.dotnotes.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

@Composable
fun DecryptedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    speed: Long = 50,
    maxIterations: Int = 10,
    sequential: Boolean = false,
    revealDirection: RevealDirection = RevealDirection.START,
    useOriginalCharsOnly: Boolean = false,
    characters: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!@#$%^&*()_+",
    encryptedColor: Color = Color.Green
) {
    var displayText by remember { mutableStateOf(text) }
    var revealedIndices by remember { mutableStateOf(setOf<Int>()) }
    var isScrambling by remember { mutableStateOf(true) }
    var currentIteration by remember { mutableIntStateOf(0) }

    LaunchedEffect(text, isScrambling) {
        if (!isScrambling) return@LaunchedEffect
        
        val availableChars = if (useOriginalCharsOnly) {
            text.toSet().filter { it != ' ' }.toList()
        } else {
            characters.toList()
        }

        while (isActive && isScrambling) {
            delay(speed)
            
            if (sequential) {
                if (revealedIndices.size < text.length) {
                    val nextIndex = getNextIndex(revealedIndices, text.length, revealDirection)
                    if (nextIndex != -1) {
                        revealedIndices = revealedIndices + nextIndex
                        displayText = shuffleText(text, revealedIndices, availableChars, useOriginalCharsOnly)
                    } else {
                        // Should be done
                        isScrambling = false
                    }
                } else {
                    isScrambling = false
                }
            } else {
                displayText = shuffleText(text, revealedIndices, availableChars, useOriginalCharsOnly)
                currentIteration++
                if (currentIteration >= maxIterations) {
                    isScrambling = false
                    displayText = text
                    revealedIndices = text.indices.toSet()
                }
            }
        }
    }

    // Reset when text changes
    LaunchedEffect(text) {
        revealedIndices = emptySet()
        currentIteration = 0
        isScrambling = true
    }

    Text(
        text = buildAnnotatedString {
            displayText.forEachIndexed { index, char ->
                if (revealedIndices.contains(index) || !isScrambling) {
                    append(char)
                } else {
                    withStyle(SpanStyle(color = encryptedColor)) {
                        append(char)
                    }
                }
            }
        },
        modifier = modifier,
        style = style
    )
}

enum class RevealDirection {
    START, END, CENTER
}

private fun getNextIndex(revealedSet: Set<Int>, length: Int, direction: RevealDirection): Int {
    val indices = (0 until length).toList()
    val unrevealed = indices.filter { !revealedSet.contains(it) }
    
    if (unrevealed.isEmpty()) return -1

    return when (direction) {
        RevealDirection.START -> unrevealed.first()
        RevealDirection.END -> unrevealed.last()
        RevealDirection.CENTER -> {
            // Simple approx center logic
            val center = length / 2
            unrevealed.minByOrNull { kotlin.math.abs(it - center) } ?: -1
        }
    }
}

private fun shuffleText(
    originalText: String,
    revealedSet: Set<Int>,
    availableChars: List<Char>,
    useOriginalCharsOnly: Boolean
): String {
    return originalText.mapIndexed { index, char ->
        if (char == ' ' || revealedSet.contains(index)) {
            char
        } else {
            availableChars.random()
        }
    }.joinToString("")
}
