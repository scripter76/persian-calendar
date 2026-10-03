package com.byagowi.persiancalendar.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.byagowi.persiancalendar.global.isGradient
import com.byagowi.persiancalendar.global.userSetTheme
import com.byagowi.persiancalendar.ui.theme.Theme
import com.byagowi.persiancalendar.ui.theme.animateColor
import com.byagowi.persiancalendar.ui.theme.liquidGlass

@Composable
fun AppFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isGlass = userSetTheme == Theme.LIQUID_GLASS
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressAmount by animateFloatAsState(if (pressed && isGlass) 1f else 0f, spring(dampingRatio = .7f, stiffness = 500f))
    val containerColor = if (isGlass) Color.Transparent else FloatingActionButtonDefaults.containerColor
    val contentColor = if (isGlass) androidx.compose.material3.MaterialTheme.colorScheme.primary else contentColorFor(containerColor)
    val shape = if (isGlass) RoundedCornerShape(28.dp) else FloatingActionButtonDefaults.shape
    FloatingActionButton(
        onClick = onClick,
        containerColor = animateColor(containerColor).value,
        contentColor = animateColor(contentColor).value,
        modifier = if (isGlass) modifier.graphicsLayer {
            scaleX = 1f - .04f * pressAmount
            scaleY = 1f - .04f * pressAmount
        }.liquidGlass(shape, pressAmount) else modifier,
        shape = shape,
        interactionSource = interactionSource,
        content = content,
        elevation = if (isGlass || !isGradient) FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
        ) else FloatingActionButtonDefaults.elevation(),
    )
}
