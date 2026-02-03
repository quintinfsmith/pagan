package com.qfs.pagan.composable

import android.view.ViewTreeObserver
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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.selectAll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
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
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.composable.button.SmallButton
import com.qfs.pagan.composable.button.SmallOutlinedButton
import com.qfs.pagan.enumerate
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shadows
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.ui.theme.Typography
import kotlinx.coroutines.launch
import kotlin.math.abs
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
    Text(
        stringResource(string_id),
        modifier,
        color,
        autoSize,
        fontSize,
        fontStyle,
        fontWeight,
        fontFamily,
        letterSpacing,
        textDecoration,
        textAlign,
        lineHeight,
        overflow,
        softWrap,
        maxLines,
        minLines,
        onTextLayout,
        style
    )
}

@Composable
fun HexInput(
    value: MutableState<Int>,
    maximum: Int? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    text_align: TextAlign = TextAlign.End,
    prefix: @Composable (() -> Unit)? = null,
    label: (@Composable TextFieldLabelScope.() -> Unit)? = null,
    on_focus_enter: (() -> Unit)? = null,
    on_focus_exit: ((Int?) -> Unit)? = null,
    callback: (Int) -> Unit
) {
    val minimum = 0x00
    val hex_state = remember { mutableStateOf(value.value.toHexString()) }
    NumberInput(
        hex_state,
        modifier,
        contentPadding,
        text_align,
        prefix,
        label,
        on_focus_enter,
        { on_focus_exit?.invoke(value.value) },
        object : InputTransformation {
            override fun TextFieldBuffer.transformInput() {
                val working_string = this.toString()

                var converted_value = try {
                    if (working_string.isEmpty()) {
                        0
                    } else {
                        this.toString().toInt(16)
                    }
                } catch (_: Exception) {
                    this.revertAllChanges()
                    return
                }

                converted_value = max(minimum, converted_value)
                maximum?.let {
                    converted_value = min(it, converted_value)
                }

                value.value = converted_value
            }
        },
        {
            val working_string = this.toString()

            var converted_value = try {
                if (working_string.isEmpty()) {
                    0
                } else {
                    this.toString().toInt(16)
                }
            } catch (_: Exception) {
                return@NumberInput
            }

            if (minimum > converted_value) {
                converted_value = max(minimum, converted_value)
                this.delete(0, this.length)
                this.append(converted_value.toString())
            }

            maximum?.let {
                if (it < converted_value) {
                    converted_value = min(it, converted_value)
                    this.delete(0, this.length)
                    this.append(converted_value.toString())
                }
            }
        },

        callback = {
            callback(value.value)
        }
    )
}

