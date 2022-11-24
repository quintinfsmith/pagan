"""Cursor Layer of the OpusManager and the classes required to make it work"""
from __future__ import annotations
from typing import List, Optional, Tuple, Union, Any
from collections.abc import Callable
from .miditree import MIDITree, MIDITreeEvent
from .layer_links import LinksLayer, BeatKey

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

        working_tree = self.opus_manager.get_beat_tree(self.get_triplet())
        # First, traverse the position to get the currently active tree
        for j in self.position:
            working_tree = working_tree[j]

        # Then move the cursor up until it can move left
        while self.position:
            if self.position[-1] == 0:
                self.position.pop()
                working_tree = working_tree.parent
            else:
                self.position[-1] -= 1
                break


        settle_right = True
        if not self.position:
            if self.x:
                self.x -= 1
            else:
                settle_right = False

        # Move to the leaf
        self.settle(right_align=settle_right)

    def move_right(self) -> None:
        """Point the cursor to the next leaf"""
        working_tree = self.opus_manager.get_beat_tree(self.get_triplet())
        # First, traverse the position to get the currently active tree
        for j in self.position:
            working_tree = working_tree[j]

        # Then move the cursor up until it can move right
        while self.position:
            if len(working_tree.parent) - 1 == self.position[-1]:
                self.position.pop()
                working_tree = working_tree.parent
            elif len(working_tree.parent) - 1 > self.position[-1]:
                self.position[-1] += 1
                break

        settle_right = False
        if not self.position:
            if self.x < self.opus_manager.opus_beat_count - 1:
                self.x += 1
            else:
                settle_right = True
        self.settle(right_align=settle_right)

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
        beat = self.opus_manager.get_tree([self.y, self.x])
        working_tree = beat
        for j in self.position:
            working_tree = working_tree[j]

        if not working_tree.is_leaf() and index < len(working_tree):
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
            new_x = beat_count + max(0 - beat_count, new_x)
        else:
            new_x = min(new_x, beat_count - 1)
        self.x = new_x

    def settle(self, right_align: bool = False) -> None:
        """ Sink the cursor as deep as possible. (until it finds a leaf) """
        self.x = max(0, min(self.x, self.opus_manager.opus_beat_count - 1))
        channel, line, beat = self.get_triplet()
        ## First get the beat ...
        working_beat = self.opus_manager.channel_lines[channel][line][beat]
        working_tree = working_beat

        if not self.position:
            if right_align:
                self.position = [len(working_tree) - 1]
            else:
                self.position = [0]

        ## The get the current working_tree ...
        index = 0
        for j in self.position:
            if working_tree.is_leaf() or len(working_tree) <= j:
                break
            working_tree = working_tree[j]
            index += 1

        self.position = self.position[0:index]


        ## ... Then find the leaf, if not already found
        if not right_align:
            while not working_tree.is_leaf() or working_tree == working_beat:
                self.position.append(0)
                working_tree = working_tree[0]
        else:
            while not working_tree.is_leaf() or working_tree == working_beat:
                self.position.append(len(working_tree) - 1)
                working_tree = working_tree[-1]

    def get_channel_index(self) -> (int, int):
        """
            get the channel and index from y
        """
        return self.opus_manager.get_channel_index(self.y)

    def get_triplet(self) -> BeatKey:
        """Get the active beat's channel, line and x position rather than x/y"""
        channel, index = self.get_channel_index()
        return (channel, index, self.x)

