"""Cursor Layer of the OpusManager and the classes required to make it work"""
from __future__ import annotations
from typing import List, Optional, Tuple, Union, Any
from collections.abc import Callable

from .layer_flag import FlagLayer

class ReadyEvent:
    """Temporary placeholder for note events as they are being created"""
    def __init__(self, initial_value: int, *, relative: bool = False):
        self.value: int = initial_value
        self.relative: bool = relative
        self.flag_changed: bool = True

class Cursor:
    """Points to a position in an OpusManager."""
    def __init__(self, opus_manager: CursorLayer):
        self.opus_manager: CursorLayer = opus_manager
        self.y: int = 0
        self.x: int = 0
        self.position: List[int] = [0]

    def set(self, y: int, x: int, *position: int) -> None:
        """Set all the cursor's values at once"""
        self.y = y
        self.x = x

        # Position *Needs* to have at least 1 item
        if position:
            self.position = list(position)
        else:
            self.position = [0]

    def move_left(self) -> None:
        """Point the cursor to the previous leaf"""
        channel, line, beat = self.get_triplet()
        working_grouping = self.opus_manager.channel_groupings[channel][line][beat]
        # First, traverse the position to get the currently active grouping
        for j in self.position:
            working_grouping = working_grouping[j]

        # Then move the cursor up until it can move left
        while self.position:
            if self.position[-1] == 0:
                self.position.pop()
                working_grouping = working_grouping.parent
            else:
                self.position[-1] -= 1
                break

        if not self.position:
            # Couldn't find any available leaf to the left of the previous active grouping
            # within the active *beat* so move over a beat if possible
            if self.x > 0:
                self.x -= 1
                self.position = [len(working_grouping) - 1]
            else:
                self.position = [0]

        # Move to the leaf
        self.settle()

    def move_right(self) -> None:
        """Point the cursor to the next leaf"""
        channel, line, beat = self.get_triplet()
        working_grouping = self.opus_manager.channel_groupings[channel][line][beat]
        # First, traverse the position to get the currently active grouping
        for j in self.position:
            working_grouping = working_grouping[j]

        # Then move the cursor up until it can move right
        while self.position:
            if len(working_grouping.parent) - 1 == self.position[-1]:
                self.position.pop()
                working_grouping = working_grouping.parent
            elif len(working_grouping.parent) - 1 > self.position[-1]:
                self.position[-1] += 1
                break

        if not self.position:
            # Couldn't find any available leaf to the right of the previous active grouping
            # within the active *beat* so move over a beat if possible
            if self.x < self.opus_manager.opus_beat_count - 1:
                self.x += 1
                self.position = [0]
            else:
                self.position = [len(working_grouping) - 1]

        # Move to the leaf
        self.settle()

    def move_up(self) -> None:
        """Point the cursor to the previous line"""
        if self.y > 0:
            self.y -= 1

        self.settle()

    def move_down(self) -> None:
        """Point the cursor to the next line"""
        line_count = self.opus_manager.line_count
        if self.y < line_count - 1:
            self.y += 1

        self.settle()

    def move_climb(self) -> None:
        """Point to the parent of the currently active Grouping"""
        if len(self.position) > 1:
            self.position.pop()

    def move_dive(self, index: int) -> None:
        """Point the cursor to a child of the currently active Grouping"""
        beat = self.opus_manager.get_grouping([self.y, self.x])
        working_grouping = beat
        for j in self.position:
            working_grouping = working_grouping[j]

        if working_grouping.is_structural() and index < len(working_grouping):
            self.position.append(index)

    def to_list(self) -> List[int]:
        """Convert the cursor to a coordinate"""
        return [self.y, self.x, *self.position]

    def set_y(self, new_y: int) -> None:
        """Set which line the cursor points to"""
        self.y = new_y

    def set_x(self, new_x: int) -> None:
        """Set which beat the cursor points to"""
        beat_count = self.opus_manager.opus_beat_count
        if new_x < 0:
            new_x = beat_count - new_x
        else:
            new_x = min(new_x, beat_count - 1)
        self.x = new_x

    def settle(self, right_align: bool = False) -> None:
        """ Sink the cursor as deep as possible. (until it finds a leaf) """
        self.x = max(0, min(self.x, self.opus_manager.opus_beat_count - 1))

        channel, line, beat =  self.get_triplet()
        ## First get the beat ...
        working_grouping = self.opus_manager.channel_groupings[channel][line][beat]

        if not self.position:
            self.position = [0]

        ## The get the current working_grouping ...
        index = 0
        for j in self.position:
            if not working_grouping.is_structural() or len(working_grouping) <= j:
                break
            working_grouping = working_grouping[j]
            index += 1

        self.position = self.position[0:index]

        ## ... Then find the leaf, if not already found
        if not right_align:
            while working_grouping.is_structural():
                self.position.append(0)
                working_grouping = working_grouping[0]
        else:
            while working_grouping.is_structural():
                self.position.append(len(working_grouping) - 1)
                working_grouping = working_grouping[-1]

    def get_channel_index(self) -> (int, int):
        """
            get the channel and index from y
        """
        return self.opus_manager.get_channel_index(self.y)

    def get_triplet(self) -> Tuple[int, int, int]:
        """Get the active beat's channel, line and x position rather than x/y"""
        channel, index = self.get_channel_index()
        return (channel, index, self.x)

