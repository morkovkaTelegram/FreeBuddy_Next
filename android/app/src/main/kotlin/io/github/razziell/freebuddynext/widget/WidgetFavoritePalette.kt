package io.github.razziell.freebuddynext.widget

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import io.github.razziell.freebuddynext.ui.appStringResource as stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.razziell.freebuddynext.R

internal data class WidgetFavoritePaletteState(
    val colors: List<Int>,
    val toggle: (Int) -> Unit,
)

/** One observed palette shared by the HEX action and swatches; changes are saved immediately. */
@Composable
internal fun rememberWidgetFavoritePaletteState(): WidgetFavoritePaletteState {
    val context = LocalContext.current.applicationContext
    val prefs = remember(context) { context.getSharedPreferences(FAVORITES_FILE, Context.MODE_PRIVATE) }
    fun read() = WidgetFavoriteColors.decode(prefs.getString(FAVORITE_COLORS_KEY, "").orEmpty())
    var colors by remember(prefs) { mutableStateOf(read()) }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == FAVORITE_COLORS_KEY || key == null) colors = read()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        colors = read()
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return WidgetFavoritePaletteState(colors) { rgb ->
        val color = WidgetColorHex.opaque(rgb)
        // Re-read instead of overwriting with the composition's potentially stale list.
        val current = read()
        val updated = if (color in current) WidgetFavoriteColors.remove(current, color)
            else WidgetFavoriteColors.add(current, color)
        prefs.edit().putString(FAVORITE_COLORS_KEY, WidgetFavoriteColors.encode(updated)).apply()
        colors = updated
    }
}

@Composable
internal fun WidgetFavoritePalette(colors: List<Int>, selectedRgb: Int?, onSelect: (Int) -> Unit) {
    val selected = selectedRgb?.let(WidgetColorHex::opaque)
    val listState = rememberLazyListState()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val thumbColor = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (colors.isNotEmpty()) {
            Text(stringResource(R.string.widget_favorites_title, WidgetFavoriteColors.LIMIT),
                style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
            LazyRow(Modifier.fillMaxWidth().selectableGroup(), state = listState) {
                items(colors, key = { it }) { color ->
                    val isSelected = selected == color
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                            .selectable(selected = isSelected, role = Role.RadioButton,
                                onClick = { onSelect(color) })
                            .semantics { contentDescription = WidgetColorHex.format(color) }
                            .padding(4.dp)
                            .border(if (isSelected) 2.dp else 0.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape).background(Color(color))
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (Color(color).luminance() > 0.179f) Color.Black else Color.White)
                        }
                    }
                }
            }
            if (listState.canScrollBackward || listState.canScrollForward) {
                // Visual indicator only; LazyRow retains swipe and accessibility scroll actions.
                Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                    val layout = listState.layoutInfo
                    // All cells have equal width, no spacing or content padding.
                    val cellWidth = layout.visibleItemsInfo.firstOrNull()?.size?.toFloat()
                        ?: return@Canvas
                    val contentWidth = cellWidth * layout.totalItemsCount
                    val viewportWidth = layout.viewportSize.width.toFloat()
                    val scrollRange = contentWidth - viewportWidth
                    if (scrollRange <= 0f || size.width <= 0f) return@Canvas
                    val offset = listState.firstVisibleItemIndex * cellWidth +
                        listState.firstVisibleItemScrollOffset
                    val progress = (offset / scrollRange).coerceIn(0f, 1f)
                    val thumbWidth = (size.width * viewportWidth / contentWidth)
                        .coerceIn(minOf(24.dp.toPx(), size.width), size.width)
                    val x = (size.width - thumbWidth) * if (rtl) 1f - progress else progress
                    val radius = CornerRadius(size.height / 2f)
                    drawRoundRect(trackColor, cornerRadius = radius)
                    drawRoundRect(thumbColor, topLeft = Offset(x, 0f),
                        size = Size(thumbWidth, size.height), cornerRadius = radius)
                }
            }
        }
    }
}
