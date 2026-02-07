/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.structure.opusmanager.base

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType

class UnknownSaveVersion(v: Int): Exception("Unknown Save Version $v")
class BadBeatKey(beat_key: BeatKey) : Exception("BeatKey $beat_key doesn't exist")
class NonEventConversion(beat_key: BeatKey, position: List<Int>) : Exception("Attempting to convert non-event @ $beat_key:$position")
class PercussionEventSet : Exception("Attempting to set percussion event on non-percussion channel")
class InvalidOverwriteCall : Exception()
class InvalidPercussionLineException: Exception("Attempting to add a non-percussion line to the percussion channel")
class MixedInstrumentException(first_key: BeatKey, second_key: BeatKey) : Exception("Can't mix percussion with non-percussion instruments here (${first_key.channel} & ${second_key.channel})")
class BlockedTreeException(beat_key: BeatKey, position: List<Int>, blocker_key: BeatKey, blocker_position: List<Int>): Exception("$beat_key | $position is blocked by event @ $blocker_key $blocker_position")
class PercussionBankException : Exception("Can't set percussion channel bank. It is always 128")
class BadInsertPosition : Exception("Can't insert tree at top level")
class RemovingLastBeatException : Exception("OpusManager requires at least 1 beat")
class IncompatibleChannelException(channel_old: Int, channel_new: Int) : Exception("Can't move lines into or out of the percussion channel ($channel_old -> $channel_new)")
class RangeOverflow(from_key: BeatKey, to_key: BeatKey, startkey: BeatKey) : Exception("Range($from_key .. $to_key) @ $startkey overflows")
class EventlessTreeException : Exception("Tree requires event for operation")
class InvalidMergeException : Exception()
class RemovingRootException : Exception()
class InvalidChannel(channel: Int) : Exception("Channel $channel doesn't exist")
class NoteOutOfRange(var n: Int) : Exception("Attempting to use unsupported note $n")
class InvalidLineException: Exception("Attempting to add a percussion line to the non-percussion channel")
class EmptyPath : Exception("Path Required but not given")
class EmptyJSONException: Exception("JSON object was NULL")
class PercussionChannelRequired(channel: Int) : Exception("Channel $channel is not a Percussion Channel")
class UnhandledLineType(line: OpusLineAbstract<*>): Exception("Unhandled Line Implementation: ${line::class.java.name}")
class TrivialBranchException(beat_key: BeatKey, position: List<Int>): Exception("Trivial Branch found at @ $beat_key, $position")
class GlobalEffectRowNotVisible(type: EffectType): Exception("Global $type Row is either disabled or invisible.")
class ChannelEffectRowNotVisible(type: EffectType, channel: Int): Exception("Channel $channel's $type row is either disabled or invisible.")
class LineEffectRowNotVisible(type: EffectType, channel: Int, line_offset: Int): Exception("Line ($channel|$line_offset)'s $type row is either disabled or invisible.")

/** Used to indicate to higher layers that the action was blocked, doesn't need more than a message since the actual handling is done with callbacks in this layer */
class BlockedActionException(msg: String? = null) : Exception(msg)

