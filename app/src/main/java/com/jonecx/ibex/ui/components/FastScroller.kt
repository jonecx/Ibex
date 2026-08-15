package com.jonecx.ibex.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// A drag-to-scrub fast scrollbar: grab the right edge, a letter bubble tracks the section you land on.
// The thumb fades in on scroll or drag and hides after a beat; when nothing scrolls, it stays hidden.
private const val AutoHideMillis = 1100L
private val RailWidth = 28.dp
private val ThumbWidth = 6.dp
private val ThumbHeight = 44.dp
private val BubbleSize = 64.dp
private val BubbleGap = 12.dp

const val FastScrollThumbTag = "fast_scroll_thumb"
const val FastScrollBubbleTag = "fast_scroll_bubble"

// List overload: reads position and motion straight off the LazyColumn state.
@Composable
fun FastScroller(
    state: LazyListState,
    itemCount: Int,
    labelForIndex: (Int) -> String,
    modifier: Modifier = Modifier,
) {
    // derivedStateOf keeps the scroller off the recomposition path except when these scalars actually change.
    val canScroll by remember(state) { derivedStateOf { state.canScrollForward || state.canScrollBackward } }
    val visibleCount by remember(state) { derivedStateOf { state.layoutInfo.visibleItemsInfo.size } }
    FastScrollerContent(
        totalItems = itemCount,
        firstVisibleIndex = state.firstVisibleItemIndex,
        visibleCount = visibleCount,
        canScroll = canScroll,
        isScrolling = state.isScrollInProgress,
        labelForIndex = labelForIndex,
        onScrollToIndex = { index -> state.requestScrollToItem(index) },
        modifier = modifier,
    )
}

// Grid overload: same behaviour, driven by the LazyVerticalGrid state.
@Composable
fun FastScroller(
    state: LazyGridState,
    itemCount: Int,
    labelForIndex: (Int) -> String,
    modifier: Modifier = Modifier,
) {
    val canScroll by remember(state) { derivedStateOf { state.canScrollForward || state.canScrollBackward } }
    val visibleCount by remember(state) { derivedStateOf { state.layoutInfo.visibleItemsInfo.size } }
    FastScrollerContent(
        totalItems = itemCount,
        firstVisibleIndex = state.firstVisibleItemIndex,
        visibleCount = visibleCount,
        canScroll = canScroll,
        isScrolling = state.isScrollInProgress,
        labelForIndex = labelForIndex,
        onScrollToIndex = { index -> state.requestScrollToItem(index) },
        modifier = modifier,
    )
}

// Stateless core: owns only the drag/auto-hide interaction so both list and grid share one implementation.
@Composable
fun FastScrollerContent(
    totalItems: Int,
    firstVisibleIndex: Int,
    visibleCount: Int,
    canScroll: Boolean,
    isScrolling: Boolean,
    labelForIndex: (Int) -> String,
    onScrollToIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isScrolling, dragging) {
        if (isScrolling || dragging) {
            visible = true
        } else {
            delay(AutoHideMillis)
            visible = false
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible && canScroll) 1f else 0f,
        label = "fastScrollAlpha",
    )

    // Resting thumb tracks the first visible item; the last page still lets it reach the bottom.
    val restingFraction = run {
        val denom = (totalItems - visibleCount).coerceAtLeast(1)
        (firstVisibleIndex.toFloat() / denom).coerceIn(0f, 1f)
    }
    val thumbFraction = if (dragging) dragFraction else restingFraction
    val label = if (totalItems > 0) labelForIndex(indexForFraction(dragFraction, totalItems)) else ""

    val gestures = Modifier.pointerInput(totalItems, canScroll) {
        if (!canScroll) return@pointerInput
        val height = size.height.toFloat()
        val scrub = { y: Float ->
            val fraction = (y / height).coerceIn(0f, 1f)
            dragFraction = fraction
            onScrollToIndex(indexForFraction(fraction, totalItems))
        }
        detectVerticalDragGestures(
            onDragStart = { offset ->
                dragging = true
                scrub(offset.y)
            },
            onVerticalDrag = { change, _ ->
                scrub(change.position.y)
                change.consume()
            },
            onDragEnd = { dragging = false },
            onDragCancel = { dragging = false },
        )
    }

    FastScrollOverlay(
        alpha = alpha,
        thumbFraction = thumbFraction,
        dragging = dragging,
        bubbleFraction = dragFraction,
        label = label,
        modifier = modifier.then(gestures),
    )
}

// Pure visual: draws the thumb and letter bubble for the given state. Stateless so previews and screenshots
// can render every state directly.
@Composable
fun FastScrollOverlay(
    alpha: Float,
    thumbFraction: Float,
    dragging: Boolean,
    bubbleFraction: Float,
    label: String,
    modifier: Modifier = Modifier,
) {
    val thumbColor by animateColorAsState(
        targetValue = if (dragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        label = "fastScrollThumb",
    )
    val description = stringResource(R.string.fast_scroll)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(RailWidth)
            .semantics { contentDescription = description },
    ) {
        val thumbTop = (maxHeight - ThumbHeight) * thumbFraction.coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = thumbTop)
                .size(width = ThumbWidth, height = ThumbHeight)
                .alpha(alpha)
                .background(thumbColor, RoundedCornerShape(ThumbWidth / 2))
                .testTag(FastScrollThumbTag),
        )
        if (dragging && label.isNotEmpty()) {
            val bubbleTop = (maxHeight - BubbleSize) * bubbleFraction.coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = -(BubbleSize + BubbleGap), y = bubbleTop)
                    // requiredSize so the bubble keeps its full width instead of being clamped to the narrow rail.
                    .requiredSize(BubbleSize)
                    // Teardrop: three rounded corners, one sharp toward the thumb.
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 32.dp, bottomEnd = 8.dp),
                    )
                    .testTag(FastScrollBubbleTag),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.wrapContentSize(),
                )
            }
        }
    }
}

// Maps a 0..1 scrub position onto an item index.
private fun indexForFraction(fraction: Float, totalItems: Int): Int =
    (fraction.coerceIn(0f, 1f) * (totalItems - 1)).roundToInt().coerceIn(0, (totalItems - 1).coerceAtLeast(0))
