from __future__ import annotations
import math
import os
import re
import time
import threading


from typing import Optional, Dict
import wrecked
from apres import MIDI

from structures import BadStateError
from mgrouping import MGrouping
from interactor import Interactor


class InvalidCursor(Exception):
    '''Raised when attempting to pass a cursor without enough arguments'''

class NoterEnvironment:
    tick_delay = 1 / 24

    def daemon_input(self):
        while self.running:
            self.interactor.get_input()
        self.interactor.restore_input_settings()

    def __init__(self):
        self.running = False
        self.root = wrecked.init()
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

        self.view_offset = 0

        self.rendered_channel_rects = set()
        self.rendered_beat_widths = []

        self.rendered_cursor_position = []
        self.rect_subbeat_map = {}

        self.flag_beat_changes = []
        self.flag_line_changes = []

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

        for c, channel in enumerate(self.opus_manager.channel_groupings):
            for i, grouping in enumerate(channel):
                grouping_rect = self.root.new_rect()
                self.channel_rects[c].append(grouping_rect)
                self.flag_line_changes.append((c,i))
                for b, _ in enumerate(grouping):
                    self.flag_beat_changes.append((c, i, b))


    def tick(self):
        channels = self.opus_manager.channel_groupings
        cursor  = self.opus_manager.cursor_position
        flag_draw = False

        _lines_changed = set()

        rect_beats = {}
        for c, i, b in self.flag_beat_changes:
            if math.fabs(cursor[1] - b) >= 13:
                continue

            _lines_changed.add((c, i))
            channel = channels[c]
            line = channel[i]
            beat = line[b]
            rect_line = self.channel_rects[c][i]

            #TODO: remove old beat
            rect_beats[(c, i, b)] = rect_line.new_rect()
            self.build_beat_rect(beat, rect_beats[(c, i ,b)])
            while b >= len(self.rendered_beat_widths):
                self.rendered_beat_widths.append(0)

            bw = self.rendered_beat_widths[b]
            self.rendered_beat_widths[b] = max(bw, rect_beats[(c, i, b)].width)

        line_length = sum(self.rendered_beat_widths) + self.opus_manager.opus_beat_count - 1
        for c, i in _lines_changed:
            line_position = self.opus_manager.get_y(c, i)
            rect_line = self.channel_rects[c][i]
            rect_line.set_fg_color(wrecked.BLUE)
            rect_line.resize(line_length, 1)
            rect_line.move(4, line_position)
            if i == 0:
                self.root.set_string(0, line_position, f"{c}:{i} ")
            else:
                self.root.set_string(0, line_position, f" :{i} ")

        while self.flag_beat_changes:
            c, i, b = self.flag_beat_changes.pop()
            if math.fabs(cursor[1] - b) >= 13:
                continue

            cwidth = self.rendered_beat_widths[b]
            offset = sum(self.rendered_beat_widths[0:b]) + b
            rect_beats[(c, i, b)].move(offset + ((cwidth - rect_beats[(c, i, b)].width) // 2), 0)

            if offset + cwidth < rect_line.width:
                rect_line = self.channel_rects[c][i]
                rect_line.set_string(offset + cwidth, 0, chr(9474))

            flag_draw = True

            self.rendered_channel_rects.add((c,i))

        # Draw cursor
        if self.rendered_cursor_position != self.opus_manager.cursor_position:
            if self.rendered_cursor_position:
                stack = [self.opus_manager.get_grouping(self.rendered_cursor_position)]
                while stack:
                    grouping = stack.pop()
                    self.rect_subbeat_map[grouping.get_uuid()].unset_invert()
                    if grouping.is_structural():
                        for child in grouping:
                            stack.append(child)

            stack = [self.opus_manager.get_grouping(self.opus_manager.cursor_position)]
            while stack:
                grouping = stack.pop()
                self.rect_subbeat_map[grouping.get_uuid()].invert()
                if grouping.is_structural():
                    for child in grouping:
                        stack.append(child)

            flag_draw = True
            self.rendered_cursor_position = self.opus_manager.cursor_position.copy()

        if flag_draw:
            self.root.draw()

    def get_subbeat_rect(self, position):
        subgrouping = self.opus_manager.get_grouping(position)
        return self.rect_subbeat_map[subgrouping.get_uuid()]

    def build_beat_rect(self, beat_grouping, rect) -> Dict[int, wrecked.Rect]:
        stack = [(beat_grouping, rect, 0)]
        depth_sorted_queue = []

        while stack:
            working_grouping, working_rect, depth = stack.pop(0)
            depth_sorted_queue.append((depth, working_grouping))
            self.rect_subbeat_map[working_grouping.get_uuid()] = working_rect
            if working_grouping.is_structural():
                for subgrouping in working_grouping:
                    stack.append((subgrouping, working_rect.new_rect(), depth + 1))

        depth_sorted_queue = sorted(depth_sorted_queue, key=sort_by_first, reverse=True)

        for _, working_grouping in depth_sorted_queue:
            working_rect = self.rect_subbeat_map[working_grouping.get_uuid()]
            if working_grouping.is_structural():
                running_width = 0
                if working_grouping != beat_grouping:
                    running_width += 1

                comma_points = []
                for i, subgrouping in enumerate(working_grouping):
                    child_rect = self.rect_subbeat_map[subgrouping.get_uuid()]
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
                    for i, (note, _, new_press, _) in enumerate(events):
                        if new_press:
                            new_string += "0123456789AB"[note // 12]
                            new_string += "0123456789AB"[note % 12]
                        else:
                            new_string += "~~"

                else:
                    new_string = '__'

                working_rect.resize(len(new_string), 1)
                working_rect.set_string(0, 0, new_string)

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

    def set_beat_event(self, octave, offset, position):
        grouping = self.get_grouping(position)
        grouping.add_event(((octave * self.BASE) + offset))

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

    def get_y(self, c: int, i: int) -> int:
        y = 0
        for j in self.channel_order:
            for g, grouping in enumerate(self.channel_groupings[j]):
                if i == g and j == c:
                    return y
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
                    grouping.crop_redundancies()

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

def split_by_channel(event, other_events):
    return event[3]

def split_by_note_order(event, other_events):
    e_notes = [e[0] for e in other_events]
    e_notes.sort()
    return e_notes.index(event[0])

def sort_by_first(a):
    return a[0]
