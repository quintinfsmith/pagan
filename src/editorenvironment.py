from __future__ import annotations
import math
import time
import threading

from inspect import signature
from enum import Enum, auto
from typing import Optional, Dict, List, Tuple

import wrecked
from .wrecked_elements import RectFrame
from .mgrouping import get_number_string
from .opusmanager import OpusManager

class InvalidCursor(Exception):
    '''Raised when attempting to pass a cursor without enough arguments'''

class EditorEnvironment:
    tick_delay = 1 / 24

    def daemon_input(self):
        interactor = self.opus_manager.interactor
        while self.running:
            interactor.get_input()
        interactor.restore_input_settings()

    def __init__(self):
        self.running = False
        self.root = wrecked.init()

        self.register = None
        #self.rendered_register = None
        #self.rect_register = self.rect_view_window.new_rect()

        self.opus_manager = OpusManager()
        self.channel_rects = [[] for i in range(16)]
        self.block_tick = False

        self.rendered_command_register = None

        self.rendered_channel_rects = set()
        self.rendered_beat_widths = []

        self.rendered_cursor_position = []
        self.force_cursor_update = False
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

    def kill(self):
        self.running = False
        wrecked.kill()

    def tick(self):
        flag_draw = False
        self.tick_update_context()
        if not self.block_tick:
            flag_draw |= self.tick_update_beats()
            flag_draw |= self.tick_update_lines()
            flag_draw |= self.tick_update_cursor()
            flag_draw |= self.tick_update_view_offset()
            #flag_draw |= self.tick_update_register()
            flag_draw |= self.tick_update_command_register()

            if flag_draw:
                self.root.draw()

    def tick_update_context(self):
        self.opus_manager.update_context()


    def tick_update_command_register(self) -> bool:
        output = False
        opus_manager = self.opus_manager
        command_ledger = opus_manager.command_ledger

        cmd_register = command_ledger.get_register()
        cmd_error = command_ledger.get_error_msg()
        if cmd_error is not None:
            cmd_msg = cmd_error
        elif cmd_register is not None:
            cmd_msg = cmd_register
        else:
            cmd_msg = None

        if self.rendered_command_register != cmd_msg:
            output = True
            if cmd_msg is not None:
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
                        2 + len(cmd_msg),
                        self.frame_command_register.full_width - 2
                    ),
                    1
                )

                if cmd_error is not None:
                    rect_content.set_string(0, 0, f"{cmd_msg}")
                    rect_content.set_fg_color(wrecked.RED)
                else:
                    rect_content.set_string(0, 0, f":{cmd_msg}_")
                    rect_content.unset_fg_color()

               # self.rect_content_wrapper.resize(self.rect_content_wrapper.width, self.rect_view.height - 5)
            else:
                self.frame_command_register.detach()
               # self.rect_content_wrapper.resize(self.rect_content_wrapper.width, self.rect_view.height - 2)

            self.rendered_command_register = cmd_msg

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
        if self.force_cursor_update or self.rendered_cursor_position != self.opus_manager.cursor_position:
            self.force_cursor_update = False
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

        return flat_map

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
