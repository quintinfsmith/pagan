from __future__ import annotations
import math
import time
import threading

from inspect import signature
from enum import Enum, auto
from typing import Optional, Dict, List, Tuple

import wrecked
from .wrecked_elements import RectFrame
from .opusmanager.mgrouping import get_number_string
from .opusmanager import OpusManager

class InvalidCursor(Exception):
    '''Raised when attempting to pass a cursor without enough arguments'''

class EditorEnvironment:
    tick_delay = 1 / 24

    def __init__(self, opus_manager: OpusManager):
        self.running = False
        self.root = wrecked.init()

        self.register = None
        #self.rendered_register = None
        #self.rect_register = self.rect_view_window.new_rect()

        self.opus_manager = opus_manager
        self.channel_rects = [[] for i in range(16)]
        self.channel_divider_rects = {}

        self.rendered_command_register = None

        self.rendered_channel_rects = set()
        self.rendered_beat_widths = []

        self.rendered_cursor_position = []
        self.force_cursor_update = False
        self.rendered_line_length = 0
        self.rendered_line_count = 0

        # Rect sizes for borders
        self.rendered_frames = { }

        self.rect_beats = {}
        self.rect_beat_lines = {}
        self.rect_beat_labels = []
        self.subbeat_rect_map = {}

        self.rect_view = self.root.new_rect()
        self.rect_view.resize(self.root.width, self.root.height)
        self.rect_view.move(0,0)

        self.frame_line_labels = RectFrame(self.rect_view)
        self.frame_content = RectFrame(self.rect_view)
        self.frame_beat_labels = RectFrame(self.rect_view, False)
        self.frame_command_register = RectFrame(self.rect_view)
        self.frame_command_register.detach()

    def tick_update_size(self) -> bool:
        w, h = wrecked.get_terminal_size()
        output = False
        if h != self.rect_view.height or w != self.rect_view.width:
            self.root.resize(w, h)
            output = True
        return output

    def kill(self):
        self.running = False
        wrecked.kill()

    def tick(self):
        flag_draw = False
        if self.opus_manager.flag_kill:
            self.kill()
            return

        flag_draw |= self.tick_update_size()
        flag_draw |= self.tick_update_frames()

        self.tick_manage_lines()
        self.tick_manage_beats()

        flag_draw |= self.tick_update_beats()
        flag_draw |= self.tick_update_lines()

        flag_draw |= self.tick_update_cursor()
        flag_draw |= self.tick_update_view_offset()

        #flag_draw |= self.tick_update_register()
        flag_draw |= self.tick_update_command_register()

        if flag_draw:
            self.root.draw()


    def tick_manage_lines(self):
        lines = self.opus_manager.fetch('line')
        rect_content = self.frame_content.get_content_rect()
        for channel, index, operation in lines:
            self.rendered_line_count = None
            if operation == "pop":
                if self.opus_manager.get_y(channel, index) == -1:
                    self.rendered_cursor_position = None

                # Remove the beat cells
                for i in range(self.opus_manager.opus_beat_count):
                    rect = self.rect_beats[(channel, index, i)]
                    rect.parent.remove()
                    del rect.parent

                # Remove beat lines
                beat_line_list = self.rect_beat_lines[channel].pop()
                for rect in beat_line_list:
                    if rect.parent is not None:
                        rect.remove()
                    del rect

                # Remove the line Rect
                line = self.channel_rects[channel].pop(index)
                line.remove()
                del line

                # Remove the divider if the channel is empty
                if not self.channel_rects[channel]:
                    self.channel_divider_rects[channel].remove()
                    del self.channel_divider_rects[channel]


            elif operation in ("new", 'init'):
                rect_line = rect_content.new_rect()
                rect_line.set_bg_color(wrecked.BRIGHTBLUE)
                # Add the divider if necessary
                if not self.channel_rects[channel]:
                    self.channel_divider_rects[channel] = rect_content.new_rect()
                    self.rect_beat_lines[channel] = []

                self.rect_beat_lines[channel].append([])

                if len(self.channel_rects[channel]) == index:
                    self.channel_rects[channel].append(rect_line)
                else:
                    self.channel_rects[channel].insert(index, rect_line)

                # all these will be created by the beat flag on init instead of here
                if operation == 'new':
                    for i in range(self.opus_manager.opus_beat_count):
                        rect_beat = rect_line.new_rect()
                        self.rect_beats[(channel, index, i)] = rect_beat.new_rect()

                        rect_beat_line = rect_line.new_rect()
                        rect_beat_line.resize(1, 1)
                        rect_beat_line.set_fg_color(wrecked.BRIGHTBLACK)
                        self.rect_beat_lines[channel][index].append(rect_beat_line)

                for i in range(self.opus_manager.opus_beat_count):
                    self.opus_manager.flag('beat_change', (channel, index, i))

    def tick_manage_beats(self):
        beats = self.opus_manager.fetch('beat')
        for index, operation in beats:
            if operation == 'new':
                adj_beat_rects = []
                for (k_a, k_b, k_c), rect_beat in self.rect_beats.items():
                    if k_c >= index:
                        adj_beat_rects.append(((k_a, k_b, k_c + 1), rect_beat))

                for k, rect in adj_beat_rects:
                    self.rect_beats[k] = rect
                    self.opus_manager.flag('beat_change', k)

                for i, channel in enumerate(self.channel_rects):
                    for j, rect_line in enumerate(channel):
                        rect_beat = rect_line.new_rect()
                        self.rect_beats[(i, j, index)] = rect_beat.new_rect()

                        rect_beat_line = rect_line.new_rect()
                        rect_beat_line.resize(1, 1)
                        rect_beat_line.set_fg_color(wrecked.BRIGHTBLACK)
                        self.rect_beat_lines[i][j].insert(index, rect_beat_line)

                        self.opus_manager.flag('beat_change', (i, j, index))

                rect_topbar = self.frame_beat_labels.get_content_rect()
                rect_label = rect_topbar.new_rect()
                rect_label.set_bg_color(wrecked.BRIGHTBLACK)
                self.rect_beat_labels.append(rect_label)
                self.rendered_beat_widths.insert(index, 0)
            elif operation == 'pop':
                for i, channel in enumerate(self.channel_rects):
                    for j, rect_line in enumerate(channel):
                        self.rect_beats[(i, j, index)].parent.remove()
                        del self.rect_beats[(i, j, index)].parent
                        del self.subbeat_rect_map[(i, j, index)]
                        rect_beat_line = self.rect_beat_lines[i][j].pop()
                        rect_beat_line.remove()

                self.rendered_beat_widths.pop(index)
                if self.rendered_cursor_position[1] == index:
                    self.rendered_cursor_position = None

                # adjust rect_beat map's keys
                adj_beat_rects = []
                for (k_a, k_b, k_c), rect_beat in self.rect_beats.items():
                    if k_c > index:
                        adj_beat_rects.append(((k_a, k_b, k_c - 1), rect_beat))

                for k, rect in adj_beat_rects:
                    self.rect_beats[k] = rect
                    self.opus_manager.flag('beat_change', k)

                # Remove dangling rect entries
                # (the rects got shifted, we just don't need the entries in the dicts)
                k = self.opus_manager.opus_beat_count
                for i, channel in enumerate(self.channel_rects):
                    for j, rect_line in enumerate(channel):
                        if (i,j,k) in self.rect_beats:
                            del self.rect_beats[(i,j,k)]
                        if (i,j,k) in self.subbeat_rect_map:
                            del self.subbeat_rect_map[(i,j,k)]

                self.rect_beat_labels.pop()

    def tick_update_frames(self):
        output = False

        force_all = self.rect_view.width != self.root.width or self.rect_view.height != self.root.height
        if force_all:
            self.rect_view.resize(self.root.width, self.root.height)

        width = self.rect_view.width
        height = self.rect_view.height


        # Draw Register Frame
        command_ledger = self.opus_manager.command_ledger
        if command_ledger.is_open() or command_ledger.is_in_err():
            register = (0, height - 3, width - 2, 1)

            if force_all or register != self.rendered_frames.get('register'):
                self.frame_command_register.attach()
                self.frame_command_register.resize(register[2], register[3])
                self.frame_command_register.move(register[0], register[1])
                self.rendered_frames['register'] = register
                output = True
        else:
            register = (0, height, 0, 0)
            if self.rendered_frames.get('register') is not None:
                self.frame_command_register.detach()
                self.rendered_frames['register'] = None
                output = True

        #Draw Line Labels Frame
        line_labels = (
            0,
            1,
            5,
            self.rect_view.height - register[3] - 3
        )

        if force_all or line_labels != self.rendered_frames.get('line_labels'):
            self.frame_line_labels.resize(line_labels[2], line_labels[3])
            self.frame_line_labels.move(line_labels[0], line_labels[1])
            self.rendered_frames['line_labels'] = line_labels
            output = True

        #Draw Beat Labels Frame
        beat_labels = (
            line_labels[2] + line_labels[0] + 3,
            0,
            self.rect_view.width - self.frame_line_labels.full_width - 2,
            1
        )

        if force_all or beat_labels != self.rendered_frames.get('beat_labels'):
            self.frame_beat_labels.resize(beat_labels[2], beat_labels[3])
            self.frame_beat_labels.move(beat_labels[0], beat_labels[1])
            self.rendered_frames['beat_labels'] = beat_labels
            output = True

        #Draw Content Frame
        content = (
            line_labels[0] + line_labels[2] + 2,
            beat_labels[1] + beat_labels[3],
            beat_labels[2],
            self.rect_view.height - 3
        )

        if force_all or content != self.rendered_frames.get('content'):
            self.frame_content.resize(content[2], content[3])
            self.frame_content.move(content[0], content[1])
            self.rendered_frames['content'] = content
            output = True

        return output

    def tick_update_command_register(self) -> bool:
        output = False
        opus_manager = self.opus_manager
        command_ledger = opus_manager.command_ledger
        if not command_ledger.is_open() and not command_ledger.is_in_err():
            return False

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

            rect_register = self.frame_command_register.get_content_rect()
            rect_register.clear_children()
            rect_register.clear_characters()
            rect_register.resize(
                max(
                    2 + len(cmd_msg),
                    self.frame_command_register.full_width - 2
                ),
                1
            )

            if cmd_error is not None:
                rect_register.set_string(0, 0, f"{cmd_msg}")
                rect_register.set_fg_color(wrecked.RED)
            else:
                rect_register.set_string(0, 0, f":{cmd_msg}_")
                rect_register.unset_fg_color()

            self.rendered_command_register = cmd_msg
        return output


    def tick_update_view_offset(self) -> bool:
        did_change_flag = False

        cursor = self.opus_manager.cursor.to_list()
        beat_offset = sum(self.rendered_beat_widths[0:cursor[1]]) + cursor[1] - 1
        line_length = sum(self.rendered_beat_widths) + len(self.rendered_beat_widths) - 1
        beat_width = self.rendered_beat_widths[cursor[1]]

        x_offset = beat_offset - ((self.frame_content.width - beat_width) // 2.5)
        x_offset = min(line_length - self.frame_content.width, x_offset)
        x_offset = max(0, x_offset)

        c, i = self.opus_manager.get_channel_index(cursor[0])

        y_offset = self.channel_rects[c][i].y - ((self.frame_content.height - 1) // 2.5)
        y_offset = min(self.frame_content.get_content_rect().height - self.frame_content.height - 1, y_offset)
        y_offset = max(0, y_offset)

        new_offset = (int(-1 *  x_offset), int(-1 * y_offset))

        if did_change_flag or new_offset != self.frame_content.get_view_offset():
            self.frame_beat_labels.move_inner(new_offset[0], 0)
            self.frame_content.move_inner(*new_offset)
            self.frame_line_labels.move_inner(0, new_offset[1])
            did_change_flag = True

        return did_change_flag


    def tick_update_beats(self) -> bool:
        channels = self.opus_manager.channel_groupings

        rect_topbar = self.frame_beat_labels.get_content_rect()

        beat_indices_changed = set()
        # The structure of the beat changed. rebuild the rects
        flagged_beat_changes = set(self.opus_manager.fetch('beat_change'))
        if not flagged_beat_changes:
            return False

        output = True
        cursor = self.opus_manager.cursor.get_triplet()
        while flagged_beat_changes:
            i, j, k = flagged_beat_changes.pop()

            rect_line = self.channel_rects[i][j]

            rect_beat = self.rect_beats[(i, j, k)]

            beat = channels[i][j][k]

            subbeat_map = self.build_beat_rect(beat, rect_beat)
            self.subbeat_rect_map[(i,j,k)] = subbeat_map

            if (i,j,k) in self.opus_manager.linked_beat_map:
                rect_beat.underline()
                for rect in self.subbeat_rect_map[(i,j,k)].values():
                    rect.underline()

            beat_indices_changed.add(k)

            # force the cursor to be redrawn
            if (i, j, k) == cursor:
                self.force_cursor_update = True

        # Resize the adjacent beats in all the lines to match sizes
        ## First, update the list of widest beat widths (self.rendered_beat_widths)
        ##  and map the increases/reductions in changed widths (in rect_size_diffs)
        rect_size_diffs = {}
        for i in beat_indices_changed:
            new_max = 2 # width will not be less than this
            for j, channel in enumerate(self.channel_rects):
                for k, _rect_line in enumerate(channel):
                    rect_beat = self.rect_beats[(j,k,i)]
                    new_max = max(rect_beat.width, new_max)

            old_max = self.rendered_beat_widths[i]
            if new_max != old_max:
                rect_size_diffs[i] = new_max - old_max
                self.rendered_beat_widths[i] = new_max

        # TODO: Merge consecutive rect_size_diffs

        ## Then, Shift groups of beats that would be moved by beat size changes
        diff_keys = list(rect_size_diffs.keys())
        diff_keys.sort()
        line_length = sum(self.rendered_beat_widths) + len(self.rendered_beat_widths)
        for k in diff_keys:
            offset = rect_size_diffs[k]
            # KLUDGE! TODO: modify wrecked shift_contents in box to handle infinites
            # Note: "k + 1" because we are shifting the contents AFTER the beat
            box_limit = (sum(self.rendered_beat_widths[0:max(0, k)]) + k + 1, 0, 9999999, 1)
            for channel in self.channel_rects:
                for rect_line in channel:
                    rect_line.shift_contents_in_box(offset, 0, box_limit)
            rect_topbar.shift_contents_in_box(offset, 0, box_limit)

        ## Now move the beat Rects that were specifically rebuilt
        for i in beat_indices_changed:
            cwidth = self.rendered_beat_widths[i]
            offset = sum(self.rendered_beat_widths[0:i]) + i
            for j, channel in enumerate(self.channel_rects):
                for k, rect_line in enumerate(channel):
                    rect_beat = self.rect_beats[(j, k,i)]
                    rect_beat.parent.resize(cwidth, 1)
                    rect_beat.move((cwidth - rect_beat.width) // 2, 0)
                    rect_beat.parent.move(offset, 0)
                    rect_beat_line = self.rect_beat_lines[j][k][i]
                    rect_beat_line.set_string(0, 0, chr(9474))
                    rect_beat_line.move(offset + cwidth, 0)

            # update label at top
            rect_label = self.rect_beat_labels[i]
            label = f"{i:02}"
            rect_label.resize(cwidth, 1)
            rect_label.move(sum(self.rendered_beat_widths[0:i]) + i, 0)
            rect_label.set_string(0, 0, " " * cwidth)
            rect_label.set_string(cwidth - len(label), 0, label)

        return output

    def tick_update_cursor(self) -> bool:
        output = False
        cursor = self.opus_manager.cursor.to_list()
        if self.rendered_cursor_position != cursor or self.force_cursor_update:
            output = True

            if self.rendered_cursor_position is not None:
                try:
                    rect = self.get_subbeat_rect(self.rendered_cursor_position)
                    rect.unset_invert()
                except KeyError:
                    pass
                except IndexError:
                    pass

            rect = self.get_subbeat_rect(cursor)
            rect.invert()

            self.rendered_cursor_position = cursor
            self.force_cursor_update = False

        return output

    def get_line_count(self):
        output = 0
        for channel in self.channel_rects:
            for _line in channel:
                output += 1
        return output

    def tick_update_lines(self) -> bool:
        line_length = sum(self.rendered_beat_widths) + self.opus_manager.opus_beat_count
        line_count = self.get_line_count()
        output = False
        if line_length != self.rendered_line_length or line_count != self.rendered_line_count:
            new_height = 0
            line_labels = []
            line_offset = 0
            for i, channel_index in enumerate(self.opus_manager.channel_order):
                channel = self.channel_rects[channel_index]
                for j, rect_line in enumerate(channel):
                    line_position = self.opus_manager.get_y(channel_index, j) + line_offset
                    rect_line.set_fg_color(wrecked.BLUE)
                    rect_line.resize(line_length, 1)
                    rect_line.move(0, line_position)
                    line_labels.append((channel_index,j))
                    new_height += 1 # Add space for this line

                if channel:
                    new_height += 1 # Add space for divider
                    rect_channel_divider = self.channel_divider_rects[channel_index]
                    rect_channel_divider.set_fg_color(wrecked.BRIGHTBLACK)
                    rect_channel_divider.resize(line_length, 1)
                    rect_channel_divider.set_string(0, 0, chr(9472) * line_length)
                    rect_channel_divider.move(0, self.opus_manager.get_y(channel_index, len(channel) - 1) + 1 + line_offset)
                    line_offset += 1

            rect_line_labels = self.frame_line_labels.get_content_rect()
            rect_line_labels.resize(self.frame_line_labels.width, new_height)
            rect_line_labels.clear_characters()

            y_offset = 0
            prev_c = None
            for y, (c, i) in enumerate(line_labels):
                if prev_c != c and y != 0:
                    y_offset += 1

                if c != prev_c:
                    if c != 9:
                        strlabel = f"{c:02}:{i:02}"
                    else:
                        strlabel = f"PS:{i:02}"
                else:
                    strlabel = f"  :{i:02}"

                rect_line_labels.set_string(0, y + y_offset, strlabel)
                prev_c = c

            rect_content = self.frame_content.get_content_rect()
            rect_content.resize(line_length, new_height)
            self.frame_beat_labels.get_content_rect().resize(line_length, 1)

            self.rendered_line_length = line_length
            self.rendered_line_count = line_count

            output = True

        return output

    def get_subbeat_rect(self, position):
        c, i = self.opus_manager.get_channel_index(position[0])
        return self.subbeat_rect_map[(c, i, position[1])][tuple(position[2:])]

    def build_beat_rect(self, beat_grouping, rect) -> Dict[Tuple(int), wrecked.Rect]:
        rect.clear_children()
        rect.clear_characters()
        rect.unset_invert()
        rect.unset_bg_color()
        rect.unset_fg_color()
        rect.unset_underline()
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

        # Traverse the subgroupings, depth-first
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
                                new_string += get_number_string(int(math.fabs(event.note)), base, 1)
                            else:
                                if event.note < 0:
                                    new_string += chr(8595)
                                    is_negative = True
                                else:
                                    new_string += chr(8593)
                                new_string += get_number_string(int(math.fabs(event.note)) // base, base, 1)
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
        while self.running:
            try:
                self.tick()
                time.sleep(self.tick_delay)
            except Exception as e:
                self.kill()
                raise e
            except KeyboardInterrupt:
                self.kill()

def sort_by_first(a):
    return a[0]
