package com.qfs.pagan.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.button.OutlinedButton
import com.qfs.pagan.composable.wrappers.DropdownMenu
import com.qfs.pagan.composable.wrappers.DropdownMenuItem
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.enumerate
import com.qfs.pagan.ui.theme.Colors
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.ui.theme.Typography

@Composable
fun TuningDialogTopLine(
    transpose_numerator: MutableState<Int>,
    transpose_denominator: MutableState<Int>,
    radix: MutableState<Int>,
    note_map: MutableList<Pair<Int, Int>>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(R.string.dlg_transpose, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IntegerInput(
                    value = transpose_numerator,
                    minimum = 0,
                    contentPadding = PaddingValues(Dimensions.TransposeDialogInputPadding),
                    modifier = Modifier.width(Dimensions.TransposeDialogInputWidth),
                    revert_on_exit = true,
                    callback = {
                       // transpose_numerator.value = it
                    }
                )
                DivisorSeparator()
                IntegerInput(
                    value = transpose_denominator,
                    minimum = 1,
                    contentPadding = PaddingValues(Dimensions.TransposeDialogInputPadding),
                    modifier = Modifier.width(Dimensions.TransposeDialogInputWidth),
                    revert_on_exit = true,
                    callback = {
                        //transpose_denominator.value = it
                    }
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                R.string.dlg_set_radix,
                maxLines = 1,
                textAlign = TextAlign.End
            )
            IntegerInput(
                value = radix,
                minimum = 1,
                maximum = 36,
                contentPadding = PaddingValues(Dimensions.TransposeDialogInnerPadding),
                modifier = Modifier.width(Dimensions.TransposeDialogInputWidth),
                revert_on_exit = true,
                callback = {
                    note_map.clear()
                    //radix.value = it
                    for (i in 0 until it) {
                        note_map.add(Pair(i, it))
                    }
                }
            )
        }
    }
}

