package com.qfs.pagan.composable

import androidx.annotation.IntRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.selectAll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.button.OutlinedButton
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.composable.button.SmallButton
import com.qfs.pagan.composable.button.SmallOutlinedButton
import com.qfs.pagan.enumerate
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
fun IntegerInput(
    value: MutableState<Int>,
    minimum: Int? = null,
    maximum: Int? = null,
    modifier: Modifier = Modifier,
    outlined: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    text_align: TextAlign = TextAlign.End,
    prefix: @Composable (() -> Unit)? = null,
    label: (@Composable TextFieldLabelScope.() -> Unit)? = null,
    on_focus_enter: (() -> Unit)? = null,
    on_focus_exit: ((Int?) -> Unit)? = null,
    callback: (Int) -> Unit
) {
    NumberInput(
        value,
        minimum,
        maximum,
        modifier,
        outlined,
        contentPadding,
        text_align,
        prefix,
        label,
        on_focus_enter,
        on_focus_exit,
        object : InputTransformation {
            override fun TextFieldBuffer.transformInput() {
                val working_string = this.toString()
                if (working_string == "-" && minimum != null && minimum < 0) return

                var converted_value = try {
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
                    converted_value = max(it, converted_value)
                }
                maximum?.let {
                    converted_value = min(it, converted_value)
                }

                value.value = converted_value
            }
        },
        callback
    )
}

@Composable
fun FloatInput(
    value: MutableState<Float>,
    precision: Int? = null,
    minimum: Float? = null,
    maximum: Float? = null,
    modifier: Modifier = Modifier,
    outlined: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    text_align: TextAlign = TextAlign.End,
    prefix: @Composable (() -> Unit)? = null,
    label: (@Composable TextFieldLabelScope.() -> Unit)? = null,
    on_focus_enter: (() -> Unit)? = null,
    on_focus_exit: ((Float?) -> Unit)? = null,
    callback: (Float) -> Unit
) {
    NumberInput(
        value,
        minimum,
        maximum,
        modifier,
        outlined,
        contentPadding,
        text_align,
        prefix,
        label,
        on_focus_enter,
        on_focus_exit,
        object : InputTransformation {
            override fun TextFieldBuffer.transformInput() {
                val working_string = this.toString()
                if (working_string == "-" && minimum != null && minimum < 0) return

                var converted_value = try {
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
                    converted_value = (converted_value * p).roundToInt().toFloat() / p
                }
                minimum?.let {
                    converted_value = max(it, converted_value)
                }
                maximum?.let {
                    converted_value = min(it, converted_value)
                }

                value.value = converted_value
            }
        },
        callback
    )
}

@Composable
fun <T: Number> NumberInput(
    value: MutableState<T>,
    minimum: T? = null,
    maximum: T? = null,
    modifier: Modifier = Modifier,
    outlined: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    text_align: TextAlign = TextAlign.End,
    prefix: @Composable (() -> Unit)? = null,
    label: (@Composable TextFieldLabelScope.() -> Unit)? = null,
    on_focus_enter: (() -> Unit)? = null,
    on_focus_exit: ((T?) -> Unit)? = null,
    input_transformation: InputTransformation,
    callback: (T) -> Unit
) {
    val state = rememberTextFieldState(value.value.toString())

    // Prevent weird focusing behavior causing on_focus_exit to be called without any initial focus
    val was_focused = remember { mutableStateOf(false) }
    val focus_change_callback = { focus_state: FocusState ->
        if (focus_state.isFocused) {
            was_focused.value = true
            state.edit { this.selectAll() }
            on_focus_enter?.let { it() }
        } else if (was_focused.value) {
            was_focused.value = false
            on_focus_exit?.let { it(value.value) }
        }
    }

    if (outlined) {
        OutlinedTextField(
            state = state,
            label = label,
            contentPadding = contentPadding,
            textStyle = LocalTextStyle.current.copy(textAlign = text_align),
            prefix = prefix,
            modifier = modifier
                .heightIn(1.dp)
                .widthIn(1.dp)
                .onFocusChanged { focus_change_callback(it) },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Companion.Number
            ),
            inputTransformation = input_transformation,
            lineLimits = TextFieldLineLimits.SingleLine,
            onKeyboardAction = { action ->
                callback(value.value)
                action()
            }
        )
    } else {
        TextField(
            state = state,
            label = label,
            contentPadding = contentPadding,
            textStyle = LocalTextStyle.current.copy(textAlign = text_align),
            prefix = prefix,
            modifier = modifier
                .heightIn(1.dp)
                .widthIn(1.dp)
                .onFocusChanged { focus_change_callback(it) },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Companion.Number
            ),
            inputTransformation = input_transformation,
            lineLimits = TextFieldLineLimits.SingleLine,
            onKeyboardAction = { action ->
                callback(value.value)
                action()
            }
        )
    }

}


