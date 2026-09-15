package io.github.razziell.freebuddynext.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Outlined by default, with an opt-in filled primary action and shared press feedback. */
@Composable
internal fun AppActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    actionDelayMillis: Long = 120,
    filled: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf(false) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentEnabled by rememberUpdatedState(enabled)
    LaunchedEffect(interaction) {
        interaction.interactions.collect { event ->
            if (event is PressInteraction.Press) {
                scale.animateTo(0.9f, tween(90))
                scale.animateTo(1f, spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ))
            }
        }
    }
    val handleClick: () -> Unit = {
            if (currentEnabled && !pending) {
                if (actionDelayMillis <= 0) currentOnClick()
                else {
                    pending = true
                    scope.launch {
                        try {
                            delay(actionDelayMillis)
                            if (currentEnabled) currentOnClick()
                        } finally { pending = false }
                    }
                }
            }
    }
    val animatedModifier = modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
    if (filled) {
        Button(onClick = handleClick, enabled = enabled, modifier = animatedModifier,
            interactionSource = interaction, content = content)
    } else {
        OutlinedButton(onClick = handleClick, enabled = enabled, modifier = animatedModifier,
            interactionSource = interaction, content = content)
    }
}
