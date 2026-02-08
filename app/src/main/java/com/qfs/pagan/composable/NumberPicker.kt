package com.qfs.pagan.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Typography
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun NumberPicker(modifier: Modifier = Modifier, range: IntRange, default: MutableState<Int>) {
    val h = Dimensions.NumberPickerRowHeight

    val column_height = 3
    val page_count = range.last - range.first + 1
    val state = rememberPagerState(
        (default.value - range.first) + (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % page_count),
        pageCount = { Int.MAX_VALUE }
    )

    default.value = (state.currentPage % page_count) + range.first

    val scope = rememberCoroutineScope()
    ProvideContentColorTextStyle(MaterialTheme.colorScheme.onSurface) {
        Box(
            modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                .width(Dimensions.NumberPickerRowWidth)
                .height(h * column_height),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(h),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(
                    Modifier
                        .height(1.dp)
                        .fillMaxWidth(.8F)
                        .background(MaterialTheme.colorScheme.outline)
                )
                Spacer(
                    Modifier
                        .height(1.dp)
                        .fillMaxWidth(.8F)
                        .background(MaterialTheme.colorScheme.outline)
                )
            }

            VerticalPager(
                state = state,
                pageSize = PageSize.Fixed(h),
                snapPosition = SnapPosition.Center,
                beyondViewportPageCount = 6,
                modifier = Modifier
                    .height(h * column_height),
                contentPadding = PaddingValues(vertical = h * 4)
            ) { i ->
                val page = i % page_count
                Row(
                    Modifier
                        .height(h)
                        .graphicsLayer {
                            val page_offset = abs((state.currentPage - i) + state.currentPageOffsetFraction)
                            alpha = lerp(
                                start = 0.0f,
                                stop = 1f,
                                fraction = 1f - (page_offset / 1.5F).coerceIn(0f, 1f)
                            )
                        }
                        .combinedClickable(
                            onClick = {
                                scope.launch { state.scrollToPage(i) }
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    content = {
                        Text(
                            "${range.first + page}",
                            style = Typography.NumberPicker
                        )
                    }
                )
            }
        }
    }
}