from __future__ import annotations
import os
import re

from typing import Optional, Dict, List, Tuple

from apres import MIDI

from structures import BadStateError
from mgrouping import MGrouping, MGroupingEvent

class InvalidCursor(Exception):
    '''Raised when attempting to pass a cursor without enough arguments'''

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


    def export(self, *, path, tempo=120):
        opus = MGrouping()
        opus.set_size(self.opus_beat_count)
        for groupings in self.channel_groupings:
            for grouping in groupings:
                for i, beat in enumerate(grouping):
                    opus[i].merge(beat)

        opus.to_midi(tempo=tempo).save(path)

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
        working_position = self.cursor_position.copy()

        working_position[0] = max(0, working_position[0] - 1)
        while len(working_position) > 2:
            try:
                self.get_grouping(working_position)
                break
            except InvalidCursor:
                working_position.pop()
            except IndexError:
                working_position.pop()

        while self.get_grouping(working_position).is_structural():
            working_position.append(0)

        self.cursor_position = working_position

    def cursor_down(self):
        working_position = self.cursor_position.copy()

        if self.get_line(working_position[0] + 1) is not None:
            working_position[0] += 1

        while len(working_position) > 2:
            try:
                self.get_grouping(working_position)
                break
            except InvalidCursor:
                working_position.pop()
            except IndexError:
                working_position.pop()

        while self.get_grouping(working_position).is_structural():
            working_position.append(0)

        self.cursor_position = working_position

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

    def load(self, path: str) -> None:
        if os.path.isdir(path):
            self.load_folder(path)
        elif path[path.rfind("."):].lower() == ".mid":
            self.import_midi(path)
        else:
            self.load_file(path)

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

    def unset_at_cursor(self):
        position = self.cursor_position
        self.unset(position)

    def remove_at_cursor(self):
        position = self.cursor_position
        parent_grouping = self.get_grouping(position[0:-1])
        new_size = len(parent_grouping) - 1
        self.remove(position)

        if new_size < 2 and len(self.cursor_position) > 2:
            self.cursor_position.pop()
        elif position[-1] == new_size:
            self.cursor_position[-1] -= 1

    def unset(self, position):
        grouping = self.get_grouping(position)
        if grouping.is_event():
            grouping.clear_events()
        elif grouping.is_structural():
            grouping.clear()

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
        if index is None:
            index = self.opus_beat_count - 1

        for channel in self.channel_groupings:
            for line in channel:
                for i in range(index, self.opus_beat_count - 1):
                    line[i] = line[i + 1]

        self.set_beat_count(self.opus_beat_count - 1)

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
        if not self.channel_groupings[channel]:
            current_channel, current_index = self.get_channel_index(self.cursor_position[0])
            if current_channel > channel:
                self.cursor_position[0] += 1

        new_grouping = MGrouping()
        new_grouping.set_size(self.opus_beat_count)
        self.channel_groupings[channel].append(new_grouping)

    def remove_line(self, channel, index):
        self.channel_groupings[channel].pop(index)

    def remove_channel(self, channel):
        if self.channel_groupings[channel]:
            current_channel, current_index = self.get_channel_index(self.cursor_position[0])
            if channel < current_channel:
                self.cursor_position[0] -= len(self.channel_groupings[channel])
            elif channel == current_channel:
                self.cursor_position[0] -= current_index + 1
            self.cursor_position[0] = max(0, self.cursor_position[0])

        while self.channel_groupings[channel]:
            self.channel_groupings[channel].pop()


    def swap_channels(self, channel_a, channel_b):
        cursor = self.get_channel_index(self.cursor_position[0])

        tmp = self.channel_groupings[channel_b]
        self.channel_groupings[channel_b] = self.channel_groupings[channel_a]
        self.channel_groupings[channel_a] = tmp

        self.set_cursor_by_line(*cursor)

    def set_cursor_by_line(self, target_channel, target_line):
        y = 0
        for i, channel in enumerate(self.channel_groupings):
            for j in range(len(channel)):
                if target_channel == i and target_line == j:
                    self.cursor_position[0] = y
                    break
                y += 1

def split_by_channel(event, other_events):
    return event['channel']

def split_by_note_order(event, other_events):
    e_notes = [e.note for e in other_events]
    e_notes.sort()
    return e_notes.index(event.note)

