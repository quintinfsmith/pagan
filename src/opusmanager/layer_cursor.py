from __future__ import annotations
from typing import Optional, Dict, List, Tuple

from .layer_flag import FlagLayer

class Cursor:
    def __init__(self, opus_manager):
        self.opus_manager = opus_manager
        self.y = 0
        self.x = 0
        self.position = [0]

    def set(self, y, x, *position):
        self.y = y
        self.x = x
        self.position = list(position)

    def move_left(self):
        channel, line, beat = self.get_triplet()
        working_grouping = self.opus_manager.channel_groupings[channel][line][beat]
        for i, j in enumerate(self.position):
            working_grouping = working_grouping[j]

        while self.position:
            if self.position[-1] == 0:
                self.position.pop()
                working_grouping = working_grouping.parent
            else:
                self.position[-1] -= 1
                break

        if not self.position:
            if self.x > 0:
                self.x -= 1
                self.position = [len(working_grouping) - 1]
            else:
                self.position = [0]

        self.settle()

    def move_right(self):
        channel, line, beat = self.get_triplet()
        working_grouping = self.opus_manager.channel_groupings[channel][line][beat]
        for i, j in enumerate(self.position):
            working_grouping = working_grouping[j]

        while self.position:
            if len(working_grouping.parent) - 1 > self.position[-1]:
                self.position[-1] += 1
                break
            elif len(working_grouping.parent) - 1 == self.position[-1]:
                self.position.pop()
                working_grouping = working_grouping.parent

        if not self.position:
            if self.x < self.opus_manager.opus_beat_count - 1:
                self.x += 1
                self.position = [0]
            else:
                self.position = [len(working_grouping) - 1]

        self.settle()

    def move_up(self):
        if self.y > 0:
            self.y -= 1

        self.settle()

    def move_down(self):
        line_count = self.opus_manager.line_count
        if self.y < line_count - 1:
            self.y += 1

        self.settle()

    def move_climb(self):
        if len(self.position) > 1:
            self.position.pop()

    def move_dive(self, index):
        beat = self.opus_manager.get_grouping([self.y, self.x])
        working_grouping = beat
        for i, j in enumerate(self.position):
            working_grouping = working_grouping[j]

        if working_grouping.is_structural() and index < len(working_grouping):
            self.position.append(index)

    def to_list(self):
        return [self.y, self.x, *self.position]

    def set_y(self, new_y):
        self.y = new_y

    def set_x(self, new_x):
        beat_count = self.opus_manager.opus_beat_count
        if new_x < 0:
            new_x = beat_count - new_x
        else:
            new_x = min(new_x, beat_count - 1)
        self.x = new_x

    def settle(self, right_align=False):
        ''' Sink the cursor as deep as possible. (until it finds a leaf) '''
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
        '''
            get the channel and index from y
        '''
        return self.opus_manager.get_channel_index(self.y)

    def get_triplet(self):
        channel, index = self.get_channel_index()
        return (channel, index, self.x)

