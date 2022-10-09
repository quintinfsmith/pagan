from __future__ import annotations
import math
import time
import threading

from enum import Enum, auto
from typing import Optional, Dict, List, Tuple

import wrecked

from mgrouping import get_number_string
from interactor import Interactor
from opusmanager import OpusManager

class InvalidCursor(Exception):
    '''Raised when attempting to pass a cursor without enough arguments'''

class ReadyEvent:
    def __init__(self, initial_value, *, relative=False):
        self.value = initial_value
        self.relative = relative
        self.flag_changed = True

class InputContext(Enum):
    Default = auto()
    Text = auto()

class RectFrame:
    def __init__(self, parent_rect, border=None):
        self.frame = parent_rect.new_rect()
        self.wrapper = self.frame.new_rect()
        self.content = self.wrapper.new_rect()
        self.rendered_size = None
        self.parent = parent_rect
        self.view_offset = (0, 0)
        if border is None:
            self.border = [
                chr(9581),
                chr(9582),
                chr(9583),
                chr(9584),
                chr(9472),
                chr(9474)
            ]
        else:
            self.border = border

    def resize(self, width, height):
        '''Wrap the resize function for the wrapper'''
        if self.border:
            self.frame.resize(width + 2, height + 2)
            self.wrapper.move(1, 1)
        else:
            self.frame.resize(width, height)
            self.wrapper.move(0, 0)

        self.wrapper.resize(width, height)
        self.draw_border()

    @property
    def full_height(self):
        return self.frame.height

    @property
    def full_width(self):
        return self.frame.width

    @property
    def height(self):
        return self.wrapper.height
    @property
    def width(self):
        return self.wrapper.width

    @property
    def size(self):
        return (self.full_width, self.full_height)

    def detach(self):
        self.frame.detach()

    def attach(self):
        self.parent.attach(self.frame)

    def move(self, x, y):
        self.frame.move(x, y)

    def move_inner(self, x, y):
        self.view_offset = (x, y)
        self.content.move(x, y)

    def get_content_rect(self):
        return self.content

    def get_view_offset(self):
        return self.view_offset

    def draw_border(self):
        if not self.border:
            return

        width = self.frame.width
        height = self.frame.height
        for y in range(height):
            self.frame.set_string(0, y, self.border[5])
            self.frame.set_string(width - 1, y, self.border[5])

        for x in range(width):
            self.frame.set_string(x, 0, self.border[4])
            self.frame.set_string(x, height - 1, self.border[4])

        self.frame.set_string(0, 0, chr(9581))
        self.frame.set_string(width - 1, 0, chr(9582))
        self.frame.set_string(width - 1, height - 1, chr(9583))
        self.frame.set_string(0, height - 1, chr(9584))

class CommandLedger:
    def __init__(self, command_map):
        self.command_map = command_map
        self.history = []
        self.register = None
        self.active_entry = None
        self.register_bkp = None

    def go_to_prev(self):
        if self.active_entry is None:
            if self.history:
                self.active_entry = len(self.history) - 1
                self.register_bkp = self.register
                self.register = self.history[self.active_entry]
        elif self.active_entry > 0:
            self.active_entry -= 1
            self.register = self.history[self.active_entry]

    def go_to_next(self):
        if self.active_entry is None:
            return
        elif self.active_entry < len(self.history) - 2:
            self.active_entry += 1
            self.register = self.history[self.active_entry]
        elif self.active_entry == len(self.history) - 1:
            self.active_entry = None
            self.register = self.register_bkp
            self.register_bkp = None

    def open(self):
        self.register = ""
        self.active_entry = None
        self.register_bkp = None

    def close(self):
        self.register = None
        self.active_entry = None
        self.register_bkp = None

    def is_open(self):
        return self.register is not None

    def input(self, character: str):
        if not self.is_open():
            return
        self.register += character

    def backspace(self):
        if not self.is_open():
            return

        if self.register:
            self.register = self.register[0:-1]
        else:
            self.close()


    def run(self):
        if not self.is_open():
            return
        cmd_parts = self.register.split(" ")
        if cmd_parts[0] in self.command_map:
            try:
                self.command_map[cmd_parts[0]](*cmd_parts[1:])
                self.history.append(self.register)
            except Exception as exception:
                raise Exception from exception

    def get_register(self):
        return self.register

