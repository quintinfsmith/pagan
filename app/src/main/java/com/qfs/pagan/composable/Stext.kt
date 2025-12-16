package com.qfs.pagan.composable

import androidx.annotation.IntRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.composable.button.SmallButton
import com.qfs.pagan.composable.button.SmallOutlinedButton
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.compose.material3.DropdownMenu as OriginalDropdownMenu
import androidx.compose.material3.DropdownMenuItem as OriginalDropdownMenuItem
import androidx.compose.material3.Slider as StupidSlider

@Composable
fun SText(
    string_id: Int,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    autoSize: TextAutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    Text(stringResource(string_id), modifier, color)
}

@Composable
fun IntegerInput(value: MutableState<Int>, minimum: Int? = null, maximum: Int? = null, modifier: Modifier = Modifier, outlined: Boolean = true, callback: (Int) -> Unit) {
    val state = rememberTextFieldState("${value.value}")
    val input_transformation = object : InputTransformation {
        override fun TextFieldBuffer.transformInput() {
            var working_string = this.toString()
            val enter_pressed = this.length > 0 && this.charAt(this.length - 1) == '\n'

            if (enter_pressed) {
                working_string = working_string.substring(0, this.length - 1)
            }

            if (working_string == "-" && minimum != null && minimum < 0) return

            if (enter_pressed && working_string.isNotEmpty()) {
                callback(value.value)
                this.revertAllChanges()
                return
            }

            var int_value = try {
                if (working_string.isEmpty()) {
                    0
                } else {
                    this.toString().toInt()
                }
            } catch (_: Exception) {
                this.revertAllChanges()
                return
            }

            minimum?.let {
                int_value = max(it, int_value)
            }
            maximum?.let {
                int_value = min(it, int_value)
            }

            value.value = int_value
        }
    }

    val focus_change_callback = { focus_state: FocusState ->
        if (focus_state.hasFocus) {
            state.edit {
                this.selection = TextRange(0, this.length)
            }
        }
    }

    if (outlined) {
        OutlinedTextField(
            state = state,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
            modifier = modifier.onFocusChanged(focus_change_callback),
            keyboardOptions = KeyboardOptions.Companion.Default.copy(keyboardType = KeyboardType.Companion.Number),
            inputTransformation = input_transformation,
        )
    } else {
        TextField(
            state = state,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
            modifier = modifier.onFocusChanged(focus_change_callback),
            keyboardOptions = KeyboardOptions.Companion.Default.copy(keyboardType = KeyboardType.Companion.Number),
            inputTransformation = input_transformation,
        )

    }
}

@Composable
fun FloatInput(value: MutableState<Float>, minimum: Float? = null, maximum: Float? = null, modifier: Modifier = Modifier, precision: Int? = null, outlined: Boolean = true, callback: (Float) -> Unit) {
    val state = rememberTextFieldState("${value.value}")
    val textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
    val modifier = modifier.onFocusChanged { focus_state ->
        if (focus_state.hasFocus) {
            state.edit {
                this.selection = TextRange(0, this.length)
            }
        }
    }
    val keyboardOptions = KeyboardOptions.Companion.Default.copy(keyboardType = KeyboardType.Companion.Number)
    val inputTransformation = object : InputTransformation {
        override fun TextFieldBuffer.transformInput() {
            var working_string = this.toString()
            val enter_pressed = this.length > 0 && this.charAt(this.length - 1) == '\n'

            if (enter_pressed) {
                working_string = working_string.substring(0, this.length - 1)
            }

            if (working_string.last() == '.') {
                working_string = working_string.substring(0, this.length - 1)
            }

            if (working_string == "-" && minimum != null && minimum < 0F) {
                return
            }

            if (enter_pressed && working_string.isNotEmpty()) {
                callback(value.value)
                this.revertAllChanges()
                return
            }

            var float_value = try {
                if (working_string.isEmpty()) {
                    0F
                } else {
                    this.toString().toFloat()
                }
            } catch (_: Exception) {
                this.revertAllChanges()
                return
            }

            precision?.let {
                val p = 10F.pow(it)
                float_value = (float_value * p).roundToInt().toFloat() / p
            }

            minimum?.let {
                float_value = max(it, float_value)
            }
            maximum?.let {
                float_value = min(it, float_value)
            }



            value.value = float_value
        }
    }

    if (outlined) {
        OutlinedTextField(
            state = state,
            textStyle = textStyle,
            modifier = modifier,
            keyboardOptions = keyboardOptions,
            inputTransformation = inputTransformation
        )
    } else {
        TextField(
            state = state,
            textStyle = textStyle,
            modifier = modifier,
            keyboardOptions = keyboardOptions,
            inputTransformation = inputTransformation
        )
    }
}