@Composable
fun TextInput(
    modifier: Modifier = Modifier,
    input: MutableState<String>,
    textAlign: TextAlign = TextAlign.End,
    label: (@Composable TextFieldLabelScope.() -> Unit)? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    callback: (String) -> Unit
) {
    val state = rememberTextFieldState(input.value)
    OutlinedTextField(
        state = state,
        lineLimits = lineLimits,
        label = label,
        textStyle = LocalTextStyle.current.copy(textAlign = textAlign),
        modifier = modifier.onFocusChanged { focus_state ->
            if (focus_state.hasFocus) {
                state.edit {
                    this.selection = TextRange(0, this.length)
                }
            }
        },
        scrollState = rememberScrollState(),
        onKeyboardAction = { callback(input.value) },
        keyboardOptions = KeyboardOptions.Companion.Default.copy(keyboardType = KeyboardType.Companion.Text),
        inputTransformation = object : InputTransformation {
            override fun TextFieldBuffer.transformInput() {
                input.value = this.toString()
            }
        }
    )
}

@Composable
fun InlineInput(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    interactionSource: MutableInteractionSource? = null
){

    OutlinedTextField(
        state = state,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        placeholder = placeholder,
        isError = isError,
        inputTransformation = inputTransformation,
        outputTransformation = outputTransformation,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        onTextLayout = onTextLayout,
        scrollState = scrollState,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    )
}

