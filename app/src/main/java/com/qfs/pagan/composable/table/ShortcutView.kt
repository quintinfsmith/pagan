package com.qfs.pagan.composable.table

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.ui.theme.Dimensions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ShortcutView(modifier: Modifier, dispatcher: ActionTracker, scope: CoroutineScope, scroll_state: LazyListState) {
    HalfBorderBox(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RectangleShape)
            .combinedClickable(
                onClick = { dispatcher.cursor_select_column() },
                onLongClick = {
                    dispatcher.cursor_select_column(0)
                    scope.launch { scroll_state.scrollToItem(0) }
                }
            ),
        border_color = MaterialTheme.colorScheme.onSurfaceVariant,
        content = {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.padding(Dimensions.ShortcutIconPadding),
                    painter = painterResource(R.drawable.icon_shortcut),
                    contentDescription = stringResource(R.string.jump_to_section)
                )
            }
        }
    )
}