@Composable
fun IntegerInput(
    value: MutableState<Int>,
    minimum: Int? = null,
    maximum: Int? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    text_align: TextAlign = TextAlign.End,
    prefix: @Composable (() -> Unit)? = null,
    label: (@Composable TextFieldLabelScope.() -> Unit)? = null,
    on_focus_enter: (() -> Unit)? = null,
    on_focus_exit: ((Int?) -> Unit)? = null,
    callback: (Int) -> Unit
) {
    NumberInput(
        value,
        modifier,
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
        {
            val working_string = this.toString()
            if (working_string == "-" && minimum != null && minimum < 0) return@NumberInput

            var converted_value = try {
                if (working_string.isEmpty()) {
                    0
                } else {
                    this.toString().toInt()
                }
            } catch (_: Exception) {
                return@NumberInput
            }

            minimum?.let {
                if (it > converted_value) {
                    converted_value = max(it, converted_value)
                    this.delete(0, this.length)
                    this.append(converted_value.toString())
                }
            }
            maximum?.let {
                if (it < converted_value) {
                    converted_value = min(it, converted_value)
                    this.delete(0, this.length)
                    this.append(converted_value.toString())
                }
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
    contentPadding: PaddingValues = PaddingValues(12.dp),
    text_align: TextAlign = TextAlign.End,
    prefix: @Composable (() -> Unit)? = null,
    label: (@Composable TextFieldLabelScope.() -> Unit)? = null,
    on_focus_enter: (() -> Unit)? = null,
    on_focus_exit: ((Float?) -> Unit)? = null,
    callback: (Float) -> Unit
) {
    NumberInput(
        value,
        modifier,
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
        {
            val working_string = this.toString()
            if (working_string == "-" && minimum != null && minimum < 0) return@NumberInput

            var converted_value = try {
                if (working_string.isEmpty()) {
                    0F
                } else {
                    this.toString().toFloat()
                }
            } catch (_: Exception) {
                return@NumberInput
            }

            minimum?.let {
                if (it > converted_value) {
                    converted_value = max(it, converted_value)
                    this.delete(0, this.length)
                    this.append(converted_value.toString())
                }
            }
            maximum?.let {
                if (it < converted_value) {
                    converted_value = min(it, converted_value)
                    this.delete(0, this.length)
                    this.append(converted_value.toString())
                }
            }
        },
        callback
    )
}

@Composable
fun <T> NumberInput(
    value: MutableState<T>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    text_align: TextAlign = TextAlign.End,
    prefix: @Composable (() -> Unit)? = null,
    label: (@Composable TextFieldLabelScope.() -> Unit)? = null,
    on_focus_enter: (() -> Unit)? = null,
    on_focus_exit: ((T?) -> Unit)? = null,
    input_transformation: InputTransformation,
    output_transformation: OutputTransformation,
    callback: (T) -> Unit
) {
    val trigger_select_all = remember { mutableStateOf<Boolean?>(null) }

    val state = rememberTextFieldState(value.value.toString())
    // Prevent weird focusing behavior causing on_focus_exit to be called without any initial focus
    val was_focused = remember { mutableStateOf(false) }
    val focus_change_callback = { focus_state: FocusState ->
        if (focus_state.isFocused) {
            trigger_select_all.value = trigger_select_all.value?.let { it -> !it } ?: true
            was_focused.value = true
            on_focus_enter?.let { it() }
        } else if (was_focused.value) {
            was_focused.value = false
            on_focus_exit?.let { it(value.value) }
        }
    }

    OutlinedTextField(
        state = state,
        label = label,
        contentPadding = contentPadding,
        textStyle = Typography.TextField.copy(textAlign = text_align),
        prefix = prefix,
        modifier = modifier
            .onKeyEvent { event ->
                when (event.key) {
                    Key.Enter -> {
                        callback(value.value)
                        false
                    }

                    else -> true
                }
            }
            .heightIn(1.dp)
            .onFocusChanged { focus_change_callback(it) },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number
        ),
        inputTransformation = input_transformation,
        outputTransformation = output_transformation,
        lineLimits = TextFieldLineLimits.SingleLine,
        onKeyboardAction = { action ->
            callback(value.value)
            action()
        }
    )

    trigger_select_all.value?.let {
        LaunchedEffect(trigger_select_all.value) {
            state.edit { selectAll() }
        }
    }
}


@Composable
fun TextInput(
    modifier: Modifier = Modifier,
    input: MutableState<String>,
    textAlign: TextAlign = TextAlign.End,
    label: (@Composable TextFieldLabelScope.() -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    on_focus_enter: (() -> Unit)? = null,
    on_focus_exit: ((String) -> Unit)? = null,
    shape: Shape = Shapes.Container,
    callback: (String) -> Unit
) {
    val state = rememberTextFieldState(input.value)
    val trigger_select_all = remember { mutableStateOf<Boolean?>(null) }

    val was_focused = remember { mutableStateOf(false) }
    val focus_change_callback = { focus_state: FocusState ->
        if (focus_state.isFocused) {
            trigger_select_all.value = trigger_select_all.value?.let { it -> !it } ?: true
            was_focused.value = true
            on_focus_enter?.let { it() }
        } else if (was_focused.value) {
            was_focused.value = false
            on_focus_exit?.let { it(input.value) }
        }
    }
    OutlinedTextField(
        state = state,
        lineLimits = lineLimits,
        label = label,
        placeholder = placeholder,
        textStyle = Typography.TextField.copy(textAlign = textAlign),
        modifier = modifier
            .onKeyEvent { event ->
                when (event.key) {
                    Key.Enter -> {
                        callback(input.value)
                        false
                    }

                    else -> true
                }
            }
            .onFocusChanged { focus_change_callback(it) },
        shape = shape,
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
                                            SText(sort_options[x].first)
                                        }
                                    } else {
                                        SText(sort_options[x].first)
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
                .clip(RoundedCornerShape(8.dp)),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(4.dp)
                    .verticalScroll(scroll_state)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                sorted_menu.forEachIndexed { i, (item, label_content) ->
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
                            modifier = Modifier
                                .then(
                                    if (default_index == i) {
                                        Modifier.background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(8.dp))
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
    shape: Shape = Shapes.Drawer,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ProvideContentColorTextStyle(contentColor = colors.contentColor) {
        Surface(
            modifier = modifier
                .dropShadow(
                    shape,
                    Shadows.ContextMenu
                )
                .wrapContentWidth()
                .then(if (border != null) modifier.border(border) else modifier),
            color = colors.containerColor,
            contentColor =  colors.contentColor,
            shape = shape
        ) {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .padding(Dimensions.DialogPadding),
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
                .then(if (border != null) modifier.border(border) else Modifier),
            shape = shape
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(Dimensions.DialogPadding),
                horizontalAlignment = Alignment.Start,
                content = content
            )
        }
    }
}


@Composable
fun ColumnScope.DialogBar(
    modifier: Modifier = Modifier,
    positive: (() -> Unit)? = null,
    negative: (() -> Unit)? = null,
    neutral: (() -> Unit)? = null,
    neutral_label: Int = android.R.string.cancel,
    negative_label: Int = R.string.no,
    positive_label: Int = android.R.string.ok,

) {
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
                content = { SText(negative_label) }
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
                content = { SText(neutral_label, maxLines = 1) }
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
                content = { SText(positive_label) }
            )
        }
    }
}

// Every time i set "steps" to be the *actual* number of discrete values I want - 2 I get a stress headache. So I'm not doing that.
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
    StupidSlider(
        value,
        onValueChange,
        modifier,
        enabled,
        valueRange,
        steps - 2,
        onValueChangeFinished,
        colors,
        interactionSource
    )
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
    content: @Composable (ColumnScope.() -> Unit),
) {
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

@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors().copy(
        textColor = MaterialTheme.colorScheme.onSurface,
    ),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    OriginalDropdownMenuItem(text, onClick, modifier, leadingIcon, trailingIcon, enabled, colors, contentPadding, interactionSource)
}

