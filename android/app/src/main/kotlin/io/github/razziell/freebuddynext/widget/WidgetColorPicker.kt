package io.github.razziell.freebuddynext.widget

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import io.github.razziell.freebuddynext.ui.appStringResource as stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.razziell.freebuddynext.R
import kotlin.math.*

/** Geometry is independent of Android so edge/wrap behaviour can be unit tested. */
internal object WidgetPickerGeometry {
    fun hue(x: Float, y: Float): Float = ((atan2(y, x) * 180f / PI.toFloat()) + 360f) % 360f
    fun component(position: Float, minimum: Float, extent: Float): Float =
        if (extent <= 0f) 0f else ((position - minimum) / extent).coerceIn(0f, 1f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WidgetColorControl(
    title: String,
    mode: Int,
    customRgb: Int,
    previewRgb: Int,
    onMode: (Int) -> Unit,
    onCustom: (Int) -> Unit,
    textColor: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val options = if (textColor) listOf(
        WidgetTextColorMode.AUTO to stringResource(R.string.widget_color_auto),
        WidgetTextColorMode.WHITE to stringResource(R.string.widget_color_white),
        WidgetTextColorMode.BLACK to stringResource(R.string.widget_color_black),
        WidgetTextColorMode.ACCENT to stringResource(R.string.widget_color_accent),
        WidgetTextColorMode.CUSTOM to stringResource(R.string.widget_color_custom),
    ) else listOf(
        WidgetColorMode.DEFAULT to stringResource(R.string.widget_color_default),
        WidgetColorMode.ACCENT to stringResource(R.string.widget_color_accent),
        WidgetColorMode.CUSTOM to stringResource(R.string.widget_color_custom),
    )
    val selectedMode = if (textColor) WidgetTextColorMode.normalize(mode) else WidgetColorMode.normalize(mode)
    val customMode = if (textColor) WidgetTextColorMode.CUSTOM else WidgetColorMode.CUSTOM
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            ExposedDropdownMenuBox(expanded, { expanded = it }) {
                OutlinedTextField(
                    value = options.first { it.first == selectedMode }.second, onValueChange = {}, readOnly = true,
                    label = { Text(stringResource(R.string.widget_color_source)) },
                    leadingIcon = {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(Color(previewRgb))
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(16.dp), singleLine = true,
                )
                ExposedDropdownMenu(expanded, { expanded = false }, shape = RoundedCornerShape(16.dp)) {
                    options.forEach { (id, label) ->
                        WidgetExpressiveDropdownMenuItem(selected = selectedMode == id,
                            text = { Text(label) },
                            trailingIcon = { if (selectedMode == id) Icon(Icons.Default.Check, null) },
                            onClick = { onMode(id); expanded = false })
                    }
                }
            }
            if (selectedMode == customMode) {
                WidgetActionButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.widget_color_choose, WidgetColorHex.format(customRgb)))
                }
            }
        }
    }
    if (picking) WidgetColorPickerDialog(title, customRgb,
        onDismiss = { picking = false }, onApply = { onCustom(it); picking = false })
}