@Composable
fun <T> SortableMenu(
    modifier: Modifier = Modifier,
    sort_row_padding: PaddingValues = PaddingValues(0.dp),
    default_menu: List<Pair<T, @Composable RowScope.() -> Unit>>,
    sort_options: List<Pair<Int, (Int, Int) -> Int>>,
    selected_sort: Int = -1,
    default_value: T? = null,
    title_content: @Composable (RowScope.() -> Unit)? = null,
    onLongClick: (T) -> Unit = {},
    onClick: (T) -> Unit
) {
    val active_sort_option = remember { mutableIntStateOf(selected_sort) }
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

    val scroll_state = rememberLazyListState()

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

                Box {
                    Button(
                        modifier = Modifier
                            .height(36.dp)
                            .width(36.dp),
                        colors = ButtonDefaults.buttonColors().copy(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = { expanded.value = !expanded.value },
                        contentPadding = PaddingValues(6.dp),
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

        Surface(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
            tonalElevation = 2.dp
        ) {
            LazyColumn(
                state = scroll_state,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                itemsIndexed(sorted_menu) { i, (item, label_content) ->
                    val row_modifier = Modifier
                    if (i > 0) {
                        Spacer(Modifier.height(4.dp))
                    }

                    ProvideContentColorTextStyle(
                        if (default_index == i) {
                            MaterialTheme.colorScheme.onTertiary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    ) {
                        Row(
                            modifier = row_modifier
                                .then(
                                    if (default_index == i) {
                                        Modifier.background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(8.dp))
                                    } else {
                                        Modifier
                                    }
                                )
                                .heightIn(dimensionResource(R.dimen.dialog_menu_line_height))
                                .combinedClickable(
                                    onClick = { onClick(item) },
                                    onLongClick = { onLongClick(item) }
                                )
                                .padding(
                                    vertical = dimensionResource(R.dimen.dialog_menu_line_padding_vertical),
                                    horizontal = dimensionResource(R.dimen.dialog_menu_line_padding_horizontal)
                                )
                                .fillMaxWidth(),
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
            scroll_state.requestScrollToItem(default_index)
        }
    }
}

@Composable
fun <T> UnSortableMenu(modifier: Modifier = Modifier, options: List<Pair<T, @Composable RowScope.() -> Unit>>, default_value: T? = null, callback: (T) -> Unit) {
    SortableMenu(modifier, PaddingValues(0.dp), options, listOf(), default_value = default_value, onClick = callback)
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
fun DrawerCard(
    modifier: Modifier = Modifier.wrapContentWidth(),
    colors: CardColors = CardColors(
        contentColor = MaterialTheme.colorScheme.onSurface,
        containerColor = MaterialTheme.colorScheme.surface,
        disabledContentColor = Color.Gray,
        disabledContainerColor = Color.Green,
    ),
    elevation: CardElevation = CardDefaults.cardElevation(),
    shape: Shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 4.dp, bottomEnd = 4.dp),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ProvideContentColorTextStyle(contentColor = colors.contentColor) {
        Surface(
            modifier = modifier
                .wrapContentWidth()
                .then(if (border != null) modifier.border(border) else modifier),
            color = colors.containerColor,
            contentColor =  colors.contentColor,
            shape = shape
        ) {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .padding(dimensionResource(R.dimen.dialog_padding)),
                horizontalAlignment = Alignment.End,
                content = content
            )
        }
    }
}

@Composable
fun DialogCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContentColor = Color.Gray,
        disabledContainerColor = Color.Green,
    ),
    elevation: CardElevation = CardDefaults.cardElevation(),
    shape: Shape = RoundedCornerShape(12.dp),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ProvideContentColorTextStyle(contentColor = colors.contentColor) {
        Surface(
            modifier = modifier
                .wrapContentWidth()
                .then(if (border != null) modifier.border(border) else modifier),
            shape = shape
        ) {
            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(dimensionResource(R.dimen.dialog_padding)),
                horizontalAlignment = Alignment.Start,
                content = content
            )
        }
    }
}


@Composable
fun ColumnScope.DialogBar(modifier: Modifier = Modifier, positive: (() -> Unit)? = null, negative: (() -> Unit)? = null, neutral: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .padding(
                vertical = dimensionResource(R.dimen.dialog_bar_padding_vertical)
            ),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        negative?.let {
            SmallButton(
                modifier = Modifier
                    .height(dimensionResource(R.dimen.dialog_bar_button_height))
                    .weight(1F),
                onClick = it,
                content = { SText(R.string.no) }
            )
        }
        neutral?.let {
            if (negative != null) {
                Spacer(Modifier.width(12.dp))
            }
            SmallOutlinedButton(
                modifier = Modifier
                    .height(dimensionResource(R.dimen.dialog_bar_button_height))
                    .weight(1F),
                onClick = it,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                content = { SText(android.R.string.cancel, maxLines = 1) }
            )
        }
        positive?.let {
            if (negative != null || neutral != null) {
                Spacer(Modifier.width(12.dp))
            }
            SmallButton(
                modifier = Modifier
                    .height(dimensionResource(R.dimen.dialog_bar_button_height))
                    .weight(1F),
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
    containerColor: Color = MaterialTheme.colorScheme.surface,
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    shadowElevation: Dp = MenuDefaults.ShadowElevation,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ProvideContentColorTextStyle(MaterialTheme.colorScheme.onSurface) {
        OriginalDropdownMenu(
            expanded,
            onDismissRequest,
            modifier,
            offset,
            scrollState,
            properties,
            shape,
            containerColor,
            tonalElevation,
            shadowElevation,
            border,
            content
        )
    }
}

@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors().copy(
        textColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    OriginalDropdownMenuItem(text, onClick, modifier, leadingIcon, trailingIcon, enabled, colors, contentPadding, interactionSource)
}

@Composable
fun NumberPicker(modifier: Modifier = Modifier, range: kotlin.ranges.IntRange, default: MutableState<Int>, callback: (Int) -> Unit) {
    val h = dimensionResource(R.dimen.numberpicker_row_height)
    val column_height = 4
    val page_count = range.last - range.first + 1
    val state = rememberPagerState(
        default.value - range.first,
        pageCount = { page_count }
    )

    default.value = (state.currentPage + range.first)

    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .width(dimensionResource(R.dimen.numberpicker_row_width))
            .height(h * column_height),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(h),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(
                Modifier
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.outline)
            )
            Spacer(
                Modifier
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.outline)
            )
        }

        VerticalPager(
            state = state,
            pageSize = PageSize.Fixed(h),
            snapPosition = SnapPosition.Center,
            beyondViewportPageCount = 6,
            modifier = Modifier.height(h * column_height),
            contentPadding = PaddingValues(vertical = h * 4)
        ) { page ->
            Row(
                Modifier
                    .height(h)
                    .combinedClickable(
                        onClick = {
                            scope.launch { state.scrollToPage(page) }
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = { Text("${range.first + page}") }
            )
        }
    }
}

@Composable
fun <T> MagicInputInner(
    modifier: Modifier = Modifier,
    value: MutableState<T>,
    contentPadding: PaddingValues = PaddingValues(vertical = 0.dp),
    prefix: @Composable (() -> Unit)? = null,
    background_icon: Int? = null,
    content: @Composable (Modifier, MutableState<Boolean>, FocusRequester) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }
    if (expanded.value) {
        val requester = remember { FocusRequester() }

        Box(
            modifier = modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceBright,
                    shape = MagicButtonShape()
                ),
            contentAlignment = Alignment.Center
        ) {
            background_icon?.let {
                ProvideContentColorTextStyle(MaterialTheme.colorScheme.onSurface.copy(alpha = .2F)) {
                    Icon(
                        modifier = Modifier
                            .padding(contentPadding)
                            .fillMaxHeight(),
                        painter = painterResource(it),
                        contentDescription = null
                    )
                }
            }
            content(modifier, expanded, requester)
        }
        LaunchedEffect(Unit) {
            requester.requestFocus()
        }
    } else {
        ProvideContentColorTextStyle(contentColor = MaterialTheme.colorScheme.onBackground) {
            Box (
                modifier = modifier
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = MagicButtonShape())
                    .combinedClickable(
                        onClick = { expanded.value = !expanded.value }
                    )
                    .background(color = MaterialTheme.colorScheme.surfaceBright, shape = MagicButtonShape()),
                contentAlignment = Alignment.Center
            ) {
                background_icon?.let {
                    ProvideContentColorTextStyle(MaterialTheme.colorScheme.onSurface.copy(alpha = .2F)) {
                        Icon(
                            modifier = Modifier
                                .padding(contentPadding)
                                .fillMaxHeight(),
                            painter = painterResource(it),
                            contentDescription = null
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    prefix?.invoke()
                    Box(
                        modifier = Modifier
                            .padding(contentPadding)
                            .weight(1F),
                        contentAlignment = Alignment.Center,
                        content = { Text("${value.value}") }
                    )
                }
            }
        }
    }
}

@Composable
fun MagicInput(
    value: MutableState<Int>,
    minimum: Int? = null,
    maximum: Int? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    prefix: @Composable (() -> Unit)? = null,
    background_icon: Int? = null,
    callback: (Int) -> Unit
) {
    MagicInputInner(
        modifier,
        value,
        contentPadding,
        prefix = prefix,
        background_icon = background_icon
    ) { modifier, expanded, requester ->
        IntegerInput(
            value = value,
            text_align = TextAlign.Center,
            on_focus_enter = {},
            on_focus_exit = { value ->
                value?.let { callback(it) }
                expanded.value = false
            },
            contentPadding = contentPadding,
            modifier = modifier
                .background(Color.Transparent)
                .focusRequester(requester),
            minimum = minimum,
            maximum = maximum,
            prefix = prefix,
            callback = {
                callback(it)
                expanded.value = false
            }
        )
    }
}

@Composable
fun MagicInput(
    value: MutableState<Float>,
    precision: Int?,
    minimum: Float? = null,
    maximum: Float? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 2.dp),
    prefix: @Composable (() -> Unit)? = null,
    background_icon: Int? = null,
    callback: (Float) -> Unit) {
    MagicInputInner(
        modifier,
        value,
        contentPadding,
        prefix = prefix,
        background_icon = background_icon
    ) { modifier, expanded, requester ->
        FloatInput(
            value = value,
            precision = precision,
            text_align = TextAlign.Center,
            on_focus_enter = {},
            on_focus_exit = { value ->
                value?.let { callback(it) }
                expanded.value = false
            },
            contentPadding = contentPadding,
            modifier = modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .focusRequester(requester),
            minimum = minimum,
            maximum = maximum,
            prefix = prefix,
            callback = {
                callback(it)
                expanded.value = false
            }
        )
    }
}