@Composable
fun NumberPicker(modifier: Modifier = Modifier, range: kotlin.ranges.IntRange, default: MutableState<Int>) {
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
                    shape = RoundedCornerShape(12.dp)
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

@Composable
fun FocusableInput(
    modifier: Modifier = Modifier,
    value: Int,
    minimum: Int? = null,
    maximum: Int? = null,
    contentPadding: PaddingValues = PaddingValues(vertical = 0.dp),
    background_icon: Int? = null,
    callback: (Int) -> Unit = {}
) {
    // Clear focus if keyboard was hidden by user
    val expanded = remember { mutableStateOf(false) }
    val label_value = remember { mutableIntStateOf(value) }
    val backup_value = remember { mutableIntStateOf(value) }
    val requester = remember { FocusRequester() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            modifier = modifier,
            onClick = { expanded.value = !expanded.value },
            contentPadding = if (expanded.value) {
                contentPadding
            } else {
                PaddingValues(2.dp)
            },
            content = {
                if (expanded.value) {
                    Icon(
                        modifier = Modifier
                            .then (
                                if (expanded.value) {
                                    Modifier
                                        .width(32.dp)
                                        .height(32.dp)
                                } else {
                                    Modifier
                                }
                            ),
                        painter = painterResource(R.drawable.icon_pan),
                        contentDescription = null
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        background_icon?.let {
                            Icon(
                                painterResource(it),
                                contentDescription = null
                            )
                        }
                        Text("${label_value.value}")
                    }
                }
            }
        )

        if (expanded.value) {
            IntegerInput(
                modifier = modifier
                    .focusRequester(requester),
                value = label_value,
                text_align = TextAlign.Center,
                on_focus_enter = {},
                on_focus_exit = { value ->
                    expanded.value = false
                    label_value.intValue = backup_value.intValue
                },
                contentPadding = contentPadding,
                minimum = minimum,
                maximum = maximum,
                callback = {
                    label_value.intValue = it
                    backup_value.intValue = it
                    callback(it)
                    expanded.value = false
                }
            )
            LaunchedEffect(Unit) {
                requester.requestFocus()
            }
        }

    }
}