class CursorLayer(FlagLayer):
    """Adds Cursor functionality to OpusManager."""
    def __init__(self):
        super().__init__()
        self.cursor = Cursor(self)
        self.register = None

    ## Layer-Specific methods
    def cursor_right(self) -> None:
        """Wrapper function"""
        self.cursor.move_right()

    def cursor_left(self) -> None:
        """Wrapper function"""
        self.cursor.move_left()

    def cursor_up(self) -> None:
        """Wrapper function"""
        self.cursor.move_up()

    def cursor_down(self) -> None:
        """Wrapper function"""
        self.cursor.move_down()

    def new_line_at_cursor(self) -> None:
        """Insert a line at the channel pointed to by the cursor"""
        channel, _, _ = self.cursor.get_triplet()
        self.new_line(channel)

    def remove_line_at_cursor(self) -> None:
        """Remove the line from the channel pointed to by the cursor"""
        channel, _, _ = self.cursor.get_triplet()
        channel, index, _ = self.cursor.get_triplet()
        self.remove_line(channel, index)

    def remove_beat_at_cursor(self) -> None:
        """Remove the beat from all channels pointed to by the cursor"""
        self.remove_beat(self.cursor.x)
        self.cursor.move_left()

    def split_grouping_at_cursor(self) -> None:
        """Divide the grouping pointed to by the cursor into 2"""
        self.split_grouping(self.cursor.to_list(), 2)
        self.cursor.settle()

    def unset_at_cursor(self) -> None:
        """
            Remove the event pointed to by the cursor.
            Leaves the grouping in tact.
        """
        self.unset(self.cursor.to_list())

    def remove_grouping_at_cursor(self) -> None:
        """
            Remove the grouping pointed to by the cursor from its parent.
            Reduces the size of the parent.
        """
        self.remove(self.cursor.to_list())
        self.cursor.settle()

    def insert_beat_at_cursor(self) -> None:
        """
            Add empty beats to all channels at the index pointed to by the cursor.
        """
        self.insert_beat(self.cursor.x + 1)
        self.cursor.settle()

    def get_grouping_at_cursor(self) -> None:
        """
            Get the grouping object pointed to by the cursor.
        """
        return self.get_grouping([
            self.cursor.y,
            self.cursor.x,
            *self.cursor.position
        ])

    def increment_event_at_cursor(self) -> None:
        """
            If the cursor points to an event grouping.
            Add 1 to it's value.
        """
        grouping = self.get_grouping_at_cursor()
        if not grouping.is_event():
            return

        for event in grouping.get_events():
            new_value = event.note
            if event.relative:
                if (event.note >= event.base \
                or event.note < 0 - event.base) \
                and event.note < (event.base * (event.base - 1)):
                    new_value = event.note + event.base
                elif event.note < event.base:
                    new_value = event.note + 1
            elif event.note < 127:
                new_value = event.note + 1

            self.set_event(
                new_value,
                self.cursor.to_list(),
                relative=event.relative
            )

    def decrement_event_at_cursor(self) -> None:
        """
            If the cursor points to an event grouping.
            Subtract 1 from it's value.
        """
        grouping = self.get_grouping_at_cursor()
        if not grouping.is_event():
            return

        for event in grouping.get_events():
            new_value = event.note
            if event.relative:
                if (event.note <= 0 - event.base \
                or event.note > event.base) \
                and event.note > 0 - (event.base * (event.base - 1)):
                    new_value = event.note - event.base
                elif event.note >= 0 - event.base:
                    new_value = event.note - 1
            elif event.note > 0:
                new_value = event.note - 1

            self.set_event(
                new_value,
                self.cursor.to_list(),
                relative=event.relative
            )

    def jump_to_beat(self, beat: int) -> None:
        """Move the cursor to a specific beat index."""
        self.cursor.set_x(beat)

    def overwrite_beat_at_cursor(
            self,
            channel: Union[int, range],
            line: Union[int, range],
            beat: Union[int, range]) -> None:
        """
            Replace the beat at the cursor with a copy of
            the beat referenced by channel/line/beat
        """
        self.overwrite_beat(self.cursor.get_triplet(), (channel, line, beat))

    def link_beat_at_cursor(
            self,
            channel: Union[int, range],
            line: Union[int, range],
            beat: Union[int, range]) -> None:
        """
            Replace the beat at the cursor with a copy of the
            beat referenced AND force any changes applied to
            one of them to be applied to the other in the future.
        """

        # Convert the arguments given into lists of indices
        if isinstance(channel, range):
            channels = list(channel)
        else:
            channels = [channel]

        if isinstance(line, range):
            lines = list(line)
        else:
            lines = [line]

        if isinstance(beat, range):
            beats = list(beat)
        else:
            beats = [beat]

        # Traverse the list of indices and link all possible
        cursor = self.cursor.get_triplet()
        for i, channel_index in enumerate(channels):
            if not self.channel_groupings[channel_index]:
                continue
            for j, line_index in enumerate(lines):
                if line_index >= len(self.channel_groupings[channel_index]):
                    continue
                for k, beat_index in enumerate(beats):
                    self.link_beats(
                        (cursor[0] + i, cursor[1] + j, cursor[2] + k),
                        (channel_index, line_index, beat_index)
                    )

    def unlink_beat_at_cursor(self) -> None:
        """
            Remove any link *from* the beat pointed to by the cursor.
            Leaves the beat unchanged.
        """
        self.unlink_beat(*self.cursor.get_triplet())

    def apply_register_at_cursor(self) -> None:
        """
            Convert the temporary event to a real one and
            set the grouping pointed to by the cursor.
        """

        register = self.fetch_register()
        if register is None:
            return

        self.set_event(
            register.value,
            self.cursor.to_list(),
            relative=register.relative
        )

    def add_digit_to_register(self, value: int) -> None:
        """Pass a digit to the register"""

        if self.register is None:
            self.register = ReadyEvent(value, relative=False)
        elif self.register.relative:
            self.register.value *= value
        else:
            self.register.value *= self.RADIX
            self.register.value += value

        # If the register is ready, apply it
        if self.register is not None \
        and self.register.relative \
        or self.register.value >= self.RADIX:
            self.apply_register_at_cursor()

    def insert_after_cursor(self) -> None:
        """Use the cursor to create a new empty Grouping."""
        self.insert_after(self.cursor.to_list())

    def clear_register(self) -> None:
        """Cancel event being input."""
        self.register = None

    def fetch_register(self) -> Optional[ReadyEvent]:
        """Get the register, unsetting it"""
        output = self.register
        self.register = None
        return output

    def relative_add_entry(self) -> None:
        """Let the register know the user is inputting a relative event, moving up some keys"""
        # Block relative notes on percussion channel
        active_channel, _, _ = self.cursor.get_triplet()
        if active_channel == 9:
            self.clear_register()
        else:
            self.register = ReadyEvent(1, relative=True)

    def relative_subtract_entry(self) -> None:
        """Let the register know the user is inputting a relative event, moving down some keys"""
        # Block relative notes on percussion channel
        active_channel, _, _ = self.cursor.get_triplet()
        if active_channel == 9:
            self.clear_register()
        else:
            self.register = ReadyEvent(-1, relative=True)

    def relative_downshift_entry(self) -> None:
        """Let the register know the user is inputting a relative event, shifting down octaves"""
        # Block relative notes on percussion channel
        active_channel, _, _ = self.cursor.get_triplet()
        if active_channel == 9:
            self.clear_register()
        else:
            self.register = ReadyEvent(-1 * self.RADIX, relative=True)

    def relative_upshift_entry(self) -> None:
        """Let the register know the user is inputting a relative event, shifting up octaves"""
        # Block relative notes on percussion channel
        active_channel, _, _ = self.cursor.get_triplet()
        if active_channel == 9:
            self.clear_register()
        else:
            self.register = ReadyEvent(self.RADIX, relative=True)

    ## OpusManager methods
    def insert_beat(self, index: Optional[int] = None) -> None:
        super().insert_beat(index)
        self.cursor.settle()

    def remove_beat(self, index: int) -> None:
        super().remove_beat(index)
        self.cursor.settle()

    def remove(self, position: List[int]) -> None:
        super().remove(position)
        self.cursor.settle()

    def swap_channels(self, channel_a: int, channel_b: int) -> None:
        orig_cursor = self.cursor.get_triplet()

        super().swap_channels(channel_a, channel_b)

        new_y = self.get_y(orig_cursor[0])
        new_y += min(orig_cursor[1], len(self.channel_groupings[orig_cursor[0]]) - 1)
        self.cursor.set_y(new_y)
        self.cursor.settle()

    def split_grouping(self, position: List[int], splits: int) -> None:
        super().split_grouping(position, splits)
        self.cursor.settle()

    def remove_line(self, channel: int, index: Optional[int] = None) -> None:
        super().remove_line(channel, index)
        self.cursor.settle()

    def remove_channel(self, channel: int) -> None:
        super().remove_channel(channel)
        self.cursor.settle()

    def overwrite_beat(
            self,
            old_beat: Tuple[int, int, int],
            new_beat: Tuple[int, int, int]) -> None:
        super().overwrite_beat(old_beat, new_beat)
        self.cursor.settle()

    def _new(self) -> None:
        super()._new()
        self.cursor.set(0,0,0)
        self.cursor.settle()

    def _load(self, path: str) -> None:
        super()._load(path)
        self.cursor.settle()

    ## HistoryLayer Methods
    def append_undoer(self, func: Callable[Any], *args, **kwargs) -> None:
        super().append_undoer(func, *args, **kwargs)
        if not (self.history_locked or self.multi_counter):
            self.history_ledger[-1].append((
                self.cursor.set,
                self.cursor.to_list(),
                {}
            ))

    def close_multi(self) -> None:
        super().close_multi()
        if not (self.history_locked or self.multi_counter):
            self.history_ledger[-1].append((
                self.cursor.set,
                self.cursor.to_list(),
                {}
            ))
