from __future__ import annotations
import os
import re
import time

from typing import Optional
import wrecked
from apres import MIDI
from mgrouping import MGrouping

class InvalidCursor(Exception):
    '''Raised when attempting to pass a cursor without enough arguments'''

class NoterEnvironment:
    tick_delay = 60 / 24

    def __init__(self):
        self.root = wrecked.init()
        self.channel_rects = [[] for i in range(16)]
        self.opus_manager = OpusManager()

        self.rendered_channel_rects = set()
        self.rendered_beat_widths = {}

    def kill(self):
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

    def tick(self):
        channels = self.opus_manager.channel_groupings
        flag_draw = False
        line_position = 0
        for c in self.opus_manager.channel_order:
            for i, line in enumerate(channels[c]):
                if (c, i) in self.rendered_channel_rects:
                    line_position += 1
                    continue

                self.rendered_channel_rects.add((c,i))
                rect_line = self.channel_rects[c][i]
                rect_line.clear_children()
                running_len = 0
                split_positions = [0]
                for b, beat in enumerate(line):
                    bstring = beat.to_string()
                    rect_beat = rect_line.new_rect()
                    new_width = max(len(bstring), self.rendered_beat_widths.get(b,0))
                    rect_beat.resize(new_width, 1)
                    rect_beat.move(running_len + 1, 0)
                    rect_beat.set_string(0,0,bstring)
                    running_len += new_width + 1

                    split_positions.append(running_len)

                rect_line.resize(running_len + 1, 1)
                rect_line.move(4, line_position)
                rect_line.set_fg_color(wrecked.BLUE)
                for x in split_positions:
                    rect_line.set_string(x, 0, "|")

                self.root.set_string(0, line_position, f"{c}:{i} ")

                flag_draw = True
                line_position += 1

        # Draw cursor


        if flag_draw:
            self.root.draw()

    def run(self):
        self.running = True
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
        for i in position[1:]:
            grouping = grouping[i]
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
        suffix_patt = re.compile(".*_(?P<suffix>([0-9A-Z]{1,3})?)(\..*)?", re.I)
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