class CursorLayer(FlagLayer):
    def __init__(self):
        super().__init__()
        self.cursor = Cursor(self)

    ## Layer-Specific methods

    def new_line_at_cursor(self):
        channel, _, _ = self.cursor.get_triplet()
        self.new_line(channel)

    def remove_line_at_cursor(self):
        channel, index, _ = self.cursor.get_triplet()
        self.remove_line(channel, index)

    def remove_beat_at_cursor(self):
        self.remove_beat(self.cursor.x)
        self.cursor.move_left()

    def split_grouping_at_cursor(self):
        self.split_grouping(self.cursor.to_list(), 2)
        self.cursor.settle()

    def unset_at_cursor(self):
        self.unset(self.cursor.to_list())

    def remove_grouping_at_cursor(self):
        self.remove(self.cursor.to_list())
        self.cursor.settle()

    def insert_beat_at_cursor(self):
        self.insert_beat(self.cursor.x + 1)
        self.cursor.settle()

    def get_grouping_at_cursor(self):
        return self.get_grouping([
            self.cursor.y,
            self.cursor.x,
            *self.cursor.position
        ])

    def increment_event_at_cursor(self):
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

    def decrement_event_at_cursor(self):
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

    def jump_to_beat(self, beat):
        self.cursor.set_x(beat)

    def overwrite_beat_at_cursor(self, channel, index, beat):
        self.overwrite_beat(self.cursor.get_triplet(), (channel, index, beat))

    def link_beat_at_cursor(self, channel, index, beat):
        if isinstance(channel, range):
            channels = list(channel)
        else:
            channels = [channel]

        if isinstance(index, range):
            indices = list(index)
        else:
            indices = [index]

        if isinstance(beat, range):
            beats = list(beat)
        else:
            beats = [beat]

        cursor_c, cursor_i, cursor_b = self.cursor.get_triplet()
        for i, channel in enumerate(channels):
            if not self.channel_groupings[channel]:
                continue
            for j, index in enumerate(indices):
                if index >= len(self.channel_groupings[channel]):
                    continue
                for k, beat in enumerate(beats):
                    self.link_beats((cursor_c + i, cursor_i + j, cursor_b + k), (channel, index, beat))

    def unlink_beat_at_cursor(self):
        self.unlink_beat(*self.cursor.get_triplet())

    def add_digit_to_register(self, value):
        super().add_digit_to_register(value)
        if self.register is None:
            pass
        elif self.register.relative or self.register.value >= self.RADIX:
            self.apply_register_at_cursor()

    def apply_register_at_cursor(self):
        register = self.fetch_register()
        if register is None:
            return

        self.set_event(
            register.value,
            self.cursor.to_list(),
            relative=register.relative
        )


    def insert_after_cursor(self):
        self.insert_after(self.cursor.to_list())

    def set_active_beat_size(self, size):
        self.set_beat_size(size, self.cursor.to_list())

    def get_active_line(self):
        return self.get_line(self.cursor.y)

    ## OpusManager methods
    def relative_add_entry(self):
        super().relative_add_entry()
        # Block relative notes on percussion channel
        active_channel, _, _ = self.cursor.get_triplet()
        if active_channel == 9:
            self.clear_register()

    def relative_subtract_entry(self):
        super().relative_subtract_entry()
        # Block relative notes on percussion channel
        active_channel, _, _ = self.cursor.get_triplet()
        if active_channel == 9:
            self.clear_register()

    def relative_downshift_entry(self):
        super().relative_downshift_entry()
        # Block relative notes on percussion channel
        active_channel, _, _ = self.cursor.get_triplet()
        if active_channel == 9:
            self.clear_register()

    def relative_upshift_entry(self):
        super().relative_upshift_entry()
        # Block relative notes on percussion channel
        active_channel, _, _ = self.cursor.get_triplet()
        if active_channel == 9:
            self.clear_register()


    def insert_beat(self, index=None):
        super().insert_beat(index)
        self.cursor.settle()

    def remove_beat(self, index):
        super().remove_beat(index)
        self.cursor.settle()

    def remove(self, position):
        super().remove(position)
        self.cursor.settle()

    def swap_channels(self, channel_a, channel_b):
        orig_cursor = self.cursor.get_triplet()

        super().swap_channels(channel_a, channel_b)

        new_y = self.get_y(orig_cursor[0])
        new_y += min(orig_cursor[1], len(self.channel_groupings[orig_cursor[0]]) - 1)
        self.cursor.set_y(new_y)
        self.cursor.settle()

    def split_grouping(self, position, splits):
        super().split_grouping(position, splits)
        self.cursor.settle()

    def remove_line(self, channel, index=None):
        super().remove_line(channel, index)
        self.cursor.settle()

    def remove_channel(self, channel):
        super().remove_channel(channel)
        self.cursor.settle()

    def overwrite_beat(self, old_beat, new_beat):
        super().overwrite_beat(old_beat, new_beat)
        self.cursor.settle()

    def new(self):
        super().new()
        self.cursor.set(0,0,0)
        self.cursor.settle()

    def load(self, path: str) -> None:
        super().load(path)
        self.cursor.settle()
