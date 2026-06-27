/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.qfs.pagan.viewmodel.ViewModelPagan

/**
 * A structure to keep track of Dialogs opened in the UI.
 * @param parent A reference to the Dialog from which this one was opened
 * @param alignment Where to align the dialog
 * @param dialog content of the current dialog
 * @param level a key to make sure no sibling/duplicate dialogs are opened (eg. via a quick double tap)
 */
class DialogChain(
    var parent: DialogChain? = null,
    val alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    val dialog: @Composable (ColumnScope.() -> Unit),
    val level: Int = 0
)