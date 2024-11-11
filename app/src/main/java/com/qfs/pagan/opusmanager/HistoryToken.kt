package com.qfs.pagan.opusmanager

enum class HistoryToken {
    // Meta
    MULTI,
    SAVE_POINT,
    // Base
    INSERT,
    INSERT_BEAT,
    INSERT_LINE,
    INSERT_LINE_PERCUSSION,
    INSERT_TREE,
    INSERT_CTL_GLOBAL,
    INSERT_CTL_CHANNEL,
    INSERT_CTL_LINE,
    MOVE_LINE,
    NEW_CHANNEL,
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
    SET_EVENT_DURATION,
    SET_PERCUSSION_EVENT,
    SET_PERCUSSION_INSTRUMENT,
    SET_PROJECT_NAME,
    UNSET_PROJECT_NAME,
    SET_TRANSPOSE,
    SET_TUNING_MAP,
    SWAP_LINES,
    UNSET,
    // Interface
    CURSOR_SELECT,
    CURSOR_SELECT_COLUMN,
    CURSOR_SELECT_LINE,
    CURSOR_SELECT_GLOBAL_CTL,
    CURSOR_SELECT_CHANNEL_CTL,
    CURSOR_SELECT_LINE_CTL,
    CURSOR_SELECT_GLOBAL_CTL_ROW,
    CURSOR_SELECT_CHANNEL_CTL_ROW,
    CURSOR_SELECT_LINE_CTL_ROW,
    CURSOR_SELECT_RANGE
}
