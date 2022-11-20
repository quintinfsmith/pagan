"""Class definition of the history layer"""
from __future__ import annotations
from typing import Optional, Dict, List, Tuple, Any
from collections.abc import Callable

from .miditree import MIDITreeEvent
from .layer_links import LinksLayer, BeatKey

class HistoryLayer(LinksLayer):
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

        for func, args, kwargs in self.history_ledger.pop():
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
        '''Traverse a tree and setup the history to recreate it for remove functions'''

        if self.history_locked:
            return

        self.open_multi()
        channel, line_offset, beat_index = beat_key

        beat_tree = self.channel_trees[channel][line_offset][beat_index]

        stack = []
        if not start_position:
            # start_position can be passed as empty, but we still need to
            # specify which subtrees are being rebuilt
            for i in range(len(beat_tree)):
                stack.append([i])
            self.append_undoer(self.split_tree, beat_key, [], len(beat_tree))
        else:
            stack.append(start_position)


        while stack:
            position = stack.pop(0)

            tree = beat_tree
            for i in position:
                tree = tree[i]

            if not tree.is_leaf():
                self.append_undoer(self.split_tree, beat_key, position, len(tree))
                for k in range(len(tree)):
                    next_position = position.copy()
                    next_position.append(k)
                    stack.append(next_position)
            elif tree.is_event():
                event = tree.get_event()
                if channel != 9:
                    self.append_undoer(
                        self.set_event,
                        beat_key,
                        position,
                        event
                    )
                else:
                    self.append_undoer(self.set_percussion_event, beat_key, position)
            else:
                self.append_undoer(self.unset, beat_key, position)

        self.close_multi()

    ## OpusManager methods
    def set_percussion_instrument(self, line_offset: int, instrument: int) -> None:
        original_instrument = self.percussion_map.get(line_offset, self.DEFAULT_PERCUSSION)
        super().set_percussion_instrument(line_offset, instrument)

        self.append_undoer(self.set_percussion_instrument, line_offset, original_instrument)

    def overwrite_beat(self, old_beat: BeatKey, new_beat: BeatKey) -> None:
        old_tree = self.channel_trees[old_beat[0]][old_beat[1]][old_beat[2]].copy()
        self.append_undoer(self.replace_beat, old_beat, old_tree)
        super().overwrite_beat(old_beat, new_beat)

    def swap_channels(self, channel_a: int, channel_b: int) -> None:
        self.append_undoer(self.swap_channels, channel_a, channel_b)
        super().swap_channels(channel_a, channel_b)

    def new_line(self, channel=0, index=None):
        self.append_undoer(self.remove_line, channel, index)
        super().new_line(channel, index)

    def remove_line(self, channel: int, line_offset: Optional[int] = None) -> None:
        self.open_multi()
        self.append_undoer(self.new_line, channel, line_offset)
        for i in range(self.opus_beat_count):
            self.setup_repopulate((channel, line_offset, i), [])
        self.close_multi()

        super().remove_line(channel, line_offset)

    def insert_after(self, beat_key: BeatKey, position: List[int]) -> None:
        if position:
            # Else is implicitly handled by 'split_tree'
            rposition = position.copy()
            rposition[-1] += 1
            self.append_undoer(self.remove, beat_key, rposition)
        super().insert_after(beat_key, position)

    def split_tree(self, beat_key: BeatKey, position: List[int], splits: int) -> None:
        self.setup_repopulate(beat_key, position[0:-1])
        super().split_tree(beat_key, position, splits)

    def remove(self, beat_key: BeatKey, position: List[int]) -> None:
        self.setup_repopulate(beat_key, position[0:-1])
        super().remove(beat_key, position)

    def insert_beat(self, index: int) -> None:
        self.append_undoer(self.remove_beat, index)
        super().insert_beat(index)

    def remove_beat(self, index: int) -> None:
        self.open_multi()
        self.append_undoer(self.insert_beat, index)

        # TODO: This could be more precise
        for i, channel in enumerate(self.channel_trees):
            for j, line in enumerate(channel):
                for k, beat in enumerate(line):
                    if k < index:
                        continue
                    self.setup_repopulate((i, j, k), [])

        self.close_multi()
        super().remove_beat(index)

    def set_event(
            self,
            beat_key: BeatKey,
            position: List[int],
            event: MIDITreeEvent) -> None:

        tree = self.get_tree(beat_key, position)
        if not tree.is_event():
            self.append_undoer(
                self.unset,
                beat_key,
                position
            )
        else:
            original_event = tree.get_event()
            self.append_undoer(
                self.set_event,
                beat_key,
                position,
                original_event
            )

        super().set_event(beat_key, position, event)


    def unset(self, beat_key: BeatKey, position: List[int]) -> None:
        tree = self.get_tree(beat_key, position)
        if tree.is_event():
            original_event = tree.get_event()
            self.append_undoer(
                self.set_event,
                beat_key,
                position,
                original_event
            )
        super().unset(beat_key, position)
