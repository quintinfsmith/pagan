/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.composable.wrappers.DropdownMenu
import com.qfs.pagan.composable.wrappers.DropdownMenuItem
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.enumerate
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Dimensions.Unpadded
import com.qfs.pagan.ui.theme.Shapes
import kotlin.math.roundToInt

@Composable
fun <T> SortableMenu(
    modifier: Modifier = Modifier,
    sort_row_padding: PaddingValues = Unpadded,
    default_menu: List<Pair<T, @Composable RowScope.() -> Unit>>,
    sort_options: List<Pair<Int, (Int, Int) -> Int>>,
    active_sort_option: MutableIntState = mutableIntStateOf(-1),
    default_value: T? = null,
    title_content: @Composable (RowScope.() -> Unit)? = null,
    other: @Composable (RowScope.() -> Unit)? = null,
    onLongClick: (T) -> Unit = {},
    onClick: (T) -> Unit
) {
    val sorted_menu = if (sort_options.isEmpty() || active_sort_option.intValue == -1) {
        default_menu
    } else {
        val indices = default_menu.indices.sortedWith(sort_options[active_sort_option.intValue].second)
        List(default_menu.size) { i -> default_menu[indices[i]] }
    }

    var default_index = -1
    for ((i, item) in sorted_menu.enumerate()) {
        if (item.first == default_value) {
            default_index = i
            break
        }
    }

    val scroll_state = rememberScrollState()
    val item_map = HashMap<Int, Float>()

    Column(modifier = modifier) {
        if (sort_options.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(sort_row_padding)
            ) {
                val expanded = remember { mutableStateOf(false) }

                title_content?.let { it() }
                Spacer(Modifier.weight(1F))
                other?.let {
                    it()
                    Spacer(Modifier.width(Dimensions.SortableMenuHeadSpacing))
                }
                Box {
                    Button(
                        modifier = Modifier
                            .height(Dimensions.SortableMenuSortButtonDiameter)
                            .width(Dimensions.SortableMenuSortButtonDiameter),
                        colors = ButtonDefaults.buttonColors().copy(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = { expanded.value = !expanded.value },
                        contentPadding = Dimensions.SortableMenuSortButtonPadding,
                        content = {
                            Icon(
                                painter = painterResource(R.drawable.icon_sort),
                                contentDescription = stringResource(R.string.cd_sort_options)
                            )
                        }
                    )
                    DropdownMenu(
                        expanded = expanded.value,
                        onDismissRequest = { expanded.value = false }
                    ) {
                        for (x in sort_options.indices) {
                            DropdownMenuItem(
                                modifier = Modifier
                                    .then(
                                        if (x == active_sort_option.value) {
                                            Modifier.background(color = MaterialTheme.colorScheme.tertiary)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                text = {
                                    if (x == active_sort_option.value) {
                                        ProvideContentColorTextStyle(MaterialTheme.colorScheme.onTertiary) {
                                            Text(sort_options[x].first)
                                        }
                                    } else {
                                        Text(sort_options[x].first)
                                    }
                                },
                                onClick = {
                                    expanded.value = false
                                    active_sort_option.value = x
                                }
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .clip(Shapes.SortableMenuBox),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(Dimensions.SortableMenuLineGap)
                    .verticalScroll(scroll_state)
                    .clip(Shapes.SortableMenuBox)
            ) {
                sorted_menu.forEachIndexed { i, (item, label_content) ->
                    if (i > 0) {
                        Spacer(Modifier.height(Dimensions.SortableMenuLineGap))
                    }

                    ProvideContentColorTextStyle(
                        if (default_index == i) {
                            MaterialTheme.colorScheme.onTertiary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .testTag(TestTag.MenuItem, i)
                                .then(
                                    if (default_index == i) {
                                        Modifier.background(
                                            MaterialTheme.colorScheme.tertiary,
                                            Shapes.SortableMenuBox
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .heightIn(min = Dimensions.DialogLineHeight)
                                .combinedClickable(
                                    onClick = { onClick(item) },
                                    onLongClick = { onLongClick(item) }
                                )
                                .onPlaced { coordinates ->
                                    item_map[i] = coordinates.positionInParent().y
                                }
                                .fillMaxWidth()
                                .padding(Dimensions.DialogLinePadding),
                            content = label_content,
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(rememberCoroutineScope()) {
        if (default_index > -1) {
            scroll_state.scrollTo(item_map[default_index]?.roundToInt() ?: 0)
        }
    }
}

@Composable
fun <T> UnSortableMenu(
    modifier: Modifier = Modifier,
    options: List<Pair<T, @Composable RowScope.() -> Unit>>,
    default_value: T? = null,
    callback: (T) -> Unit
) {
    SortableMenu(
        modifier,
        Unpadded,
        options,
        listOf(),
        default_value = default_value,
        onClick = callback
    )
}