@Composable
fun TextInput(modifier: Modifier = Modifier, input: MutableState<String>, maxLines: Int = 1, callback: (String) -> Unit) {
    val state = rememberTextFieldState(input.value)
    OutlinedTextField(
        state = state,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
        modifier = modifier.onFocusChanged { focus_state ->
            if (focus_state.hasFocus) {
                state.edit {
                    this.selection = TextRange(0, this.length)
                }
            }
        },
        keyboardOptions = KeyboardOptions.Companion.Default.copy(keyboardType = KeyboardType.Companion.Text),
        inputTransformation = object : InputTransformation {
            override fun TextFieldBuffer.transformInput() {
                var working_string = this.toString()
                val enter_pressed = this.length > 0 && this.charAt(this.length - 1) == '\n'
                if (enter_pressed) {
                    working_string = working_string.substring(0, this.length - 1)
                }

                if (enter_pressed && working_string.isNotEmpty()) {
                    callback(working_string)
                    this.revertAllChanges()
                    return
                }

                input.value = working_string
            }
        }
    )
}
@Composable
fun <T> SortableMenu(
    modifier: Modifier = Modifier,
    default_menu: List<Pair<T, @Composable RowScope.() -> Unit>>,
    sort_options: List<Pair<Int, (Int, Int) -> Int>>,
    selected_sort: Int = -1,
    default_value: T? = null,
    onLongClick: (T) -> Unit = {},
    onClick: (T) -> Unit
) {
    val active_sort_option = remember { mutableStateOf(selected_sort) }
    val scroll_state = rememberLazyListState()
    var default_index = 0
    Column(modifier = modifier) {
        if (sort_options.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                val expanded = remember { mutableStateOf(false) }
                SText(
                    R.string.sorting_by,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1F)
                )
                Box(modifier = Modifier.weight(1F)) {
                    SmallButton(
                        onClick = { expanded.value = !expanded.value },
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            if (selected_sort == -1) {
                                SText(R.string.unsorted)
                            } else {
                                SText(sort_options[active_sort_option.value].first)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expanded.value,
                        onDismissRequest = { expanded.value = false }
                    ) {
                        for (x in sort_options.indices) {
                            DropdownMenuItem(
                                text = { SText(sort_options[x].first) },
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
        LazyColumn(
            state = scroll_state,
            modifier = Modifier.fillMaxWidth()
        ) {
            val sorted_menu = if (sort_options.isEmpty() || active_sort_option.value == -1) {
                default_menu
            } else {
                val indices = default_menu.indices.sortedWith(sort_options[active_sort_option.value].second)
                List(default_menu.size) { i -> default_menu[indices[i]] }
            }
            itemsIndexed(sorted_menu) { i, (item, label_content) ->
                val row_modifier = Modifier
                Row(
                    modifier = Modifier
                        .then(
                            if (item == default_value) {
                                default_index = i
                                row_modifier.background(colorResource(R.color.leaf_empty_selected))
                            } else {
                                row_modifier
                            }
                        )
                        .then(
                            if (i % 2 == 0) {
                                row_modifier.background(Color(0x10000000))
                            } else {
                                row_modifier.background(Color(0x10FFFFFF))
                            }
                        )
                        .height(dimensionResource(R.dimen.dialog_menu_line_height))
                        .padding(
                            vertical = dimensionResource(R.dimen.dialog_menu_line_padding_vertical),
                             horizontal = dimensionResource(R.dimen.dialog_menu_line_padding_horizontal)
                        )
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onClick(item) },
                            onLongClick = { onLongClick(item) }
                        ),
                    content = label_content,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                )
            }
        }
    }
   // rememberCoroutineScope().launch {
   //     scroll_state.requestScrollToItem(default_index)
   // }
}

@Composable
fun <T> UnSortableMenu(modifier: Modifier = Modifier, options: List<Pair<T, @Composable RowScope.() -> Unit>>, default_value: T? = null, callback: (T) -> Unit) {
    SortableMenu(modifier, options, listOf(), default_value = default_value, onClick = callback)
}

@Composable
fun DialogTitle(text: String, modifier: Modifier = Modifier) {
    ProvideTextStyle(MaterialTheme.typography.titleLarge) {
        Text(
            text = text,
            modifier = modifier
                .padding(
                    vertical = 16.dp,
                    horizontal = 12.dp
                )
        )
    }
}

@Composable
fun DialogSTitle(text: Int, modifier: Modifier = Modifier) {
    DialogTitle(text = stringResource(text), modifier = modifier)
}

@Composable
fun DialogCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardColors(
        containerColor = colorResource(R.color.surface_container),
        contentColor = colorResource(R.color.on_surface_container),
        disabledContentColor = Color.Gray,
        disabledContainerColor = Color.Green,
    ),
    elevation: CardElevation = CardDefaults.cardElevation(),
    shape: Shape = RoundedCornerShape(12.dp),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ProvideContentColorTextStyle(contentColor = colors.contentColor) {
        Box(
            modifier
                .then(if (border != null) modifier.border(border) else modifier)
                .background(color = colors.containerColor, shape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(dimensionResource(R.dimen.dialog_padding)),
                content = content
            )
        }
    }
}

@Composable
fun DialogBar(modifier: Modifier = Modifier.fillMaxWidth(), positive: (() -> Unit)? = null, negative: (() -> Unit)? = null, neutral: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .padding(
                top = dimensionResource(R.dimen.dialog_bar_padding_vertical),
                bottom = 0.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        negative?.let {
            SmallButton(
                modifier = Modifier.weight(1f),
                onClick = it,
                content = { SText(R.string.no) }
            )
        }
        neutral?.let {
            SmallOutlinedButton(
                modifier = Modifier.weight(1F),
                onClick = it,
                content = { SText(android.R.string.cancel) }
            )
        }
        positive?.let {
            SmallButton(
                modifier = Modifier.weight(1f),
                onClick = it,
                content = { SText(android.R.string.ok) }
            )
        }
    }
}

// Every time i set "steps" to be the *actual* number of discrete values I want - 2, I get a stress headache. So I'm not doing that.
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 2,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    StupidSlider(value, onValueChange, modifier, enabled, valueRange, steps - 2, onValueChangeFinished, colors, interactionSource)
}

@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    shape: Shape = MenuDefaults.shape,
    containerColor: Color = colorResource(R.color.surface_container),
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    shadowElevation: Dp = MenuDefaults.ShadowElevation,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    OriginalDropdownMenu(expanded, onDismissRequest, modifier, offset, scrollState, properties, shape, containerColor, tonalElevation, shadowElevation, border, content)
}

@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    OriginalDropdownMenuItem(text, onClick, modifier, leadingIcon, trailingIcon, enabled, colors, contentPadding, interactionSource)
}