class CursorLayer(LinksLayer):
    """Adds Cursor functionality to OpusManager."""
    def __init__(self):
        super().__init__()
        self.cursor = Cursor(self)
        self.channel_order = list(range(16))

    ## Layer-Specific methods
    @property
    def line_count(self) -> int:
        """Get the number of lines active in this opus."""
        return sum((len(channel) for channel in self.channel_lines))

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

    def set_percussion_event_at_cursor(self) -> None:
        """Turn on percussion at the position pointed to by the cursor"""
        if self.cursor.get_triplet()[0] == 9:
            self.set_percussion_event(self.cursor.get_triplet(), self.cursor.position)

    def new_line_at_cursor(self) -> None:
        """Insert a line at the channel pointed to by the cursor"""
        channel, _, _ = self.cursor.get_triplet()
        self.new_line(channel)

    def remove_line_at_cursor(self) -> None:
        """Remove the line from the channel pointed to by the cursor"""
        channel, index, _ = self.cursor.get_triplet()
        if self.cursor.y == self.line_count - 1:
            self.cursor.y -= 1

        self.remove_line(channel, index)

    def remove_beat_at_cursor(self) -> None:
        """Remove the beat from all channels pointed to by the cursor"""
        self.remove_beat(self.cursor.x)
        self.cursor.move_left()

    def split_tree_at_cursor(self) -> None:
        """Divide the tree pointed to by the cursor into 2"""

        beat_key = self.cursor.get_triplet()
        position = self.cursor.position
        beat_tree = self.get_beat_tree(beat_key)

        # Special case for beats' only leaf
        if beat_tree.size == 1 and position == [0]:
            self.insert_after(beat_key, position)
        else:
            self.split_tree(beat_key, position, 2)
        self.cursor.settle()

    def unset_at_cursor(self) -> None:
        """
            Remove the event pointed to by the cursor.
            Leaves the tree in tact.
        """
        self.unset(self.cursor.get_triplet(), self.cursor.position)

    def remove_tree_at_cursor(self) -> None:
        """
            Remove the tree pointed to by the cursor from its parent.
            Reduces the size of the parent.
        """
        self.remove(self.cursor.get_triplet(), self.cursor.position)
        self.cursor.settle()

    def insert_beat_at_cursor(self) -> None:
        """
            Add empty beats to all channels at the index pointed to by the cursor.
        """
        self.insert_beat(self.cursor.x + 1)
        self.cursor.settle()

    def get_tree_at_cursor(self) -> MIDITree:
        """
            Get the tree object pointed to by the cursor.
        """
        return self.get_tree(self.cursor.get_triplet(), self.cursor.position)

    def increment_event_at_cursor(self) -> None:
        """
            If the cursor points to an event tree.
            Add 1 to it's value.
        """
        tree = self.get_tree_at_cursor()
        if not tree.is_event():
            return

        event = tree.get_event()
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

        event.note = new_value

        self.set_event(
            self.cursor.get_triplet(),
            self.cursor.position,
            event
        )

    def decrement_event_at_cursor(self) -> None:
        """
            If the cursor points to an event tree.
            Subtract 1 from it's value.
        """
        tree = self.get_tree_at_cursor()
        if not tree.is_event():
            return

        event = tree.get_event()
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

        event.note = new_value

        self.set_event(
            self.cursor.get_triplet(),
            self.cursor.position,
            event
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
            if not self.channel_lines[channel_index]:
                continue
            for j, line_index in enumerate(lines):
                if line_index >= len(self.channel_lines[channel_index]):
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
        self.unlink_beat(self.cursor.get_triplet())

    def insert_after_cursor(self) -> None:
        """Use the cursor to create a new empty Grouping."""
        self.insert_after(self.cursor.get_triplet(), self.cursor.position)

    def get_channel_index(self, y_index: int) -> Tuple[int, int]:
        """
            Given the y-index of a line (as in from the cursor),
            get the channel and index thereof
        """

        for channel in self.channel_order:
            for i, _ in enumerate(self.channel_lines[channel]):
                if y_index == 0:
                    return (channel, i)
                y_index -= 1

        raise IndexError

    def get_y(self, channel_index: int, line_offset: Optional[int] = None) -> int:
        """
            Given a channel and index,
            get the y-index of the specified line displayed
        """
        if line_offset is None:
            line_offset = len(self.channel_lines[channel_index]) - 1

        y_index = 0
        for i in self.channel_order:
            for j, _line in enumerate(self.channel_lines[i]):
                if channel_index == i and line_offset == j:
                    return y_index
                y_index += 1
        return -1

    ## OpusManager methods
    def insert_beat(self, index: Optional[int] = None) -> None:
        super().insert_beat(index)
        self.cursor.settle()

    def remove_beat(self, index: int) -> None:
        super().remove_beat(index)
        self.cursor.settle()

    def remove(self, beat_key: BeatKey, position: List[int]) -> None:
        super().remove(beat_key, position)
        self.cursor.settle()

    def swap_channels(self, channel_a: int, channel_b: int) -> None:
        orig_cursor = self.cursor.get_triplet()

        super().swap_channels(channel_a, channel_b)

        new_y = self.get_y(orig_cursor[0])
        new_y += min(orig_cursor[1], len(self.channel_lines[orig_cursor[0]]) - 1)
        self.cursor.set_y(new_y)
        self.cursor.settle()

    def split_tree(self, beat_key: BeatKey, position: List[int], splits: int) -> None:
        super().split_tree(beat_key, position, splits)
        self.cursor.settle()

    def remove_line(self, channel: int, index: Optional[int] = None) -> None:
        super().remove_line(channel, index)
        self.cursor.settle()

    def remove_channel(self, channel: int) -> None:
        super().remove_channel(channel)
        self.cursor.settle()

    def overwrite_beat(
            self,
            old_beat: BeatKey,
            new_beat: BeatKey) -> None:
        super().overwrite_beat(old_beat, new_beat)
        self.cursor.settle()

    def _new(self) -> None:
        super()._new()
        self.cursor.set(0,0,0)
        self.cursor.settle()

    def _load(self, path: str) -> None:
        super()._load(path)
        self.cursor.settle()