@Composable
private fun WidgetColorPickerDialog(title: String, initialRgb: Int, onDismiss: () -> Unit, onApply: (Int) -> Unit) {
    val initial = remember(initialRgb) { FloatArray(3).also { AndroidColor.colorToHSV(initialRgb, it) } }
    var hue by rememberSaveable(initialRgb) { mutableFloatStateOf(initial[0]) }
    var saturation by rememberSaveable(initialRgb) { mutableFloatStateOf(initial[1]) }
    var value by rememberSaveable(initialRgb) { mutableFloatStateOf(initial[2]) }
    var hex by rememberSaveable(initialRgb) { mutableStateOf(WidgetColorHex.format(initialRgb)) }
    val rgb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
    val favorites = rememberWidgetFavoritePaletteState()
    val selectedRgb = WidgetColorHex.parse(hex)
    val saved = selectedRgb != null && selectedRgb in favorites.colors
    val canToggleFavorite = selectedRgb != null &&
        (saved || favorites.colors.size < WidgetFavoriteColors.LIMIT)
    fun change(h: Float, s: Float, v: Float) {
        hue = h; saturation = s; value = v
        hex = WidgetColorHex.format(AndroidColor.HSVToColor(floatArrayOf(h, s, v)))
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().heightIn(max = 680.dp).padding(20.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Spacer(Modifier.height(4.dp))
                    HueRing(hue, saturation, value, ::change,
                        Modifier.fillMaxWidth().aspectRatio(1f))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shadowElevation = 3.dp,
                        ) {
                            Box(Modifier.padding(4.dp).fillMaxSize().clip(CircleShape)
                                .background(Color(rgb))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                        }
                        OutlinedTextField(hex, onValueChange = { input ->
                            if (input.length <= 7) {
                                hex = input
                                WidgetColorHex.parse(input)?.let { parsed ->
                                    val hsv = FloatArray(3).also { AndroidColor.colorToHSV(parsed, it) }
                                    hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
                                }
                            }
                        }, label = { Text(stringResource(R.string.widget_color_hex)) },
                            trailingIcon = {
                                WidgetFavoriteToggle(
                                    saved = saved,
                                    enabled = canToggleFavorite,
                                    onToggle = { selectedRgb?.let(favorites.toggle) },
                                )
                            },
                            isError = selectedRgb == null,
                            supportingText = if (selectedRgb == null) {
                                { Text(stringResource(R.string.widget_color_hex_error)) }
                            } else null,
                            singleLine = true, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
                    }
                    WidgetFavoritePalette(colors = favorites.colors, selectedRgb = selectedRgb, onSelect = { color ->
                        val hsv = FloatArray(3).also { AndroidColor.colorToHSV(color, it) }
                        hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
                        // Preserve the exact saved RGB rather than round-tripping through HSV.
                        hex = WidgetColorHex.format(color)
                    })
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WidgetActionButton(onClick = onDismiss,
                        modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.cancel))
                    }
                    WidgetActionButton(
                        onClick = { WidgetColorHex.parse(hex)?.let(onApply) },
                        modifier = Modifier.weight(1f),
                        enabled = WidgetColorHex.parse(hex) != null,
                        filled = true,
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetFavoriteToggle(saved: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(interaction) {
        interaction.interactions.collect { event ->
            if (event is PressInteraction.Press) {
                // Complete the pulse even on a quick tap; only the icon scales, not its touch target.
                scale.animateTo(0.78f, tween(80))
                scale.animateTo(1f, spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ))
            }
        }
    }
    IconToggleButton(
        checked = saved,
        onCheckedChange = { onToggle() },
        enabled = enabled,
        modifier = Modifier.size(48.dp),
        interactionSource = interaction,
        colors = IconButtonDefaults.iconToggleButtonColors(
            containerColor = Color.Transparent,
            checkedContainerColor = Color.Transparent,
            checkedContentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Icon(
            imageVector = if (saved) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            contentDescription = stringResource(
                if (saved) R.string.widget_favorites_remove else R.string.widget_favorites_add),
            modifier = Modifier.size(26.dp).graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        )
    }
}

@Composable
private fun HueRing(hue: Float, saturation: Float, value: Float,
    onChange: (Float, Float, Float) -> Unit, modifier: Modifier = Modifier) {
    val currentHue by rememberUpdatedState(hue)
    val currentSaturation by rememberUpdatedState(saturation)
    val currentValue by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    val ringColors = remember { (0..6).map { Color(AndroidColor.HSVToColor(floatArrayOf(it * 60f, 1f, 1f))) } }
    Canvas(modifier.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) * 0.45f
            val stroke = radius * 0.19f
            val half = radius * 0.55f
            val relative = down.position - center
            val distance = relative.getDistance()
            val ring = abs(distance - radius) <= stroke * 0.85f
            val field = abs(relative.x) <= half && abs(relative.y) <= half
            if (!ring && !field) return@awaitEachGesture
            fun update(position: Offset) {
                val p = position - center
                if (ring) change(WidgetPickerGeometry.hue(p.x, p.y), currentSaturation, currentValue)
                else change(currentHue,
                    WidgetPickerGeometry.component(p.x, -half, half * 2),
                    1f - WidgetPickerGeometry.component(p.y, -half, half * 2))
            }
            down.consume()
            update(down.position)
            do {
                val event = awaitPointerEvent()
                val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!pointer.pressed) break
                update(pointer.position)
                pointer.consume()
            } while (true)
        }
    }) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.45f
        val stroke = radius * 0.19f
        val half = radius * 0.55f
        drawCircle(Brush.sweepGradient(ringColors, center), radius, center, style = Stroke(stroke))
        val topLeft = center - Offset(half, half)
        val fieldSize = Size(half * 2, half * 2)
        val hueColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f)))
        drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor), topLeft.x, topLeft.x + fieldSize.width), topLeft, fieldSize)
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black), topLeft.y, topLeft.y + fieldSize.height), topLeft, fieldSize)
        fun marker(point: Offset, radius: Float) {
            drawCircle(Color.Black.copy(alpha = 0.7f), radius + 1.dp.toPx(), point, style = Stroke(3.dp.toPx()))
            drawCircle(Color.White, radius, point, style = Stroke(2.dp.toPx()))
        }
        val radians = hue * PI.toFloat() / 180f
        marker(center + Offset(cos(radians), sin(radians)) * radius, stroke * 0.5f)
        val selectedPoint = topLeft + Offset(saturation * fieldSize.width, (1f - value) * fieldSize.height)
        marker(selectedPoint, 10.dp.toPx())
        // Two-tone crosshair remains visible on both light and dark colors.
        // Keep the exact sample point uncovered with a small central gap.
        val inner = 1.5.dp.toPx()
        val outer = 5.dp.toPx()
        for (direction in listOf(Offset(1f, 0f), Offset(-1f, 0f), Offset(0f, 1f), Offset(0f, -1f))) {
            val start = selectedPoint + direction * inner
            val end = selectedPoint + direction * outer
            drawLine(Color.Black.copy(alpha = 0.8f), start, end, strokeWidth = 3.dp.toPx())
            drawLine(Color.White, start, end, strokeWidth = 1.dp.toPx())
        }
    }
}
