package com.folius.dotnotes.ui.components

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlurText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    delay: Long = 200,
    animateByWords: Boolean = true
) {
    // Split into words but preserve whitespace/newlines
    // This regex splits by spaces but keeps the delimiter (the space)
    val elements = if (animateByWords) {
        text.split(Regex("(?<=\\s)|(?=\\s)")).filter { it.isNotEmpty() }
    } else {
        text.map { it.toString() }
    }
    
    FlowRow(modifier = modifier) {
        elements.forEachIndexed { index, element ->
            if (element == "\n") {
                // Force a newline in FlowRow by taking up the full width
                Spacer(modifier = Modifier.fillMaxWidth().height(0.dp))
            } else if (element.isBlank()) {
                // Render space with same style to preserve width
                Text(
                    text = element,
                    style = style,
                    modifier = Modifier.alpha(0f) // Invisible space
                )
            } else {
                AnimatedWord(
                    text = element,
                    style = style,
                    delay = index * delay.toInt()
                )
            }
        }
    }
}

@Composable
private fun AnimatedWord(
    text: String,
    style: TextStyle,
    delay: Int
) {
    val alpha = remember { Animatable(0f) }
    val blur = remember { Animatable(10f) }
    val translationY = remember { Animatable(50f) } // Default direction top, so start from below (50) to 0? 
    // React code: defaultFrom: y: -50 (if top) or 50. defaultTo: y:0. 
    // Let's map "top" means appearing from top? Or moving to top?
    // React: direction='top' -> from y=-50 to y=0.
    
    // We'll stick to a nice "fade up" effect: Start +10y -> 0y.
    
    LaunchedEffect(Unit) {
        delay(delay.toLong())
        launch {
            alpha.animateTo(1f, animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing))
        }
        launch {
            blur.animateTo(0f, animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing))
        }
        launch {
            translationY.animateTo(0f, animationSpec = tween(durationMillis = 500, easing = BackOutEasing))
        }
    }

    Text(
        text = text,
        style = style,
        modifier = Modifier
            .graphicsLayer {
                this.translationY = translationY.value
                // RenderEffect only on S+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                     this.renderEffect = android.graphics.RenderEffect.createBlurEffect(
                         blur.value, blur.value, android.graphics.Shader.TileMode.DECAL
                     ).asComposeRenderEffect()
                }
            }
            .alpha(alpha.value)
    )
}

// Easing for "pop"
val BackOutEasing = Easing { x -> 
    val c1 = 1.70158f;
    val c3 = c1 + 1f;
    1f + c3 * (x - 1f).let { it * it * it } + c1 * (x - 1f).let { it * it }
}