@Composable
fun MagicInput(
    value: Int,
    minimum: Int? = null,
    maximum: Int? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    prefix: @Composable (() -> Unit)? = null,
    background_icon: Int? = null,
    content: @Composable (() -> Unit) = {},
    callback: (Int) -> Unit
) {
    
    
    
    FocusableInput(
        modifier,
        value,
        minimum,
        maximum,
        contentPadding = contentPadding,
        background_icon = background_icon,
        callback = callback
    )
}

@Composable
fun MagicInput(
    value: Float,
    precision: Int?,
    minimum: Float? = null,
    maximum: Float? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 2.dp),
    prefix: @Composable (() -> Unit)? = null,
    background_icon: Int? = null,
    callback: (Float) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }
    val label_value = remember { mutableFloatStateOf(value) }
    val backup_value = remember { mutableFloatStateOf(value) }

    if (expanded.value) {
        Dialog(
            onDismissRequest = {
                expanded.value = false
                label_value.value = backup_value.value
            }
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val requester = remember { FocusRequester() }
                FloatInput(
                    value = label_value,
                    precision = precision,
                    text_align = TextAlign.Center,
                    on_focus_enter = {},
                    on_focus_exit = { value -> expanded.value = false },
                    contentPadding = contentPadding,
                    modifier = modifier
                        .padding(12.dp)
                        .focusRequester(requester),
                    minimum = minimum,
                    maximum = maximum,
                    prefix = prefix,
                    callback = {
                        backup_value.value = it
                        label_value.value = it
                        callback(it)
                        expanded.value = false
                    }
                )

                LaunchedEffect(Unit) {
                    requester.requestFocus()
                }
            }
        }
    }

    Button(
        onClick = { expanded.value = !expanded.value },
        content = { Text("${label_value.value}") }
    )
}

@Composable
fun SettingsBoxWrapper(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(12.dp))
            .background(
                MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center,
        content = {
            ProvideContentColorTextStyle(contentColor = MaterialTheme.colorScheme.onSurface) {
                content()
            }
        }
    )
}

@Composable
fun SettingsColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
    content: @Composable ColumnScope.() -> Unit
) {
    SettingsBoxWrapper(modifier) {
        Column(
            modifier.padding(contentPadding),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            content = { content() }
        )
    }
}

@Composable
fun SettingsBox(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    contentAlignment: Alignment = Alignment.TopCenter,
    content: @Composable BoxScope.() -> Unit
) {
    SettingsBoxWrapper {
        Box(
            modifier.padding(contentPadding),
            contentAlignment = contentAlignment,
            content = { content() }
        )
    }
}

@Composable
fun SettingsRow(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit
) {
    SettingsBoxWrapper {
        Row(
            modifier.padding(contentPadding),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            content = { content() }
        )
    }
}

@Composable
fun MenuPadder() {
    Spacer(
        Modifier
            .width(Dimensions.SoundFontMenuPadding)
            .height(Dimensions.SoundFontMenuPadding)
    )
}

// https://stackoverflow.com/questions/68847559/how-can-i-detect-keyboard-opening-and-closing-in-jetpack-compose
@Composable
fun keyboardAsState(): MutableState<Boolean> {
    val keyboardState = remember { mutableStateOf(false) }
    val view = LocalView.current
    val viewTreeObserver = view.viewTreeObserver
    DisposableEffect(viewTreeObserver) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            keyboardState.value = ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.ime()) ?: true
        }
        viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose { viewTreeObserver.removeOnGlobalLayoutListener(listener) }
    }
    return keyboardState
}
