package com.folius.dotnotes.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ripple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DotPill(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
    isMenuExpanded: Boolean = false,
    onMenuExpandedChange: (Boolean) -> Unit = {},
    searchText: String = "",
    onSearchTextChange: (String) -> Unit = {},
    placeholderText: String = "Search notes...",
    actions: @Composable RowScope.() -> Unit = {},
    menuContent: @Composable ColumnScope.() -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    pillColor: Color = MaterialTheme.colorScheme.surface,
    title: String? = null,
    onTitleChange: (String) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    
    val pillOffset = remember { Animatable(0f) }
    val pillVerticalOffset = remember { Animatable(0f) }
    
    val animatedPillWidthDp by animateDpAsState(
        targetValue = if (isMenuExpanded || isExpanded) 340.dp else 220.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pillWidth"
    )
    val animatedPillHeight by animateDpAsState(
        targetValue = if (isMenuExpanded) 500.dp else 56.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pillHeight"
    )
    val animatedCornerRadius by animateDpAsState(
        targetValue = if (isMenuExpanded) 32.dp else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pillCorner"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // The Pill itself
        Box(
            modifier = Modifier
                .offset { IntOffset(pillOffset.value.roundToInt(), pillVerticalOffset.value.roundToInt()) }
                .width(animatedPillWidthDp)
                .height(animatedPillHeight)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(animatedCornerRadius)
                )
                .shadow(
                    elevation = if (isMenuExpanded) 16.dp else 4.dp,
                    shape = RoundedCornerShape(animatedCornerRadius)
                )
                .clip(RoundedCornerShape(animatedCornerRadius))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            pillColor.copy(alpha = 0.9f),
                            pillColor.copy(alpha = 0.7f)
                        )
                    )
                )
                .pointerInput(isMenuExpanded, isExpanded) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                // Only horizontal if menu closed
                                if (!isMenuExpanded) {
                                    pillOffset.snapTo(pillOffset.value + dragAmount.x * 0.5f)
                                }
                                
                                // Vertical allowed always, but damped differently
                                if (isMenuExpanded) {
                                    // If menu open, allow dragging down to close
                                    if (dragAmount.y != 0f) {
                                        pillVerticalOffset.snapTo(pillVerticalOffset.value + dragAmount.y * 0.7f)
                                    }
                                } else {
                                    if (dragAmount.y < 0) {
                                        pillVerticalOffset.snapTo(pillVerticalOffset.value + dragAmount.y * 0.3f)
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                val hOffset = pillOffset.value
                                val vOffset = pillVerticalOffset.value
                                val threshold = 100f
                                
                                if (!isMenuExpanded) {
                                    if (hOffset > threshold) onSwipeRight()
                                    else if (hOffset < -threshold) onSwipeLeft()
                                    
                                    if (vOffset < -80f) {
                                        onMenuExpandedChange(true)
                                        onSwipeUp()
                                    }
                                } else {
                                    // Dismiss if dragged down significantly
                                    if (vOffset > 100f) {
                                        onMenuExpandedChange(false)
                                    }
                                }
                                
                                pillOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                pillVerticalOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        }
                    )
                }
                .combinedClickable(
                    enabled = !isMenuExpanded && !isExpanded,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onLongClick = onLongClick,
                    onClick = {
                        onExpandedChange(true)
                    }
                )
        ) {
            // Content region
            AnimatedContent(
                targetState = isMenuExpanded,
                transitionSpec = {
                    fadeIn(tween(200)) + scaleIn(initialScale = 0.95f) togetherWith fadeOut(tween(150))
                },
                label = "pillContentAnimation"
            ) { menuActive ->
                if (menuActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Drag handle
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        menuContent()
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isExpanded) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = searchText,
                                onValueChange = onSearchTextChange,
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    focusManager.clearFocus()
                                    onExpandedChange(false)
                                }),
                                decorationBox = { innerTextField ->
                                    if (searchText.isEmpty()) {
                                        Text(
                                            placeholderText,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = { onSearchTextChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                                }
                            }
                        } else {
                            // Centered Label or Title
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (title != null) {
                                    BasicTextField(
                                        value = title,
                                        onValueChange = onTitleChange,
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        ),
                                        decorationBox = { innerTextField ->
                                            if (title.isEmpty()) {
                                                Text(
                                                    text = placeholderText,
                                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                } else {
                                    Text(
                                        text = placeholderText,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            
                            // Optional end actions (like mic or more)
                            actions()
                        }
                    }
                }
            }
        }
    }
}
