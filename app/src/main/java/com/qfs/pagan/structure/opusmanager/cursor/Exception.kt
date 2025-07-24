package com.qfs.pagan.structure.opusmanager.cursor

import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel

class InvalidModeException(actual: CursorMode, expected: CursorMode): Exception("Incorrect Cursor Mode. expected $expected but got $actual")
class InvalidControlLevelException(actual: CtlLineLevel?, expected: CtlLineLevel?): Exception("Incorrect Control Level. Expected: $expected but got $actual")
class InvalidCursorState: Exception()
class IncorrectCursorMode(val current_mode: CursorMode, vararg required_mode: CursorMode): Exception("Incorrect Cursor Mode. Found: $current_mode. Requires: $required_mode.")
