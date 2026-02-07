/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.structure.opusmanager.history

enum class HistoryToken {
    // Meta
    MULTI,
    // Base
    INSERT,
    INSERT_BEAT,
    INSERT_LINE,
    INSERT_LINE_PERCUSSION,
    INSERT_CTL_GLOBAL,
    INSERT_CTL_CHANNEL,
    INSERT_CTL_LINE,
    NEW_CHANNEL,
    MOVE_CHANNEL,
    REMOVE,
    REMOVE_CTL_GLOBAL,
    REMOVE_CTL_CHANNEL,
    REMOVE_CTL_LINE,
    REMOVE_BEATS,
    REMOVE_CHANNEL,
    REMOVE_LINE,
    REPLACE_TREE,
    REPLACE_GLOBAL_CTL_TREE,
    REPLACE_CHANNEL_CTL_TREE,
    REPLACE_LINE_CTL_TREE,
    SET_GLOBAL_CTL_INITIAL_EVENT,
    SET_CHANNEL_CTL_INITIAL_EVENT,
    SET_LINE_CTL_INITIAL_EVENT,
    SET_CHANNEL_INSTRUMENT,
    SET_EVENT,
    SET_PERCUSSION_EVENT,
    SET_PERCUSSION_INSTRUMENT,
    SET_PROJECT_NAME,
    SET_PROJECT_NOTES,
    SPLIT_TREE,
    UNSET_PROJECT_NAME,
    UNSET_PROJECT_NOTES,
    SET_TRANSPOSE,
    SET_TUNING_MAP,
    SWAP_LINES,
    UNSET,
    MUTE_CHANNEL,
    MUTE_LINE,
    UNMUTE_CHANNEL,
    UNMUTE_LINE,

    SET_LINE_COLOR,
    SET_CHANNEL_COLOR,
    UNSET_LINE_COLOR,
    UNSET_CHANNEL_COLOR,

    TAG_SECTION,
    UNTAG_SECTION,

    // Interface
    CURSOR_SELECT,
    CURSOR_SELECT_CHANNEL,
    CURSOR_SELECT_COLUMN,
    CURSOR_SELECT_LINE,
    CURSOR_SELECT_GLOBAL_CTL,
    CURSOR_SELECT_CHANNEL_CTL,
    CURSOR_SELECT_LINE_CTL,
    CURSOR_SELECT_GLOBAL_CTL_ROW,
    CURSOR_SELECT_CHANNEL_CTL_ROW,
    CURSOR_SELECT_LINE_CTL_ROW,
    CURSOR_SELECT_RANGE,
    // CTLs & Visibility
    SET_GLOBAL_CTL_VISIBILITY,
    SET_CHANNEL_CTL_VISIBILITY,
    SET_LINE_CTL_VISIBILITY,
    REMOVE_LINE_CONTROLLER,
    REMOVE_GLOBAL_CONTROLLER,
    REMOVE_CHANNEL_CONTROLLER,
    NEW_LINE_CONTROLLER,
    NEW_GLOBAL_CONTROLLER,
    NEW_CHANNEL_CONTROLLER
}