class EditorEnvironment:
    tick_delay = 1 / 24

    def daemon_input(self):
        while self.running:
            self.interactor.get_input()
        self.interactor.restore_input_settings()

    def __init__(self):
        self.running = False
        self.root = wrecked.init()

        self.register = None
        #self.rendered_register = None
        #self.rect_register = self.rect_view_window.new_rect()

        self.channel_rects = [[] for i in range(16)]
        self.opus_manager = OpusManager()


        self.command_ledger = CommandLedger({
            'w': self.save,
            'q': self.kill,
            'c+': self.add_channel,
            'c-': self.remove_channel,
            'export': self.export,
            'swap': self.cmd_swap_channels
        })

        self.rendered_command_register = None

        self.rendered_channel_rects = set()
        self.rendered_beat_widths = []

        self.rendered_cursor_position = []
        self.rendered_line_length = 0
        self.rendered_line_count = 0

        # Rect sizes for borders
        self.rendered_content_size = None
        self.rendered_command_register_size = None
        self.rendered_line_labels_size = None

        self.rect_beats = {}
        self.rect_beat_lines = {}
        self.rect_beat_labels = {}
        self.subbeat_rect_map = {}
        self.rect_channel_dividers = {}

        self.rect_view = self.root.new_rect()
        self.rect_view.resize(self.root.width, self.root.height)
        self.rect_view.move(0,0)
        self.frame_command_register = RectFrame(self.rect_view)
        self.frame_command_register.detach()

        self.frame_line_labels = RectFrame(self.rect_view)
        self.frame_content = RectFrame(self.rect_view)
        self.frame_topbar = RectFrame(self.rect_view, False)

        self.flag_beat_changed = set()
        self.flag_line_changed = set()

        self.interactor = Interactor()
        self.interactor.set_context(InputContext.Default)
        self.interactor.assign_context_sequence(
            InputContext.Default,
            'l',
            self.cursor_right
        )
        self.interactor.assign_context_sequence(
            InputContext.Default,
            'h',
            self.cursor_left
        )
        self.interactor.assign_context_sequence(
            InputContext.Default,
            'j',
            self.cursor_down
        )
        self.interactor.assign_context_sequence(
            InputContext.Default,
            'k',
            self.cursor_up
        )
        self.interactor.assign_context_sequence(
            InputContext.Default,
            "x",
            self.remove_at_cursor
        )
        self.interactor.assign_context_sequence(
            InputContext.Default,
            '.',
            self.unset_at_cursor
        )
        self.interactor.assign_context_sequence(
            InputContext.Default,
            'i',
            self.insert_after_cursor
        )
        self.interactor.assign_context_sequence(
            InputContext.Default,
            ' ',
            self.insert_beat_at_cursor
        )
        self.interactor.assign_context_sequence(
            InputContext.Default,
            'X',
            self.remove_beat_at_cursor
        )
        self.interactor.assign_context_sequence(
            InputContext.Default,
            '/',
            self.split_at_cursor
        )
        self.interactor.assign_context_sequence(
            InputContext.Default,
            ';]',
            self.new_line
        )
        self.interactor.assign_context_sequence(
            InputContext.Default,
            ';[',
            self.remove_line
        )

        self.interactor.assign_context_sequence(
            InputContext.Default,
            '+',
            self.relative_add_entry
        )

        self.interactor.assign_context_sequence(
            InputContext.Default,
            '-',
            self.relative_subtract_entry
        )
        self.interactor.assign_context_sequence(
            InputContext.Default,
            'v',
            self.relative_downshift_entry
        )
        self.interactor.assign_context_sequence(
            InputContext.Default,
            '^',
            self.relative_upshift_entry
        )
        self.interactor.assign_context_sequence(
            InputContext.Default,
            'K',
            self.increment_event_at_cursor
        )
        self.interactor.assign_context_sequence(
            InputContext.Default,
            'J',
            self.decrement_event_at_cursor
        )

        for c in "0123456789ab":
            self.interactor.assign_context_sequence(
                InputContext.Default,
                c,
                self.add_digit_to_register,
                int(c, 12)
            )

        self.interactor.assign_context_sequence(
            InputContext.Default,
            "\x1B",
            self.clear_register
        )

        self.interactor.assign_context_sequence(
            InputContext.Default,
            ":",
            self.command_ledger_open
        )

        for c in range(32, 127):
            self.interactor.assign_context_sequence(
                InputContext.Text,
                chr(c),
                self.command_ledger_input,
                chr(c)
            )
        self.interactor.assign_context_sequence(
            InputContext.Text,
            "\x7F",
            self.command_ledger_backspace
        )

        #self.interactor.assign_context_sequence(
        #    InputContext.Text,
        #    "\x1B\x1B",
        #    self.command_ledger_close
        #)

        self.interactor.assign_context_sequence(
            InputContext.Text,
            "\r",
            self.command_ledger_run
        )

        self.interactor.assign_context_sequence(
            InputContext.Text,
            "\x1B[A", # Arrow Up
            self.command_ledger.go_to_prev
        )
        self.interactor.assign_context_sequence(
            InputContext.Text,
            "\x1B[B", # Arrow Down
            self.command_ledger.go_to_next
        )



        #self.interactor.assign_context_sequence(
        #    "\x7f",
        #    self.remove_last_digit_from_register
        #)

    def cmd_swap_channels(self, *args):
        channel_a = int(args[0])
        channel_b = int(args[1])
        self.swap_channels(channel_a, channel_b)

    def export(self, /, path, *args):
        kwargs = parse_kwargs(*args)
        self.opus_manager.export(path=path, **kwargs)

    def add_channel(self, /, channel, *args):
        channel = int(channel)
        self.opus_manager.new_line(channel)

        rect_content = self.frame_content.get_content_rect()

        if not self.channel_rects[channel]:
            self.rect_channel_dividers[channel] = rect_content.new_rect()

        grouping_rect = rect_content.new_rect()
        self.channel_rects[channel].append(grouping_rect)

        for i in range(self.opus_manager.opus_beat_count):
            self.flag_beat_changed.add((channel, len(self.channel_rects[channel]) - 1, i))
        self.rendered_cursor_position = None

    def remove_channel(self, /, channel, *args):
        channel = int(channel)
        self.opus_manager.remove_channel(channel)

        if self.channel_rects[channel]:
            while self.channel_rects[channel]:
                self.channel_rects[channel].pop().remove()

            self.rect_channel_dividers[channel].remove()
            del self.rect_channel_dividers[channel]

            self.rendered_cursor_position = None

    def save(self, *args):
        self.opus_manager.save()

    def command_ledger_run(self):
        self.command_ledger.run()
        self.command_ledger_close()

    def command_ledger_open(self):
        self.command_ledger.open()
        self.interactor.set_context(InputContext.Text)

    def command_ledger_close(self):
        self.command_ledger.close()
        self.interactor.set_context(InputContext.Default)

    def command_ledger_input(self, character):
        self.command_ledger.input(character)

    def command_ledger_backspace(self):
        self.command_ledger.backspace()
        if not self.command_ledger.is_open():
            self.interactor.set_context(InputContext.Default)

    def increment_event_at_cursor(self):
        position = self.opus_manager.cursor_position
        grouping = self.opus_manager.get_grouping(position)
        if not grouping.is_event():
            return

        for event in grouping.get_events():
            if event.relative:
                if (event.note >= event.base \
                or event.note < 0 - event.base) \
                and event.note < (event.base * (event.base - 1)):
                    event.note += event.base
                elif event.note < event.base:
                    event.note += 1
            elif event.note < 127:
                event.note += 1

        c, i = self.opus_manager.get_channel_index(position[0])
        self.flag_beat_changed.add((c, i, position[1]))
        self.rendered_cursor_position = None

    def decrement_event_at_cursor(self):
        position = self.opus_manager.cursor_position
        grouping = self.opus_manager.get_grouping(position)
        if not grouping.is_event():
            return

        for event in grouping.get_events():
            if event.relative:
                if (event.note <= 0 - event.base \
                or event.note > event.base) \
                and event.note > 0 - (event.base * (event.base - 1)):
                    event.note -= event.base
                elif event.note >= 0 - event.base:
                    event.note -= 1
            elif event.note > 0:
                event.note -= 1

        c, i = self.opus_manager.get_channel_index(position[0])
        self.flag_beat_changed.add((c, i, position[1]))
        self.rendered_cursor_position = None

    def relative_add_entry(self):
        self.register = ReadyEvent(1, relative=True)

    def relative_subtract_entry(self):
        self.register = ReadyEvent(-1, relative=True)

    def relative_downshift_entry(self):
        self.register = ReadyEvent(-1 * self.opus_manager.BASE, relative=True)

    def relative_upshift_entry(self):
        self.register = ReadyEvent(self.opus_manager.BASE, relative=True)

    def remove_last_digit_from_register(self):
        if self.register is None:
            pass
        elif self.register.value == 0 or self.register.relative:
            self.register = None
        else:
            self.register.value //= self.opus_manager.BASE

    def new_line(self):
        cursor = self.opus_manager.cursor_position
        channel, _i = self.opus_manager.get_channel_index(cursor[0])

        self.opus_manager.new_line(channel)

        rect_content = self.frame_content.get_content_rect()

        grouping_rect = rect_content.new_rect()
        self.channel_rects[channel].append(grouping_rect)
        for i in range(self.opus_manager.opus_beat_count):
            self.flag_beat_changed.add((channel, len(self.channel_rects[channel]) - 1, i))

    def remove_line(self):
        if self.get_line_count() <= 1:
            return

        cursor = self.opus_manager.cursor_position
        target_c, target_i = self.opus_manager.get_channel_index(cursor[0])

        self.opus_manager.remove_line(target_c, target_i)
        self.channel_rects[target_c].pop().remove()

        if not self.channel_rects[target_c]:
            self.rect_channel_dividers[target_c].remove()
            del self.rect_channel_dividers[target_c]

        for i, _line in enumerate(self.channel_rects[target_c]):
            for j in range(self.opus_manager.opus_beat_count):
                self.flag_beat_changed.add((target_c, i, j))

        if cursor[0] == self.get_line_count():
            cursor[0] -= 1

        while True:
            try:
                grouping = self.opus_manager.get_grouping(cursor)
                break
            except InvalidCursor:
                cursor.pop()
            except IndexError:
                cursor[-1] -= 1

        grouping = self.opus_manager.get_grouping(cursor)
        while grouping.is_structural():
            cursor.append(0)
            grouping = grouping[0]

        self.opus_manager.cursor_position = cursor
        self.rendered_cursor_position = None
        self.rendered_line_count = 0

    def split_at_cursor(self):
        cursor = self.opus_manager.cursor_position
        self.opus_manager.split(cursor, 2)
        self.opus_manager.cursor_position.append(0)

        c, i = self.opus_manager.get_channel_index(cursor[0])
        self.flag_beat_changed.add((c, i, cursor[1]))
        self.rendered_cursor_position = None

    def remove_beat_at_cursor(self):
        cursor = self.opus_manager.cursor_position
        self.opus_manager.remove_beat(cursor[1])
        self.rendered_beat_widths.pop(cursor[1])

        to_del = []
        for (c,i,b) in self.rect_beats:
            if b >= self.opus_manager.opus_beat_count:
                to_del.append(self.rect_beats[(c, i, b)])
            elif b >= cursor[1]:
                self.flag_beat_changed.add((c, i, b))

        # TODO: Is this necessary?
        for beat in to_del:
            del beat

        self.opus_manager.cursor_position = cursor[0:2]
        while self.opus_manager.cursor_position[1] >= len(self.rendered_beat_widths):
            self.opus_manager.cursor_position[1] -= 1

        grouping = self.opus_manager.get_grouping(self.opus_manager.cursor_position)
        while grouping.is_structural():
            self.opus_manager.cursor_position.append(0)
            grouping = grouping[0]

        self.rendered_cursor_position = None

    def insert_beat_at_cursor(self):
        cursor = self.opus_manager.cursor_position
        self.opus_manager.insert_beat(cursor[1])
        self.rendered_beat_widths.insert(
            cursor[1],
            self.rendered_beat_widths[cursor[1]]
        )

        for (c,i,b), rect in self.rect_beats.items():
            if b == cursor[1]:
                self.flag_beat_changed.add((c, i, b))
            if b >= cursor[1]:
                self.flag_beat_changed.add((c, i, b + 1))

        self.opus_manager.cursor_position = cursor[0:2]
        self.rendered_cursor_position = None

    def add_digit_to_register(self, value):
        if self.register is None:
            self.register = ReadyEvent(value, relative=False)
        elif self.register.relative:
            self.register.value *= value
            self.set_event_at_cursor()
        else:
            self.register.value *= self.opus_manager.BASE
            self.register.value += value
            if self.register.value >= self.opus_manager.BASE:
                self.set_event_at_cursor()

    def clear_register(self):
        self.register = None

    def fetch_register(self):
        output = self.register
        self.register = None
        return output

    def kill(self, *args):
        self.running = False
        wrecked.kill()

    def load(self, path: str) -> None:
        self.opus_manager.load(path)

        # TODO: Could be cleaner
        cursor_position = [0,0]
        grouping = self.opus_manager.get_grouping(cursor_position)
        while grouping.is_structural():
            cursor_position.append(0)
            grouping = grouping[0]
        self.opus_manager.cursor_position = cursor_position

        rect_content = self.frame_content.get_content_rect()
        for i, channel in enumerate(self.opus_manager.channel_groupings):
            for j, grouping in enumerate(channel):
                grouping_rect = rect_content.new_rect()
                self.channel_rects[i].append(grouping_rect)
                #self.flag_line_changed.add((c,i))
                for k in range(len(grouping)):
                    self.flag_beat_changed.add((i, j, k))

            if channel:
                self.rect_channel_dividers[i] = rect_content.new_rect()

    def tick(self):
        flag_draw = False
        flag_draw |= self.tick_update_beats()
        flag_draw |= self.tick_update_lines()
        flag_draw |= self.tick_update_cursor()
        flag_draw |= self.tick_update_view_offset()
        #flag_draw |= self.tick_update_register()
        flag_draw |= self.tick_update_command_register()

        if flag_draw:
            self.root.draw()


    def tick_update_command_register(self) -> bool:
        output = False
        cmd_register = self.command_ledger.get_register()
        if self.rendered_command_register != cmd_register:
            output = True
            if cmd_register is not None:
                if self.rendered_command_register is None:
                    self.frame_command_register.attach()

                self.frame_command_register.resize(self.rect_view.width - 2, 1)
                self.frame_command_register.move(
                    0,
                    self.rect_view.height - self.frame_command_register.full_height
                )

                rect_content = self.frame_command_register.get_content_rect()
                rect_content.clear_children()
                rect_content.clear_characters()
                rect_content.resize(
                    max(
                        2 + len(cmd_register),
                        self.frame_command_register.full_width - 2
                    ),
                    1
                )
                rect_content.set_string(0, 0, f":{cmd_register}_")

               # self.rect_content_wrapper.resize(self.rect_content_wrapper.width, self.rect_view.height - 5)
            else:
                self.frame_command_register.detach()
               # self.rect_content_wrapper.resize(self.rect_content_wrapper.width, self.rect_view.height - 2)

            self.rendered_command_register = cmd_register

        return output

    def tick_update_view_offset(self) -> bool:
        did_change_flag = False
        if self.rect_view.width - self.frame_line_labels.full_width != self.frame_content.full_width:
            self.frame_content.resize(
                self.rect_view.width - (self.frame_line_labels.full_width) - 2,
                self.rect_view.height - 3
            )

            self.frame_content.move(self.frame_line_labels.full_width, 1)

            self.frame_topbar.resize(
                self.rect_view.width - (self.frame_line_labels.full_width) - 2,
                1
            )

            self.frame_topbar.move(self.frame_line_labels.full_width + 1, 0)

            did_change_flag = True

        cursor = self.opus_manager.cursor_position
        beat_offset = sum(self.rendered_beat_widths[0:cursor[1]]) + cursor[1] - 1
        line_length = sum(self.rendered_beat_widths) + len(self.rendered_beat_widths) - 1
        beat_width = self.rendered_beat_widths[cursor[1]]

        x_offset = beat_offset - ((self.frame_content.width - beat_width) // 2.5)
        x_offset = max(0, x_offset)
        x_offset = min(line_length - self.frame_content.width, x_offset)

        c, i = self.opus_manager.get_channel_index(cursor[0])

        y_offset = self.channel_rects[c][i].y - ((self.frame_content.height - 1) // 2.5)
        y_offset = min(self.frame_content.get_content_rect().height - self.frame_content.height - 1, y_offset)
        y_offset = max(0, y_offset)

        new_offset = (int(-1 *  x_offset), int(-1 * y_offset))

        if did_change_flag or new_offset != self.frame_content.get_view_offset():
            self.frame_topbar.move_inner(new_offset[0], 0)
            self.frame_content.move_inner(*new_offset)
            self.frame_line_labels.move_inner(0, new_offset[1])
            did_change_flag = True

        return did_change_flag

    def tick_update_beats(self) -> bool:
        channels = self.opus_manager.channel_groupings
        output = bool(self.flag_beat_changed)

        rect_topbar = self.frame_topbar.get_content_rect()

        beat_indeces_changed = set()
        # The structure of the beat changed. rebuild the rects
        while self.flag_beat_changed:
            c, i, b = self.flag_beat_changed.pop()
            if i >= len(channels[c]):
                continue

            line = channels[c][i]
            beat = line[b]
            rect_line = self.channel_rects[c][i]

            if (c, i, b) in self.rect_beats:
                rect_beat = self.rect_beats[(c, i, b)].parent
                try:
                    self.rect_beats[(c, i, b)].remove()
                except wrecked.NotFound:
                    pass
            else:
                rect_beat = rect_line.new_rect()

            self.rect_beats[(c, i, b)] = rect_beat.new_rect()
            subbeat_map = self.build_beat_rect(beat, self.rect_beats[(c, i, b)])

            self.subbeat_rect_map[(c, i, b)] = subbeat_map
            beat_indeces_changed.add(b)

        # Resize the adjacent beats in all the lines to match sizes
        ## First, update the list of widest beat widths (self.rendered_beat_widths)
        ##  and map the increases/reductions in changed widths (in rect_size_diffs)
        rect_size_diffs = {}
        for b in beat_indeces_changed:
            while b >= len(self.rendered_beat_widths):
                self.rendered_beat_widths.append(0)

            new_max = 6 # width will not be less than this
            for c, channel in enumerate(self.channel_rects):
                for i, rect_line in enumerate(channel):
                    new_max = max(self.rect_beats[(c, i, b)].width, new_max)

            old_max = self.rendered_beat_widths[b]
            if new_max != old_max:
                rect_size_diffs[b] = new_max - old_max
                self.rendered_beat_widths[b] = new_max

        ## Then, merge consecutive beats in rect_size_diffs.
        ##  Having fewer sections therein means fewer calls to shift_contents_in_box(...)
        rect_size_diffs_tmp = {}
        diff_keys = list(rect_size_diffs.keys())
        diff_keys.sort()
        skip_countdown = 0
        for k in diff_keys[::-1]:
            # Skip the next number of keys as they've been merged already
            if skip_countdown:
                skip_countdown -= 1
                continue

            rect_size_diffs_tmp[k + 1] = 0

            j = k
            while j >= 0 and j in rect_size_diffs:
                # Increase offset of proceeding beat
                rect_size_diffs_tmp[k + 1] += rect_size_diffs[j]
                skip_countdown += 1
                j -= 1
        rect_size_diffs = rect_size_diffs_tmp

        ## Then, Shift groups of beats that would be moved by beat size changes
        diff_keys = list(rect_size_diffs.keys())
        diff_keys.sort()
        line_length = sum(self.rendered_beat_widths) + len(self.rendered_beat_widths) - 1
        for k in diff_keys:
            offset = rect_size_diffs[k]
            box_limit = (sum(self.rendered_beat_widths[0:max(0, k - 1)]) + k, 0, line_length, 1)
            for channel in self.channel_rects:
                for rect_line in channel:
                    rect_line.shift_contents_in_box(offset, 0, box_limit)
            rect_topbar.shift_contents_in_box(offset, 0, box_limit)

        ## Now move the beat Rects that were specifically rebuilt
        for b in beat_indeces_changed:
            cwidth = self.rendered_beat_widths[b]
            offset = sum(self.rendered_beat_widths[0:b]) + b
            for c, channel in enumerate(self.channel_rects):
                for i, rect_line in enumerate(channel):
                    rect_beat = self.rect_beats[(c, i ,b)]
                    rect_beat.parent.resize(cwidth, 1)
                    rect_beat.move((cwidth - rect_beat.width) // 2, 0)
                    rect_beat.parent.move(offset, 0)
                    if b >= len(self.rendered_beat_widths) - 1:
                        continue

                    if (c,i,b) not in self.rect_beat_lines:
                        rect_beat_line = rect_line.new_rect()
                        self.rect_beat_lines[(c, i, b)] = rect_beat_line
                        rect_beat_line.resize(1, 1)
                        rect_beat_line.set_fg_color(wrecked.BRIGHTBLACK)
                    else:
                        rect_beat_line = self.rect_beat_lines[(c, i, b)]

                    rect_beat_line.set_string(0, 0, chr(9474))
                    rect_beat_line.move(offset + cwidth, 0)

            # update labels at top
            if b not in self.rect_beat_labels:
                rect_label = rect_topbar.new_rect()
                rect_label.set_bg_color(wrecked.BRIGHTBLACK)
                self.rect_beat_labels[b] = rect_label
            else:
                rect_label = self.rect_beat_labels[b]

            label = f"{b:02}"
            rect_label.resize(cwidth, 1)
            rect_label.move(sum(self.rendered_beat_widths[0:b]) + b, 0)
            rect_label.set_string(0, 0, " " * cwidth)
            rect_label.set_string(cwidth - len(label), 0, label)

        return output

    def tick_update_cursor(self) -> bool:
        output = False
        if self.rendered_cursor_position != self.opus_manager.cursor_position:
            output = True
            if self.rendered_cursor_position:
                rect = self.get_subbeat_rect(self.rendered_cursor_position)
                rect.unset_invert()

            rect = self.get_subbeat_rect(self.opus_manager.cursor_position)
            rect.invert()

            self.rendered_cursor_position = self.opus_manager.cursor_position.copy()

        return output

    def get_line_count(self):
        output = 0
        for channel in self.channel_rects:
            for _line in channel:
                output += 1
        return output

    def tick_update_lines(self) -> bool:
        line_length = sum(self.rendered_beat_widths) + self.opus_manager.opus_beat_count - 1
        line_count = self.get_line_count()
        output = False
        if line_length != self.rendered_line_length or line_count != self.rendered_line_count:
            new_height = 0
            line_labels = []
            for c, channel in enumerate(self.channel_rects):
                for i, rect_line in enumerate(channel):
                    line_position = self.opus_manager.get_y(c, i)
                    rect_line.set_fg_color(wrecked.BLUE)
                    rect_line.resize(line_length, 1)
                    rect_line.move(0, line_position)
                    line_labels.append((c,i))
                    new_height += 1 # Add space for this line

                if channel:
                    new_height += 1 # Add space for divider
                    rect_channel_divider = self.rect_channel_dividers[c]
                    rect_channel_divider.set_fg_color(wrecked.BRIGHTBLACK)
                    rect_channel_divider.resize(line_length, 1)
                    rect_channel_divider.set_string(0, 0, chr(9472) * line_length)
                    rect_channel_divider.move(0, self.opus_manager.get_y(c, len(channel) - 1) + 1)

            self.frame_line_labels.resize(5, min(self.rect_view.height - 3, new_height - 1))
            self.frame_line_labels.move(0, 1)

            rect_line_labels = self.frame_line_labels.get_content_rect()
            rect_line_labels.resize(self.frame_line_labels.width, new_height)
            rect_line_labels.clear_characters()

            y_offset = 0
            prev_c = None
            for y, (c, i) in enumerate(line_labels):
                if prev_c != c and y != 0:
                    y_offset += 1

                if c != prev_c:
                    strlabel = f"{c:02}:{i:02}"
                else:
                    strlabel = f"  :{i:02}"

                rect_line_labels.set_string(0, y + y_offset, strlabel)


                prev_c = c

            rect_content = self.frame_content.get_content_rect()
            rect_content.resize(line_length, new_height)
            self.frame_topbar.get_content_rect().resize(line_length, 1)

            self.rendered_line_length = line_length
            self.rendered_line_count = line_count

            output = True

        return output

    def get_subbeat_rect(self, position):
        c, i = self.opus_manager.get_channel_index(position[0])
        return self.subbeat_rect_map[(c, i, position[1])][tuple(position[2:])]

    def build_beat_rect(self, beat_grouping, rect) -> Dict[Tuple(int), wrecked.Rect]:
        # Single-event or empty beats get a buffer rect for spacing purposes
        stack = [(beat_grouping, rect, 0, [])]

        depth_sorted_queue = []
        flat_map = {}
        while stack:
            working_grouping, working_rect, depth, cursor_map = stack.pop(0)

            depth_sorted_queue.append((depth, working_grouping, tuple(cursor_map)))
            flat_map[tuple(cursor_map)] = working_rect
            if working_grouping.is_structural():
                for i, subgrouping in enumerate(working_grouping):
                    next_map = cursor_map.copy()
                    next_map.append(i)
                    stack.append((subgrouping, working_rect.new_rect(), depth + 1, next_map))

        depth_sorted_queue = sorted(depth_sorted_queue, key=sort_by_first, reverse=True)

        for _, working_grouping, cursor_path in depth_sorted_queue:
            working_rect = flat_map[cursor_path]

            if working_grouping.is_structural():
                running_width = 0
                if working_grouping != beat_grouping:
                    running_width += 1

                comma_points = []
                for i, subgrouping in enumerate(working_grouping):
                    child_path = list(cursor_path)
                    child_path.append(i)
                    child_rect = flat_map[tuple(child_path)]
                    # Account for commas
                    if i != 0:
                        comma_points.append(running_width)
                        running_width += 1

                    child_rect.move(running_width, 0)
                    running_width += child_rect.width

                if working_grouping != beat_grouping:
                    running_width += 1

                working_rect.resize(running_width, 1)
                if working_grouping != beat_grouping:
                    working_rect.set_character(0, 0, '[')
                    working_rect.set_character(running_width - 1, 0, ']')

                for x in comma_points:
                    working_rect.set_character(x, 0, '/')

            else:
                is_relative = False
                is_negative = False
                if working_grouping.is_event():
                    events = working_grouping.get_events()
                    new_string = ''
                    for i, event in enumerate(events):
                        base = event.get_base()
                        if event.relative:
                            is_relative = True
                            if event.note == 0 or event.note % base != 0:
                                if event.note < 0:
                                    new_string += f"-"
                                    is_negative = True
                                else:
                                    new_string += f"+"
                                new_string += get_number_string(int(math.fabs(event.note)), base)
                            else:
                                if event.note < 0:
                                    new_string += chr(8595)
                                    is_negative = True
                                else:
                                    new_string += chr(8593)
                                new_string += get_number_string(int(math.fabs(event.note)) // base, base)
                        else:
                            note = event.note
                            new_string = get_number_string(note, base)
                else:
                    new_string = '..'

                working_rect.resize(len(new_string), 1)
                working_rect.set_string(0, 0, new_string)
                if is_relative:
                    if is_negative:
                        working_rect.set_fg_color(wrecked.BLUE)
                    else:
                        working_rect.set_fg_color(wrecked.BRIGHTYELLOW)

        #structured_map = {}
        #for path, rect in flat_map.items():
        #    working_node = structured_map
        #    for p in path:
        #        if p not in working_node:
        #            working_node[p] = {}
        #        working_node = working_node[p]
        #    working_node['rect'] = rect

        return flat_map

    def split_grouping(self, splits):
        position = self.opus_manager.cursor_position
        self.opus_manager.split_grouping(position, splits)

    def set_event_at_cursor(self):
        register = self.fetch_register()
        if register is None:
            return

        position = self.opus_manager.cursor_position
        self.opus_manager.set_beat_event(register.value, position, relative=register.relative)

        c, i = self.opus_manager.get_channel_index(position[0])
        self.flag_beat_changed.add((c, i, position[1]))
        self.rendered_cursor_position = None

    def insert_after_cursor(self):
        position = self.opus_manager.cursor_position
        self.opus_manager.insert_after(position)

        if len(position) == 2:
            self.opus_manager.cursor_dive(0)

        c, i = self.opus_manager.get_channel_index(position[0])
        self.flag_beat_changed.add((c, i, position[1]))
        self.rendered_cursor_position = None

    def unset_at_cursor(self):
        self.opus_manager.unset_at_cursor()
        position = self.opus_manager.cursor_position
        c, i = self.opus_manager.get_channel_index(position[0])
        self.flag_beat_changed.add((c, i, position[1]))
        self.rendered_cursor_position = None

    def remove_at_cursor(self):
        self.opus_manager.remove_at_cursor()

        position = self.opus_manager.cursor_position
        c, i = self.opus_manager.get_channel_index(position[0])
        self.flag_beat_changed.add((c, i, position[1]))
        self.rendered_cursor_position = None

    def cursor_left(self):
        self.clear_register()
        self.opus_manager.cursor_left()

    def cursor_up(self):
        self.clear_register()
        self.opus_manager.cursor_up()

    def cursor_down(self):
        self.clear_register()
        self.opus_manager.cursor_down()

    def cursor_right(self):
        self.clear_register()
        self.opus_manager.cursor_right()

    def swap_channels(self, channel_a, channel_b):
        self.opus_manager.swap_channels(channel_a, channel_b)
        tmp = self.channel_rects[channel_b]
        self.channel_rects[channel_b] = self.channel_rects[channel_a]
        self.channel_rects[channel_a] = tmp


        # Swap Rect Beats
        to_delete = []
        for k, rect in self.rect_beats.items():
            if k[0] not in (channel_a, channel_b):
                continue
            to_delete.append((k, rect))

        while to_delete:
            (i,j,k), rect  = to_delete.pop()
            rect.remove()
            del self.rect_beats[(i,j,k)]
            if i == channel_a:
                i = channel_b
            else:
                i = channel_a
            self.flag_beat_changed.add((i,j,k))

        # Swap Rect Subbeats
        for k, subbeatmap in self.subbeat_rect_map.items():
            if not k[0] in (channel_a, channel_b):
                continue
            to_delete.append((k, subbeatmap))

        while to_delete:
            (i, j, k), subbeat_map  = to_delete.pop()
            del self.subbeat_rect_map[(i,j,k)]

        # TODO: Maybe use a function to force instead of setting this arbitrary variable
        self.rendered_line_length = None


    def run(self):
        self.running = True
        thread = threading.Thread(target=self.daemon_input)
        thread.start()
        while self.running:
            try:
                self.tick()
                time.sleep(self.tick_delay)
            except KeyboardInterrupt:
                self.running = False


def parse_kwargs(*args):
    output = {}
    skip_flag = False
    for i, arg in enumerate(args):
        if skip_flag:
            skip_flag = False
            continue

        if arg.startswith('--'):
            output[arg[2:]] = args[i + 1]
            skip_flag = True
    return output


def sort_by_first(a):
    return a[0]
