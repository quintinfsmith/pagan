"""Class definition of the history layer"""
from __future__ import annotations
from typing import Optional, Dict, List, Tuple, Any
from collections.abc import Callable

from .layer_base import OpusManagerBase

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

    def setup_repopulate(self, start_position: List[int]) -> None:
        '''Traverse a grouping and setup the history to recreate it for remove functions'''

        if self.history_locked:
            return

        self.open_multi()

        stack = [start_position]
        while stack:
            position = stack.pop(0)
            grouping = self.get_grouping(position)
            if grouping.is_structural():
                self.append_undoer(self.split_grouping, position, len(grouping))
                for k in range(len(grouping)):
                    next_position = position.copy()
                    next_position.append(k)
                    stack.append(next_position)
            elif grouping.is_event():
                event = list(grouping.get_events())[0]
                self.append_undoer(self.set_event, event.note, position, relative=event.relative)
            else:
                self.append_undoer(self.unset, position)

        self.close_multi()

    ## OpusManager methods

    def overwrite_beat(self, old_beat: Tuple[int, int, int], new_beat: Tuple[int, int, int]):
        old_position = [self.get_y(old_beat[0], old_beat[1]), old_beat[2]]
        old_grouping = self.channel_groupings[old_beat[0]][old_beat[1]][old_beat[2]].copy()
        self.append_undoer(self.replace_grouping, old_position, old_grouping)
        super().overwrite_beat(old_beat, new_beat)

    def link_beats(self, beat: Tuple[int, int, int], target: Tuple[int, int, int]) -> None:
        # Wrap function call in multi so any sub calls are considered together
        self.open_multi()
        self.append_undoer(self.unlink_beat, *beat)
        super().link_beats(beat, target)
        self.close_multi()

    def swap_channels(self, channel_a: int, channel_b: int) -> None:
        self.append_undoer(self.swap_channels, channel_a, channel_b)
        super().swap_channels(channel_a, channel_b)

    def new_line(self, channel=0, index=None):
        self.append_undoer(self.remove_line, channel, index)
        super().new_line(channel)

    def remove_line(self, channel: int, line_offset: Optional[int] = None) -> None:
        y = self.get_y(channel, line_offset)
        self.open_multi()
        self.append_undoer(self.new_line, channel, line_offset)
        for i in range(self.opus_beat_count):
            self.setup_repopulate([y, i])
        self.close_multi()

        super().remove_line(channel, line_offset)

    def insert_after(self, position: List[int]) -> None:
        # Else is implicitly handled by 'split_grouping'
        rposition = position.copy()
        rposition[-1] += 1
        self.append_undoer(self.remove, rposition)
        super().insert_after(position)

    def split_grouping(self, position: List[int], splits: int) -> None:
        self.setup_repopulate(position[0:2])
        super().split_grouping(position, splits)

    def remove(self, position: List[int]) -> None:
        self.setup_repopulate(position[0:2])
        super().remove(position)

    def insert_beat(self, index: int) -> None:
        self.append_undoer(self.remove_beat, index)
        super().insert_beat(index)

    def remove_beat(self, index: int) -> None:
        self.open_multi()
        self.append_undoer(self.insert_beat, index)
        y = 0
        for i in self.channel_order:
            for j in range(len(self.channel_groupings[i])):
                self.setup_repopulate([y, index])
                y += 1
        self.close_multi()
        super().remove_beat(index)

    def set_event(
            self,
            value: int,
            position: List[int],
            *,
            relative: bool = False) -> None:
        grouping = self.get_grouping(position)
        if not grouping.is_event():
            self.append_undoer(
                self.unset,
                position
            )
        else:
            original_event = list(grouping.get_events())[0]
            self.append_undoer(
                self.set_event,
                original_event.note,
                position,
                relative=original_event.relative
            )

        super().set_event(value, position, relative=relative)


    def unset(self, position: List[int]) -> None:
        grouping = self.get_grouping(position)
        if grouping.is_event():
            original_event = list(grouping.get_events())[0]
            self.append_undoer(
                self.set_event,
                original_event.note,
                position,
                relative=original_event.relative
            )
        super().unset(position)
