from __future__ import annotations
import math
import os
import re
import time
import threading

from typing import Optional, Dict, List, Tuple
import wrecked
from apres import MIDI

from structures import BadStateError
from mgrouping import MGrouping, MGroupingEvent
from interactor import Interactor

class InvalidCursor(Exception):
    '''Raised when attempting to pass a cursor without enough arguments'''

class EditorEnvironment:
    tick_delay = 1 / 24

    def daemon_input(self):
        while self.running:
            self.interactor.get_input()
        self.interactor.restore_input_settings()

    def __init__(self):
        self.running = False
        self.root = wrecked.init()
        self.rect_view_window = self.root.new_rect()
        self.rect_view_shifter = self.rect_view_window.new_rect()

        self.register = None
        self.channel_rects = [[] for i in range(16)]
        self.opus_manager = OpusManager()
        self.interactor = Interactor()
        self.interactor.assign_sequence(
            'l',
            self.cursor_right
        )
        self.interactor.assign_sequence(
            'h',
            self.cursor_left
        )
        self.interactor.assign_sequence(
            'j',
            self.cursor_down
        )
        self.interactor.assign_sequence(
            'k',
            self.cursor_up
        )
        self.interactor.assign_sequence(
            'q',
            self.kill
        )
        self.interactor.assign_sequence(
            'x',
            self.remove_at_cursor
        )
        self.interactor.assign_sequence(
            'i',
            self.insert_after_cursor
        )
        self.interactor.assign_sequence(
            ' ',
            self.insert_beat_at_cursor
        )
        self.interactor.assign_sequence(
            '/',
            self.split_at_cursor
        )
        self.interactor.assign_sequence(
            ';]',
            self.new_line
        )
        self.interactor.assign_sequence(
            ';[',
            self.remove_line
        )

        for c in "0123456789ab":
            self.interactor.assign_sequence(
                c,
                self.add_digit_to_register,
                int(c, 12)
            )

        self.interactor.assign_sequence(
            "\r",
            self.set_event_at_cursor
        )

        self.view_offset = 0

        self.rendered_channel_rects = set()
        self.rendered_beat_widths = []

        self.rendered_cursor_position = []
        self.rendered_line_length = 0
        self.rendered_line_count = 0
        self.rect_beats = {}
        self.rect_beat_lines = {}
        self.rect_topbar = self.rect_view_shifter.new_rect()
        self.rect_beat_labels = {}
        self.subbeat_rect_map = {}
        self.rect_channel_dividers = {}

        self.flag_beat_changed = set()
        self.flag_line_changed = set()

    def new_line(self):
        cursor = self.opus_manager.cursor_position
        channel, _i = self.opus_manager.get_channel_index(cursor[0])

        self.opus_manager.new_line(channel)

        grouping_rect = self.rect_view_shifter.new_rect()
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
        if splits := self.fetch_register(0):
            self.opus_manager.split(cursor, splits)
            self.opus_manager.cursor_position.append(0)

            c, i = self.opus_manager.get_channel_index(cursor[0])
            self.flag_beat_changed.add((c, i, cursor[1]))
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
            self.register = 0
        else:
            self.register *= self.opus_manager.BASE
        self.register += value

    def clear_register(self):
        self.register = None

    def fetch_register(self, fallback=None):
        if self.register is None:
            output = fallback
        else:
            output = self.register
            self.register = None

        return output

    def kill(self):
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
                grouping_rect = self.rect_view_shifter.new_rect()
                self.channel_rects[c].append(grouping_rect)
                #self.flag_line_changed.add((c,i))
                for b, _ in enumerate(grouping):
                    self.flag_beat_changed.add((c, i, b))

            if channel:
                self.rect_channel_dividers[c] = self.rect_view_shifter.new_rect()

    def tick(self):
        flag_draw = False
        flag_draw |= self.tick_update_beats()
        flag_draw |= self.tick_update_lines()
        flag_draw |= self.tick_update_cursor()
        flag_draw |= self.tick_update_view_offset()

        if flag_draw:
            self.root.draw()

    def tick_update_view_offset(self) -> bool:
        did_change_flag = False
        if self.root.width - 4 != self.rect_view_window.width:
            self.rect_view_window.resize(self.root.width - 4, self.root.height)
            self.rect_view_window.move(4, 0)

            #Draw view border
            vh = self.rect_view_window.height
            vw = self.rect_view_window.width
            for y in range(vh - 2):
                self.rect_view_window.set_string(0, y + 1, chr(9474))
                self.rect_view_window.set_string(vw - 1, y + 1, chr(9474))

            for x in range(vw - 2):
                self.rect_view_window.set_string(x + 1, 0, chr(9472))
                self.rect_view_window.set_string(x + 1, vh - 1, chr(9472))

            self.rect_view_window.set_string(0, 0, chr(9581))
            self.rect_view_window.set_string(0, vh - 1, chr(9584))
            self.rect_view_window.set_string(vw - 1, 0, chr(9582))
            self.rect_view_window.set_string(vw - 1, vh - 1, chr(9583))

            self.rect_view_shifter.resize(vw - 2, vh - 2)
            self.rect_view_shifter.move(1, 1)
            did_change_flag = True


        cursor = self.opus_manager.cursor_position
        beat_offset = sum(self.rendered_beat_widths[0:cursor[1]]) + cursor[1] - 1
        line_length = sum(self.rendered_beat_widths) + len(self.rendered_beat_widths) - 1
        beat_width = self.rendered_beat_widths[cursor[1]]

        new_offset = beat_offset - ((self.rect_view_shifter.width - beat_width) // 3)
        new_offset = max(0, new_offset)
        new_offset = min(line_length - self.rect_view_shifter.width, new_offset)

        if did_change_flag or new_offset != self.view_offset:
            shift_diff = self.view_offset - new_offset
            self.view_offset = new_offset
            self.rect_view_shifter.shift_contents(shift_diff, 0)
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
                self.rect_beats[(c, i, b)].remove()
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
            for line in channel:
                output += 1
        return output

    def tick_update_lines(self) -> bool:
        line_length = sum(self.rendered_beat_widths) + self.opus_manager.opus_beat_count - 1
        line_count = self.get_line_count()
        output = False
        if line_length != self.rendered_line_length or line_count != self.rendered_line_count:
            for c, channel in enumerate(self.channel_rects):
                for i, rect_line in enumerate(channel):
                    line_position = self.opus_manager.get_y(c, i) + 1
                    rect_line.set_fg_color(wrecked.BLUE)
                    rect_line.resize(line_length, 1)
                    rect_line.move(0 - self.view_offset, line_position)
                    if i == 0:
                        self.root.set_string(0, line_position + 1, f"{c}:{i} ")
                    else:
                        self.root.set_string(0, line_position + 1, f" :{i} ")

                if channel:
                    rect_channel_divider = self.rect_channel_dividers[c]
                    rect_channel_divider.set_fg_color(wrecked.BRIGHTBLACK)
                    rect_channel_divider.resize(line_length, 1)
                    rect_channel_divider.set_string(0, 0, chr(9472) * line_length)
                    rect_channel_divider.move(0 - self.view_offset, self.opus_manager.get_y(c, len(channel) - 1) + 2)

            self.rect_topbar.resize(line_length, 1)
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
                    working_rect.set_character(x, 0, ',')

            else:
                if working_grouping.is_event():
                    events = working_grouping.get_events()
                    new_string = ''
                    for i, event in enumerate(events):
                        base = event.get_base()
                        if event.relative:
                            if event.note == 0 or event.note % base != 0:
                                if event.note < 0:
                                    new_string += f"-"
                                else:
                                    new_string += f"+"
                                new_string += get_digit(int(math.fabs(event.note)), base)
                            else:
                                if event.note < 0:
                                    new_string += f"v"
                                else:
                                    new_string += f"^"
                                new_string += get_digit(int(math.fabs(event.note)) // base, base)
                        else:
                            note = event.get_note()
                            new_string += get_digit(note // base, base)
                            new_string += get_digit(note % base, base)
                else:
                    new_string = '--'

                working_rect.resize(len(new_string), 1)
                working_rect.set_string(0, 0, new_string)

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
        value = self.fetch_register()
        if value is None:
            return

        position = self.opus_manager.cursor_position
        self.opus_manager.set_beat_event(value, position)

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
        self.opus_manager.cursor_left()
    def cursor_up(self):
        self.opus_manager.cursor_up()
    def cursor_down(self):
        self.opus_manager.cursor_down()
    def cursor_right(self):
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

    def set_beat_event(self, value, position):
        channel, _index = self.get_channel_index(position[0])
        grouping = self.get_grouping(position)
        if grouping.is_structural():
            grouping.clear()
        elif grouping.is_event():
            grouping.clear_events()

        grouping.add_event(MGroupingEvent(
            value,
            base=self.BASE,
            channel=channel
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


def split_by_channel(event, other_events):
    return event['channel']

def split_by_note_order(event, other_events):
    e_notes = [e.note for e in other_events]
    e_notes.sort()
    return e_notes.index(event.note)

def sort_by_first(a):
    return a[0]

def get_digit(decimal_number, base):
    return "0123456789ABCDEFGHIJKLMNOPQRSTUVWXZ"[decimal_number % base]
