package io.github.razziell.freebuddynext.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import io.github.razziell.freebuddynext.ui.appStringResource as stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import io.github.razziell.freebuddynext.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Shared chrome stays outside both pager and page scrolling. Draft values live in the caller. */
@Composable
internal fun WidgetSettingsLayout(
    pages: List<String>,
    preview: @Composable () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    pageContent: @Composable (Int) -> Unit,
) {
    val pager = rememberPagerState(pageCount = { pages.size })
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Text(stringResource(R.string.widget_transparency_title),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                if (maxWidth > maxHeight && maxWidth >= 600.dp) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        FittedWidgetPreview(Modifier.weight(0.38f).padding(16.dp), preview)
                        WidgetSettingsPager(pages, pager, Modifier.weight(0.62f), pageContent)
                    }
                } else {
                    // Reserve most of the available height for controls. The preview scales
                    // as a whole on a small window, rather than cropping bars or labels.
                    val previewHeight = (maxHeight * 0.32f).coerceAtMost(180.dp)
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        FittedWidgetPreview(Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                            .heightIn(max = previewHeight), preview)
                        Spacer(Modifier.height(12.dp))
                        WidgetSettingsPager(pages, pager, Modifier.weight(1f).fillMaxWidth(), pageContent)
                    }
                }
            }
            Row(Modifier.align(Alignment.CenterHorizontally).widthIn(max = 480.dp)
                .fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WidgetActionButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cancel))
                }
                WidgetActionButton(onClick = onSave, modifier = Modifier.weight(1f), filled = true) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetSettingsPager(pages: List<String>, pager: PagerState, modifier: Modifier, pageContent: @Composable (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    Column(modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = pager.currentPage, modifier = Modifier.fillMaxWidth()) {
            pages.forEachIndexed { index, title ->
                Tab(selected = pager.currentPage == index,
                    onClick = { scope.launch { pager.animateScrollToPage(index) } },
                    text = { Text(title) })
            }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalAlignment = Alignment.Top, key = { it }) { page ->
            // Scroll belongs to each page, never to the fixed preview or bottom actions.
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                pageContent(page)
            }
        }
    }
}

/** Measure the real preview at natural height, then fit it without changing its proportions. */
@Composable
private fun FittedWidgetPreview(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(modifier = modifier, content = { Box(contentAlignment = Alignment.Center) { content() } }) { measurables, constraints ->
        val width = minOf(constraints.maxWidth, 340.dp.roundToPx())
        val preview = measurables.single().measure(Constraints(maxWidth = width))
        val height = constraints.constrainHeight(preview.height)
        val scale = if (preview.height > 0) minOf(1f, height.toFloat() / preview.height) else 1f
        val layoutWidth = constraints.constrainWidth(preview.width)
        layout(layoutWidth, height) {
            preview.placeWithLayer(((layoutWidth - preview.width * scale) / 2f).roundToInt(), 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}
