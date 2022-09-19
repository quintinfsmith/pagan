from __future__ import annotations
import os
import re

from typing import Optional
import wrecked
from apres import MIDI
from mgrouping import MGrouping

class InvalidCursor(Exception):
    '''Raised when attempting to pass a cursor without enough arguments'''

class NoterEnvironment:
    def __init__(self):
        self.root = wrecked.init()
        self.channel_rects = [[] for i in range(16)]
        self.opus_manager = OpusManager()

        self.rendered_channel_rects = []
        self.beat_sizes = []

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
        base = 12

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
            content = ""
            with open(f"{path}/{filename}", 'r') as fp:
                content = fp.read()

            chunks = content.split("\n[")
            for i, chunk in enumerate(chunks):
                if i > 0:
                    chunks[i] = f"[{chunk}"

            for x, chunk in enumerate(chunks):
                grouping = MGrouping.from_string(chunk, base=base, channel=channel_map[filename])
                beat_count = max(len(grouping), beat_count)
                if grouping:
                    grouping.set_size(beat_count, True)
                    grouping.crop_redundancies()

                    self.channel_groupings[channel_map[filename]].append(grouping)

    def load_file(self, path: str) -> MGrouping:
        base = 12

        content = ""
        with open(path, 'r') as fp:
            content = fp.read()

        chunks = content.split("\n[")
        for i, chunk in enumerate(chunks):
            if i > 0:
                chunks[i] = f"[{chunk}"

        for x, chunk in enumerate(chunks):
            grouping = MGrouping.from_string(chunk, base=base, channel=x)
            self.channel_groupings[x].append(grouping)

    def import_midi(self, path: str) -> None:
        midi = MIDI.load(path)
        opus = MGrouping.from_midi(midi)
        tracks = opus.split(split_by_channel)

        for i, mgrouping in enumerate(tracks):
            for _j, split_line in enumerate(mgrouping.split(split_by_note_order)):
                self.channel_groupings[i].append(split_line)

    def kill(self):
        wrecked.kill()

def split_by_channel(event, other_events):
    return event[3]

def split_by_note_order(event, other_events):
    e_notes = [e[0] for e in other_events]
    e_notes.sort()
    return e_notes.index(event[0])