@Composable
fun ColumnScope.TuningDialogNormal(
    close_callback: () -> Unit,
    transpose_numerator: MutableState<Int>,
    transpose_denominator: MutableState<Int>,
    radix: MutableState<Int>,
    note_map: MutableList<Pair<Int, Int>>,
    callback: (Array<Pair<Int, Int>>, Pair<Int, Int>) -> Unit
) {

    TuningDialogTopLine(transpose_numerator, transpose_denominator, radix, note_map)

    Spacer(Modifier.height(Dimensions.TransposeDialogInnerPadding))

    Surface(
        modifier = Modifier.weight(1F, fill = false),
        border = BorderStroke(
            Dimensions.TuningDialogStrokeWidth,
            Colors.active_color_scheme.container_border
        ),
        color = Colors.active_color_scheme.menu_background,
        shape = Shapes.TuningDialogBox,
    ) {
        key(radix.value) {
            FlowRow(
                modifier = Modifier
                    .padding(Dimensions.TuningDialogBoxPadding)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                for ((i, state) in note_map.enumerate()) {
                    val (numer, denom) = state
                    Surface(
                        modifier = Modifier.padding(Dimensions.TuningDialogLineSpacing),
                        color = Colors.active_color_scheme.tuning_table_item,
                        shape = Shapes.TuningDialogBox,
                    ) {
                        Row(
                            modifier = Modifier.padding(Dimensions.TuningDialogLinePadding),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "%02d".format(i),
                                modifier = Modifier.padding(horizontal = Dimensions.TuningDialogLinePadding)
                            )
                            Spacer(Modifier.weight(1F))
                            IntegerInput(
                                value = remember { mutableStateOf(numer) },
                                minimum = 0,
                                modifier = Modifier.width(Dimensions.TransposeDialogInputWidth),
                                contentPadding = PaddingValues(Dimensions.TransposeDialogInputPadding),
                                revert_on_exit = true,
                                callback = { note_map[i] = Pair(it, note_map[i].second) }
                            )
                            DivisorSeparator()
                            IntegerInput(
                                value = remember { mutableStateOf(denom) },
                                minimum = 1,
                                modifier = Modifier.width(Dimensions.TransposeDialogInputWidth),
                                contentPadding = PaddingValues(Dimensions.TransposeDialogInputPadding),
                                revert_on_exit = true,
                                callback = { note_map[i] = Pair(note_map[i].first, it) }
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(
        Modifier
            .height(Dimensions.Space.Medium)
            .fillMaxWidth()
    )

    DialogBar(
        neutral = close_callback,
        positive = {
            close_callback()
            callback(
                Array(note_map.size) { i ->
                    Pair(
                        note_map[i].first,
                        note_map[i].second
                    )
                },
                Pair(
                    transpose_numerator.value,
                    transpose_denominator.value
                )
            )
        }
    )
}

@Composable
fun ColumnScope.TuningDialogTiny(
    close_callback: () -> Unit,
    transpose_numerator: MutableState<Int>,
    transpose_denominator: MutableState<Int>,
    radix: MutableState<Int>,
    note_map: MutableList<Pair<Int, Int>>,
    callback: (Array<Pair<Int, Int>>, Pair<Int, Int>) -> Unit
) {
    val actively_editting_index = remember { mutableStateOf(0) }
    val expanded = remember { mutableStateOf(false) }
    Row(Modifier.height(IntrinsicSize.Min)) {
        Column(
            Modifier
                .fillMaxHeight()
                .weight(1F, fill = false),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            ProvideTextStyle(com.qfs.pagan.ui.theme.Typography.TinyTuningDialogLabel) {
                TuningDialogTopLine(transpose_numerator, transpose_denominator, radix, note_map)
            }
            Column(
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    R.string.label_tuning,
                    style = Typography.TinyTuningDialogLabel
                )
                Surface(
                    shape = Shapes.Container,
                    color = Colors.active_color_scheme.menu_background
                ) {
                    Row(
                        modifier = Modifier
                            .padding(Dimensions.TinyTuningDialogInnerPadding)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DropdownMenu(
                            expanded = expanded.value,
                            onDismissRequest = { expanded.value = false }
                        ) {
                            for (i in 0 until radix.value) {
                                DropdownMenuItem(
                                    selected = i == actively_editting_index.value,
                                    text = {
                                        Text("${"%02d".format(i)}: ${note_map[i].first} / ${note_map[i].second}")
                                    },
                                    onClick = {
                                        actively_editting_index.value = i
                                        expanded.value = false
                                    }
                                )
                            }
                        }
                        key(radix.value) {
                            Button(
                                content = { Text("%02d".format(actively_editting_index.value)) },
                                onClick = { expanded.value = !expanded.value }
                            )
                            Spacer(Modifier.weight(1F))
                            key(actively_editting_index.value) {
                                IntegerInput(
                                    value = remember { mutableStateOf(note_map[actively_editting_index.value].first) },
                                    minimum = 0,
                                    modifier = Modifier
                                        .background(Colors.active_color_scheme.tuning_table_item)
                                        .width(Dimensions.TinyTuningDialogInputWidth),
                                    contentPadding = PaddingValues(Dimensions.TransposeDialogInputPadding),
                                    revert_on_exit = true,
                                    callback = {
                                        note_map[actively_editting_index.value] = Pair(
                                            it,
                                            note_map[actively_editting_index.value].second
                                        )
                                    }
                                )
                                DivisorSeparator()
                                IntegerInput(
                                    value = remember { mutableStateOf(note_map[actively_editting_index.value].second) },
                                    minimum = 1,
                                    modifier = Modifier
                                        .background(Colors.active_color_scheme.tuning_table_item)
                                        .width(Dimensions.TinyTuningDialogInputWidth),
                                    contentPadding = PaddingValues(Dimensions.TransposeDialogInputPadding),
                                    revert_on_exit = true,
                                    callback = {
                                        note_map[actively_editting_index.value] = Pair(
                                            note_map[actively_editting_index.value].first,
                                            it
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(Dimensions.TinyTuningDialogInnerPadding))

        Column(
            Modifier
                .fillMaxHeight()
                .background(color = Colors.active_color_scheme.container, shape = Shapes.Container)
                .border(
                    width = Dimensions.TuningDialogStrokeWidth,
                    color = Colors.active_color_scheme.container_border,
                    shape = Shapes.Container
                )
                .padding(horizontal = Dimensions.TuningDialogBoxPadding),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                modifier = Modifier
                    .width(Dimensions.TinyTuningDialogButtonSize)
                    .height(Dimensions.TinyTuningDialogButtonSize),
                contentPadding = PaddingValues(Dimensions.TinyTuningDialogButtonPadding),
                shape = CircleShape,
                onClick = close_callback,
                content = {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        painter = painterResource(R.drawable.icon_x),
                        contentDescription = stringResource(android.R.string.cancel)
                    )
                }
            )
            Button(
                modifier = Modifier
                    .width(Dimensions.TinyTuningDialogButtonSize)
                    .height(Dimensions.TinyTuningDialogButtonSize),
                contentPadding = PaddingValues(Dimensions.TinyTuningDialogButtonPadding),
                shape = CircleShape,
                onClick = {
                    close_callback()
                    callback(
                        Array(note_map.size) { i ->
                            Pair(note_map[i].first, note_map[i].second)
                        },
                        Pair(
                            transpose_numerator.value,
                            transpose_denominator.value
                        )
                    )
                },
                content = {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        painter = painterResource(R.drawable.icon_check),
                        contentDescription = stringResource(android.R.string.ok)
                    )
                }
            )
        }
    }
}
