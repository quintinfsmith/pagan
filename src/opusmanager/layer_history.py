"""Class definition of the history layer"""
from __future__ import annotations
from typing import Optional, Dict, List, Tuple, Any
from collections.abc import Callable

from .layer_base import OpusManagerBase, BeatKey

class HistoryLayer(OpusManagerBase):
    """Layer of the OpusManager that handles Undo & Redo actions"""
    def __init__(self):
        super().__init__()
        self.history_ledger = []
        self.history_locked = False

        # monitors number of times open_mult() has been called without
        # close_multi() being called yet
        self.multi_counter = 0

    def apply_undo(self) -> None:
        """Undoes the last undo-able action."""
        if not self.history_ledger:
            return

        self.history_locked = True
        if isinstance(self.history_ledger[-1], list):
            for func, args, kwargs in self.history_ledger.pop():
                func(*args,**kwargs)
        else:
            func, args, kwargs = self.history_ledger.pop()
            func(*args,**kwargs)
        self.history_locked = False

    def append_undoer(self, func: Callable[Any], *args, **kwargs) -> None:
        """
            Add a function and its arguments to the ledger such that
            this function would undo the actions of another.
        """
        if self.history_locked:
            return

        if self.multi_counter:
            self.history_ledger[-1].append((func, args, kwargs))
        else:
            self.history_ledger.append([(func, args, kwargs)])

    def open_multi(self) -> None:
        """
            Let the Ledger know that incoming 'undo' functions all need
            to be called together until close_multi() is called.
        """
        if self.history_locked:
            return

        if not self.multi_counter:
            self.history_ledger.append([])
        self.multi_counter += 1

    def close_multi(self) -> None:
        """
            Stop bunching incoming 'undo' functions together.
        """
        if self.history_locked:
            return

        self.multi_counter -= 1

    def setup_repopulate(
            self,
            beat_key: BeatKey,
            start_position: List[int]) -> None:
        '''Traverse a grouping and setup the history to recreate it for remove functions'''

        if self.history_locked:
            return

        self.open_multi()
        channel, line_offset, beat_index = beat_key
        beat_grouping = self.channel_groupings[channel][line_offset][beat_index]
        stack = [start_position]
        while stack:
            position = stack.pop(0)

            grouping = beat_grouping
            for i in position:
                grouping = grouping[i]

            if grouping.is_structural():
                self.append_undoer(self.split_grouping, beat_key, position, len(grouping))
                for k in range(len(grouping)):
                    next_position = position.copy()
                    next_position.append(k)
                    stack.append(next_position)
            elif grouping.is_event():
                event = list(grouping.get_events())[0]
                self.append_undoer(self.set_event, event.note, beat_key, position, relative=event.relative)
            else:
                self.append_undoer(self.unset, beat_key, position)

        self.close_multi()

    ## OpusManager methods
    def set_percussion_instrument(self, line_offset: int, instrument: int) -> None:
        original_instrument = self.percussion_map.get(line_offset, self.DEFAULT_PERCUSSION)
        super().set_percussion_instrument(line_offset, instrument)

        self.append_undoer(self.set_percussion_instrument, line_offset, original_instrument)


    def overwrite_beat(self, old_beat: BeatKey, new_beat: BeatKey) -> None:
        old_grouping = self.channel_groupings[old_beat[0]][old_beat[1]][old_beat[2]].copy()
        self.append_undoer(self.replace_beat, old_beat, old_grouping)
        super().overwrite_beat(old_beat, new_beat)

    def link_beats(self, beat: BeatKey, target: BeatKey) -> None:
        # Wrap function call in multi so any sub calls are considered together
        self.open_multi()
        self.append_undoer(self.unlink_beat, beat)
        super().link_beats(beat, target)
        self.close_multi()

    def swap_channels(self, channel_a: int, channel_b: int) -> None:
        self.append_undoer(self.swap_channels, channel_a, channel_b)
        super().swap_channels(channel_a, channel_b)

    def new_line(self, channel=0, index=None):
        self.append_undoer(self.remove_line, channel, index)
        super().new_line(channel)

    def remove_line(self, channel: int, line_offset: Optional[int] = None) -> None:
        self.open_multi()
        self.append_undoer(self.new_line, channel, line_offset)
        for i in range(self.opus_beat_count):
            self.setup_repopulate([channel, line_offset, i], [])
        self.close_multi()

        super().remove_line(channel, line_offset)

    def insert_after(self, beat_key: BeatKey, position: List[int]) -> None:
        # Else is implicitly handled by 'split_grouping'
        rposition = position.copy()
        rposition[-1] += 1
        self.append_undoer(self.remove, beat_key, rposition)
        super().insert_after(beat_key, position)

    def split_grouping(self, beat_key: BeatKey, position: List[int], splits: int) -> None:
        self.setup_repopulate(beat_key, [])
        super().split_grouping(beat_key, position, splits)

    def remove(self, beat_key: BeatKey, position: List[int]) -> None:
        self.setup_repopulate(beat_key, position)
        super().remove(beat_key, position)

    def insert_beat(self, index: int) -> None:
        self.append_undoer(self.remove_beat, index)
        super().insert_beat(index)

    def remove_beat(self, index: int) -> None:
        self.open_multi()
        self.append_undoer(self.insert_beat, index)

        # TODO: This could be more precise
        for i, channel in enumerate(self.channel_groupings):
            for j, line in enumerate(channel):
                for k, beat in enumerate(line):
                    if k < index:
                        continue
                    self.setup_repopulate((i, j, k), [])

        self.close_multi()
        super().remove_beat(index)

    def set_event(
            self,
            value: int,
            beat_key: BeatKey,
            position: List[int],
            *,
            relative: bool = False) -> None:

        grouping = self.get_grouping(beat_key, position)
        if not grouping.is_event():
            self.append_undoer(
                self.unset,
                beat_key,
                position
            )
        else:
            original_event = list(grouping.get_events())[0]
            self.append_undoer(
                self.set_event,
                original_event.note,
                beat_key,
                position,
                relative=original_event.relative
            )

        super().set_event(value, beat_key, position, relative=relative)


    def unset(self, beat_key: BeatKey, position: List[int]) -> None:
        grouping = self.get_grouping(beat_key, position)
        if grouping.is_event():
            original_event = list(grouping.get_events())[0]
            self.append_undoer(
                self.set_event,
                original_event.note,
                beat_key,
                position,
                relative=original_event.relative
            )
        super().unset(beat_key, position)
