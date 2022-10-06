from __future__ import annotations
import math
import os
import re
import time
import threading

from enum import Enum, auto

from typing import Optional, Dict, List, Tuple
import wrecked
from apres import MIDI

from structures import BadStateError
from mgrouping import MGrouping, MGroupingEvent, get_number_string
from interactor import Interactor

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

        self.view_offset = 0

        self.command_map = {
            'w': self.save,
            'q': self.kill
        }
        self.command_register = None
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
        self.rect_command_register = self.rect_view.new_rect()
        self.rect_command_register.detach()

        self.rect_line_labels_wrapper = self.rect_view.new_rect()
        self.rect_line_labels = self.rect_line_labels_wrapper.new_rect()
        self.rect_content_wrapper = self.rect_view.new_rect()
        self.rect_content = self.rect_content_wrapper.new_rect()
        self.rect_topbar = self.rect_content.new_rect()


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
            'x',
            self.remove_at_cursor
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

        # KLUDGE FOR TESTING
        self.interactor.assign_context_sequence(
            InputContext.Default,
            'EE',
            self.opus_manager.export
        )
        self.interactor.assign_context_sequence(
            InputContext.Default,
            'ES',
            self.opus_manager.save
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
            self.command_register_open
        )

        for c in range(32, 127):
            self.interactor.assign_context_sequence(
                InputContext.Text,
                chr(c),
                self.command_register_input,
                chr(c)
            )
        self.interactor.assign_context_sequence(
            InputContext.Text,
            "\x7F",
            self.command_register_backspace
        )

        self.interactor.assign_context_sequence(
            InputContext.Text,
            "\x1B",
            self.command_register_close
        )

        self.interactor.assign_context_sequence(
            InputContext.Text,
            "\r",
            self.command_register_run
        )

        #self.interactor.assign_context_sequence(
        #    "\x7f",
        #    self.remove_last_digit_from_register
        #)

    def save(self, *args):
        self.opus_manager.save()

    def command_register_run(self):
        if self.command_register is None:
            return
        cmd_parts = self.command_register.split(" ")
        if cmd_parts[0] in self.command_map:
            self.command_map[cmd_parts[0]](*cmd_parts[1:])
        self.command_register_close()

    def command_register_open(self):
        self.command_register = ""
        self.interactor.set_context(InputContext.Text)

    def command_register_close(self):
        self.command_register = None
        self.interactor.set_context(InputContext.Default)

    def command_register_input(self, character):
        if self.command_register is None:
            return
        self.command_register += character

    def command_register_backspace(self):
        if self.command_register is None:
            return

        if self.command_register:
            self.command_register = self.command_register[0:-1]
        else:
            self.command_register_close()

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

        grouping_rect = self.rect_content.new_rect()
        self.channel_rects[channel].append(grouping_rect)
        for b in range(self.opus_manager.opus_beat_count):
            self.flag_beat_changed.add((channel, len(self.channel_rects[channel]) - 1, b))

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
            for b in range(self.opus_manager.opus_beat_count):
                self.flag_beat_changed.add((target_c, i, b))

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

        for (c,i,b) in self.rect_beats:
            if b >= self.opus_manager.opus_beat_count:
                pass
                #del self.rect_beats[(c, i, b)]
            elif b >= cursor[1]:
                self.flag_beat_changed.add((c, i, b))

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
            self.cursor_right()
        else:
            self.register.value *= self.opus_manager.BASE
            self.register.value += value
            if self.register.value >= self.opus_manager.BASE:
                self.set_event_at_cursor()
                self.cursor_right()

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
        if os.path.isdir(path):
            self.opus_manager.load_folder(path)
        elif path[path.rfind("."):].lower() == ".mid":
            self.opus_manager.import_midi(path)
        else:
            self.opus_manager.load_file(path)

        # TODO: Could be cleaner
        cursor_position = [0,0]
        grouping = self.opus_manager.get_grouping(cursor_position)
        while grouping.is_structural():
            cursor_position.append(0)
            grouping = grouping[0]
        self.opus_manager.cursor_position = cursor_position

        for c, channel in enumerate(self.opus_manager.channel_groupings):
            for i, grouping in enumerate(channel):
                grouping_rect = self.rect_content.new_rect()
                self.channel_rects[c].append(grouping_rect)
                #self.flag_line_changed.add((c,i))
                for b, _ in enumerate(grouping):
                    self.flag_beat_changed.add((c, i, b))

            if channel:
                self.rect_channel_dividers[c] = self.rect_content.new_rect()


    def tick(self):
        flag_draw = False
        flag_draw |= self.tick_update_beats()
        flag_draw |= self.tick_update_lines()
        flag_draw |= self.tick_update_cursor()
        flag_draw |= self.tick_update_view_offset()
        #flag_draw |= self.tick_update_register()
        flag_draw |= self.tick_update_command_register()
        flag_draw |= self.tick_update_borders()

        if flag_draw:
            self.root.draw()

    def tick_update_borders(self):
        output = False
        content_size = (
            self.rect_content_wrapper.width,
            self.rect_content_wrapper.height
        )
        line_labels_size = (
            self.rect_line_labels_wrapper.width,
            self.rect_line_labels_wrapper.height
        )
        if self.command_register is not None:
            command_register_size = (
                self.rect_command_register.width,
                self.rect_command_register.height
            )
        else:
            command_register_size = None

        if self.rendered_content_size != content_size \
        or self.rendered_command_register_size != command_register_size \
        or self.rendered_line_labels_size != line_labels_size:
            self.rect_view.clear_characters()

            draw_border_around_rect(self.rect_line_labels_wrapper)
            draw_border_around_rect(self.rect_content_wrapper)
            if command_register_size is not None:
                draw_border_around_rect(self.rect_command_register)


            self.rendered_content_size = content_size
            self.rendered_command_register_size = command_register_size
            self.rendered_line_labels_size = line_labels_size
            output = True

        return output

    def tick_update_command_register(self) -> bool:
        output = False
        if self.rendered_command_register != self.command_register:
            output = True
            if self.command_register is not None:
                if self.rendered_command_register is None:
                    self.rect_view.attach(self.rect_command_register)
                self.rect_command_register.clear_children()
                self.rect_command_register.clear_characters()

                self.rect_command_register.resize(self.rect_view.width - 2, 1)
                self.rect_command_register.move(1, self.rect_view.height - 2)

                self.rect_command_register.set_string(0, 0, f":{self.command_register}_")

                self.rect_content_wrapper.resize(self.rect_content_wrapper.width, self.rect_view.height - 5)
            else:
                self.rect_command_register.detach()
                self.rect_content_wrapper.resize(self.rect_content_wrapper.width, self.rect_view.height - 2)

            self.rendered_command_register = self.command_register

        return output

    def tick_update_view_offset(self) -> bool:
        did_change_flag = False
        if self.rect_view.width - (self.rect_line_labels_wrapper.width + 2) != self.rect_content_wrapper.width + 2:
            self.rect_content_wrapper.resize(
                self.rect_view.width - (self.rect_line_labels_wrapper.width + 2) - 2,
                self.rect_view.height - 2
            )

            self.rect_content_wrapper.move((self.rect_line_labels_wrapper.width + 2) + 1, 1)

            did_change_flag = True

        cursor = self.opus_manager.cursor_position
        beat_offset = sum(self.rendered_beat_widths[0:cursor[1]]) + cursor[1] - 1
        line_length = sum(self.rendered_beat_widths) + len(self.rendered_beat_widths) - 1
        beat_width = self.rendered_beat_widths[cursor[1]]

        new_offset = beat_offset - ((self.rect_content_wrapper.width - beat_width) // 3)
        new_offset = max(0, new_offset)
        new_offset = min(line_length - self.rect_content_wrapper.width, new_offset)

        if did_change_flag or new_offset != self.view_offset:
            shift_diff = self.view_offset - new_offset
            self.view_offset = new_offset
            self.rect_content.move(0 - self.view_offset, 0)
            did_change_flag = True

        return did_change_flag

    def tick_update_beats(self) -> bool:
        channels = self.opus_manager.channel_groupings
        output = bool(self.flag_beat_changed)

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
            self.rect_topbar.shift_contents_in_box(offset, 0, box_limit)


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
                rect_label = self.rect_topbar.new_rect()
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
            new_height = 1
            line_labels = []
            for c, channel in enumerate(self.channel_rects):
                for i, rect_line in enumerate(channel):
                    # '+ 1' is reserved space for topbar
                    line_position = self.opus_manager.get_y(c, i) + 1
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
                    rect_channel_divider.move(0, self.opus_manager.get_y(c, len(channel) - 1) + 2)

            self.rect_line_labels_wrapper.move(1, 2)
            self.rect_line_labels_wrapper.resize(5, new_height - 2)
            self.rect_line_labels.resize(self.rect_line_labels_wrapper.width, new_height)
            self.rect_line_labels.clear_characters()
            y_offset = 0
            prev_c = None
            for y, (c, i) in enumerate(line_labels):
                if prev_c != c and y != 0:
                    y_offset += 1

                if c != prev_c:
                    strlabel = f"{c:02}:{i:02}"
                else:
                    strlabel = f"  :{i:02}"

                self.rect_line_labels.set_string(0, y + y_offset, strlabel)


                prev_c = c

            self.rect_content.resize(line_length, new_height)
            self.rect_topbar.resize(self.rect_content.width, 1)

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
                    new_string = '  '

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

class OpusManager:
    BASE = 12
    def __init__(self):
        self.channel_groupings = [[] for i in range(16)]
        self.channel_order = list(range(16))
        self.cursor_position = [0, 0] # Y, X,... deeper divisions
        self.opus_beat_count = 1
        self.path = None
        self.clipboard_grouping = None

    def copy_grouping(self, position):
        self.clipboard_grouping = self.get_grouping(position)

    def replace_grouping(self, position, grouping):
        self.get_grouping(position).replace_with(grouping)

    def paste_grouping(self, position):
        if self.clipboard_grouping is None:
            return

        self.replace_grouping(position, self.clipboard_grouping)
        self.clipboard_grouping = None

    def save(self):
        if self.path.lower().endswith("mid"):
            working_path = self.path[0:self.path.rfind("/")]
            working_dir = self.path[self.path.rfind("/") + 1:self.path.rfind(".")]
        elif os.path.isfile(self.path):
            working_path = self.path[0:self.path.rfind("/")]
            working_dir = self.path[self.path.rfind("/") + 1:self.path.rfind(".")]
        elif os.path.isdir(self.path):
            tmp_split = self.path.split("/")
            working_path = "/".join(tmp_split[0:-1])
            working_dir = tmp_split[-1]

        fullpath = f"{working_path}/{working_dir}"
        if not os.path.isdir(fullpath):
            os.mkdir(fullpath)

        for f in os.listdir(fullpath):
            os.remove(f"{fullpath}/{f}")

        for c, channel_lines in enumerate(self.channel_groupings):
            if not channel_lines:
                continue

            strlines = []
            for line in channel_lines:
                strlines.append(line.to_string())
            n = "0123456789ABCDEF"[c]
            with open(f"{fullpath}/channel_{n}", "w") as fp:
                fp.write("\n".join(strlines))


    def export(self):
        working_path = self.path
        if self.path.lower().endswith("mid"):
            working_path = f"{self.path[0:self.path.rfind('.mid')]}_radix.mid"
        elif os.path.isfile(working_path):
            working_path = f"{self.path[0:self.path.rfind('.mid')]}.mid"
        elif os.path.isdir(working_path):
            working_path = f"{self.path}.mid"

        opus = MGrouping()
        opus.set_size(self.opus_beat_count)
        for groupings in self.channel_groupings:
            for grouping in groupings:
                for b, beat in enumerate(grouping):
                    opus[b].merge(beat)
        opus.to_midi().save(working_path)

    def set_active_beat_size(self, size):
        self.set_beat_size(size, self.cursor_position)

    def get_active_line(self):
        return self.get_line(self.cursor_position[0])

    def cursor_left(self):
        fully_left = True
        while True:
            if self.cursor_position[-1] > 0:
                self.cursor_position[-1] -= 1
                fully_left = False
                break
            elif len(self.cursor_position) > 2:
                self.cursor_position.pop()
            else:
                break

        while (grouping := self.get_grouping(self.cursor_position)).is_structural():
            if not fully_left:
                self.cursor_position.append(len(grouping) - 1)
            else:
                # Move back to fully left position
                self.cursor_position.append(0)

    def cursor_right(self):
        fully_right = True
        while True:
            parent_grouping = self.get_grouping(self.cursor_position[0:-1])
            if self.cursor_position[-1] < len(parent_grouping) - 1:
                self.cursor_position[-1] += 1
                fully_right = False
                break
            elif len(self.cursor_position) > 2:
                self.cursor_position.pop()
            else:
                break

        while (grouping := self.get_grouping(self.cursor_position)).is_structural():
            if not fully_right:
                self.cursor_position.append(0)
            else:
                # Move back to fully right position
                self.cursor_position.append(len(grouping) - 1)

    def cursor_up(self):
        self.cursor_position[0] = max(0, self.cursor_position[0] - 1)
        while len(self.cursor_position) > 2:
            try:
                self.get_grouping(self.cursor_position)
                break
            except InvalidCursor:
                self.cursor_position.pop()
            except IndexError:
                self.cursor_position.pop()

        while self.get_grouping(self.cursor_position).is_structural():
            self.cursor_position.append(0)

    def cursor_down(self):
        if self.get_line(self.cursor_position[0] + 1) is not None:
            self.cursor_position[0] += 1

        while len(self.cursor_position) > 2:
            try:
                self.get_grouping(self.cursor_position)
                break
            except InvalidCursor:
                self.cursor_position.pop()
            except IndexError:
                self.cursor_position.pop()

        while self.get_grouping(self.cursor_position).is_structural():
            self.cursor_position.append(0)

    def cursor_climb(self):
        if len(self.cursor_position) > 2:
            self.cursor_position.pop()

    def cursor_dive(self, index):
        if index >= len(self.get_grouping(self.cursor_position)):
            tmp = self.cursor_position.copy()
            tmp.append(index)
            raise InvalidCursor(tmp)

        self.cursor_position.append(index)

    def get_grouping(self, position):
        grouping = self.get_line(position[0])
        try:
            for i in position[1:]:
                grouping = grouping[i]
        except BadStateError as e:
            raise InvalidCursor from e

        return grouping

    def set_beat_event(self, value, position, *, relative=False):
        channel, _index = self.get_channel_index(position[0])
        grouping = self.get_grouping(position)
        if grouping.is_structural():
            grouping.clear()
        elif grouping.is_event():
            grouping.clear_events()

        grouping.add_event(MGroupingEvent(
            value,
            base=self.BASE,
            channel=channel,
            relative=relative
        ))

    def unset_beat_event(self, position):
        grouping = self.get_grouping(position)
        grouping.clear_events()

    def set_beat_size(self, size, position):
        grouping = self.get_grouping(position)
        grouping.set_size(size, True)

    def set_beat_count(self, new_count):
        self.opus_beat_count = new_count
        for groupings in self.channel_groupings:
            for grouping in groupings:
                grouping.set_size(new_count, True)

    def add_line_to_channel(self, channel=0):
        new_grouping = MGrouping()
        new_grouping.set_size(self.opus_beat_count)
        self.channel_groupings[channel].append(new_grouping)

    def change_line_channel(self, old_channel, line_index, new_channel):
        grouping = self.channel_groupings[old_channel].pop(line_index)
        self.channel_groupings[new_channel].append(grouping)

    def move_line(self, channel, old_index, new_index):
        if old_index == new_index:
            return

        grouping = self.channel_groupings[channel].pop(old_index)
        if new_index == len(grouping) - 1:
            self.channel_groupings[channel].append(grouping)
        else:
            self.channel_groupings[channel].insert(new_index, grouping)

    def set_cursor(self, *position):
        if len(position) < 2:
            raise InvalidCursor(position)
        self.cursor_position = position

    def get_channel(self, y: int) -> int:
        return self.get_channel_index(y)[0]

    def get_channel_index(self, y: int) -> (int, int):
        for channel in self.channel_order:
            for i, _ in enumerate(self.channel_groupings[channel]):
                if y == 0:
                    return (channel, i)
                y -= 1

        raise IndexError

    def get_y(self, c: int, i: int) -> int:
        y = 0
        for j in self.channel_order:
            for g, grouping in enumerate(self.channel_groupings[j]):
                if i == g and j == c:
                    return y
                y += 1
            if self.channel_groupings[j]:
                y += 1
        return -1

    def get_line(self, y) -> Optional[MGrouping]:
        output = None
        for c in self.channel_order:
            for grouping in self.channel_groupings[c]:
                if y == 0:
                    output = grouping
                    break
                y -= 1

            if output is not None:
                break

        return output

    def load_folder(self, path: str) -> None:
        self.path = path
        base = self.BASE

        channel_map = {}
        suffix_patt = re.compile(".*_(?P<suffix>([0-9A-Z]{1,3})?)(\\..*)?", re.I)
        filenames = os.listdir(path)
        filenames_clean = []

        # create a reference map of channels and remove non-suffixed files from the list
        for filename in filenames:
            if filename[filename.rfind("."):] == ".swp":
                continue

            channel = None
            for hit in suffix_patt.finditer(filename):
                channel = int(hit.group('suffix'), 16)

            if channel is not None:
                channel_map[filename] = channel
                filenames_clean.append(filename)

        beat_count = 1
        for filename in filenames_clean:
            channel = channel_map[filename]
            content = ""
            with open(f"{path}/{filename}", 'r') as fp:
                content = fp.read()

            chunks = content.split("\n[")
            for i, chunk in enumerate(chunks):
                if i > 0:
                    chunks[i] = f"[{chunk}"

            for x, chunk in enumerate(chunks):
                grouping = MGrouping.from_string(chunk, base=base, channel=channel)
                if grouping:
                    beat_count = max(len(grouping), beat_count)
                    grouping.set_size(beat_count, True)

                    self.channel_groupings[channel].append(grouping)

        self.opus_beat_count = beat_count


    def load_file(self, path: str) -> MGrouping:
        self.path = path
        base = 12

        content = ""
        with open(path, 'r') as fp:
            content = fp.read()

        chunks = content.split("\n[")
        for i, chunk in enumerate(chunks):
            if i > 0:
                chunks[i] = f"[{chunk}"

        self.opus_beat_count = 1
        for x, chunk in enumerate(chunks):
            grouping = MGrouping.from_string(chunk, base=base, channel=x)
            self.channel_groupings[x].append(grouping)
            self.opus_beat_count = max(self.opus_beat_count, len(grouping))

    def import_midi(self, path: str) -> None:
        self.path = path
        midi = MIDI.load(path)
        opus = MGrouping.from_midi(midi)
        tracks = opus.split(split_by_channel)

        self.opus_beat_count = 1
        for i, mgrouping in enumerate(tracks):
            for _j, split_line in enumerate(mgrouping.split(split_by_note_order)):
                self.channel_groupings[i].append(split_line)
                self.opus_beat_count = max(self.opus_beat_count, len(split_line))

    def split_grouping(self, position: List[int], splits: int):
        grouping = self.get_grouping(position)
        grouping.set_size(splits, True)

    def set_event_note(self, position: List[int], note: int):
        channel = self.get_channel(position[0])
        grouping = self.get_grouping(position)
        grouping.clear_events()
        # TODO: get channel from position instead of mgroupingevent
        grouping.add_event(
            MGroupingEvent(
                note,
                base=self.BASE,
                channel=channel
            )
        )

    def insert_after(self, position: List[int]):
        grouping = self.get_grouping(position)

        if len(position) == 2:
            if grouping.is_event() or grouping.is_open():
                parent = self.get_line(position[0])
                new_beat = MGrouping()
                parent[position[-1]] = new_beat
                new_beat.parent = parent
                new_beat.set_size(2)
                new_beat[0] = grouping
                grouping.parent = new_beat
        else:
            parent = grouping.parent
            at_end = position[-1] == len(parent) - 1
            parent.set_size(len(parent) + 1, True)
            if not at_end:
                tmp = parent[-1]
                i = len(parent) - 1
                while i > position[-1] + 1:
                    parent[i] = parent[i - 1]
                    i -= 1
                parent[i] = tmp
            else:
                pass

    def remove_at_cursor(self):
        position = self.cursor_position
        parent_grouping = self.get_grouping(position[0:-1])
        new_size = len(parent_grouping) - 1
        self.remove(position)

        if new_size < 2 and len(self.cursor_position) > 2:
            self.cursor_position.pop()
        elif position[-1] == new_size:
            self.cursor_position[-1] -= 1

    def remove(self, position: List[int]):
        index = position[-1]
        grouping = self.get_grouping(position)
        parent = grouping.parent
        new_size = len(parent) - 1

        if new_size > 0 and len(position) > 2:
            for i, child in enumerate(parent):
                if i < index or i == len(parent) - 1:
                    continue
                parent[i] = parent[i + 1]
            parent.set_size(new_size, True)

        # replace the parent with the child
        if new_size == 1:
            parent_index = position[-2]
            parent.parent[parent_index] = parent[0]

    def remove_beat(self, index=None):
        original_beat_count = self.opus_beat_count
        if index is None:
            index = original_beat_count - 1

        for channel in self.channel_groupings:
            for line in channel:
                for i, _beat in enumerate(line[index:-1]):
                    b = i + index
                    line[b] = line[b + 1]

        self.set_beat_count(original_beat_count - 1)

    def insert_beat(self, index=None):
        original_beat_count = self.opus_beat_count
        if index is None:
            index = original_beat_count - 1

        self.set_beat_count(original_beat_count + 1)
        if index >= original_beat_count - 1:
            return


        for channel in self.channel_groupings:
            for line in channel:
                tmp = line[-1]
                i = len(line) - 1
                while i > index:
                    line[i] = line[i - 1]
                    i -= 1
                line[i] = tmp

    def split(self, position, splits):
        grouping = self.get_grouping(position)
        if grouping.is_event():
            parent = self.get_grouping(position[0:-1])
            new_grouping = MGrouping()
            new_grouping.set_size(splits)
            new_grouping.parent = parent
            parent[position[-1]] = new_grouping
            new_grouping[0] = grouping
            grouping.parent = new_grouping
        else:
            grouping.set_size(splits, True)

    def new_line(self, channel=0):
        new_grouping = MGrouping()
        new_grouping.set_size(self.opus_beat_count)
        self.channel_groupings[channel].append(new_grouping)

    def remove_line(self, channel, index):
        self.channel_groupings[channel].pop(index)


def draw_border_around_rect(rect, border=None):
    if border is None:
        border = [
            chr(9581),
            chr(9582),
            chr(9583),
            chr(9584),
            chr(9472),
            chr(9474)
        ]
    parent_rect = rect.parent
    width = rect.width
    height = rect.height
    x_offset = rect.x
    y_offset = rect.y
    for y in range(height):
        parent_rect.set_string(x_offset - 1, y_offset + y, border[5])
        parent_rect.set_string(x_offset + width, y_offset + y, border[5])

    for x in range(width):
        parent_rect.set_string(x_offset + x, y_offset - 1, border[4])
        parent_rect.set_string(x_offset + x, y_offset + height, border[4])

    parent_rect.set_string(x_offset - 1, y_offset - 1, chr(9581))
    parent_rect.set_string(x_offset + width, y_offset - 1, chr(9582))
    parent_rect.set_string(x_offset + width, y_offset + height, chr(9583))
    parent_rect.set_string(x_offset - 1, y_offset + height, chr(9584))

def split_by_channel(event, other_events):
    return event['channel']

def split_by_note_order(event, other_events):
    e_notes = [e.note for e in other_events]
    e_notes.sort()
    return e_notes.index(event.note)

def sort_by_first(a):
    return a[0]
