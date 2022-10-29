from __future__ import annotations
import os
import re
import json

from typing import Optional, Dict, List, Tuple
from apres import MIDI

from .structures import BadStateError
from .mgrouping import MGrouping, MGroupingEvent
from .errors import NoPathGiven, InvalidPosition

class OpusManagerBase:
    """ Pure form of the OpusManager. Made for functional control over the opus """
    RADIX = 12
    def __init__(self):
        self.channel_groupings = [[] for i in range(16)]
        self.channel_order = list(range(16))
        self.opus_beat_count = 1
        self.linked_beat_map = {}
        self.inv_linked_beat_map = {}
        self.path = None
        self.clipboard_grouping = None
        self.flag_kill = False

    @classmethod
    def new(cls):
        """Build a new OpusManager object"""
        new_manager = cls()
        new_manager._new()
        return new_manager

    def _new(self):
        """Only called from new() class method"""
        new_line = MGrouping()
        new_line.set_size(4)
        for i in range(4):
            new_line[i].set_size(1)
        self.channel_groupings[0].append(new_line)
        self.opus_beat_count = 4

    @classmethod
    def load(cls, path: str):
        """Build a new OpusManager object from a radix-notation file or midi"""
        new_manager = cls()
        new_manager._load(path)
        return new_manager

    def _load(self, path):
        """Only called from load(..) class method"""
        if os.path.isdir(path):
            if len(path) > 1 and path[-1] == "/":
                path = path[0:-1]
            self.load_folder(path)
        elif path[path.rfind("."):].lower() == ".mid":
            self.import_midi(path)
            self.path = path[0:path.rfind(".mid")] + "_midi"
        else:
            self.load_file(path)
        for channel in self.channel_groupings:
            for line in channel:
                line.clear_singles()


    @property
    def line_count(self) -> int:
        """Get the number of lines active in this opus."""
        return sum((len(channel) for channel in self.channel_groupings))

    def clear_links_to_beat(self, channel: int, line: int, beat: int) -> None:
        """Remove all links pointing to the specific beat"""
        target = (channel, line, beat)
        if target not in self.inv_linked_beat_map:
            return

        while self.inv_linked_beat_map[target]:
            linked_key = self.inv_linked_beat_map[target].pop()
            del self.linked_beat_map[linked_key]
        del self.inv_linked_beat_map[target]

    def unlink_beat(self, channel: int, line: int, beat: int) -> None:
        """
            Removes the link from this beat to another.
            Leaves the beat unchanged.
        """
        key = (channel, line, beat)
        if key not in self.linked_beat_map:
            return

        target_key = self.linked_beat_map[key]
        self.inv_linked_beat_map[target_key].remove(key)
        if not self.inv_linked_beat_map[target_key]:
            del self.inv_linked_beat_map[target_key]

        del self.linked_beat_map[key]

    def link_beats(self, beat, target) -> None:
        """
            Overwrites *beat* with a copy of *target*,
            then creates a link between the two such that
            any changes made to one are made to the other.
        """
        # Remove any existing link
        self.unlink_beat(*beat)

        # Replace existing grouping with a copy of the target
        self.overwrite_beat(beat, target)

        self.linked_beat_map[beat] = target
        if target not in self.inv_linked_beat_map:
            self.inv_linked_beat_map[target] = set()
        self.inv_linked_beat_map[target].add(beat)

    def overwrite_beat(self, old_beat, new_beat) -> None:
        """Overwrite a beat with a copy of the grouping of another"""
        new_grouping = self.channel_groupings[new_beat[0]][new_beat[1]][new_beat[2]].copy()
        old_grouping = self.channel_groupings[old_beat[0]][old_beat[1]][old_beat[2]]
        old_grouping.replace_with(new_grouping)
        self.channel_groupings[old_beat[0]][old_beat[1]][old_beat[2]] = new_grouping



    def copy_grouping(self, position):
        self.clipboard_grouping = self.get_grouping(position)

    def replace_grouping(self, position, grouping):
        self.get_grouping(position).replace_with(grouping)

    def paste_grouping(self, position):
        if self.clipboard_grouping is None:
            return

        self.replace_grouping(position, self.clipboard_grouping)
        self.clipboard_grouping = None

    def get_working_dir(self):
        path = self.path
        if path.lower().endswith("mid"):
            working_path = path[0:path.rfind("/")]
            working_dir = path[path.rfind("/") + 1:path.rfind(".")]
        elif os.path.isfile(path):
            working_path = path[0:path.rfind("/")]
            working_dir = path[path.rfind("/") + 1:path.rfind(".")]
        elif os.path.isdir(path):
            tmp_split = path.split("/")
            working_path = "/".join(tmp_split[0:-1]) + "/"
            working_dir = tmp_split[-1]
        else:
            working_path = path[0:path.rfind("/") + 1]
            working_dir = path[path.rfind("/") + 1:]

        fullpath = f"{working_path}{working_dir}"

        return fullpath

    def save(self, path=None):
        if path is None and self.path is None:
            raise NoPathGiven()
        elif path is None:
            path = self.path

        self.path = path

        fullpath = self.get_working_dir()

        if not os.path.isdir(fullpath):
            os.mkdir(fullpath)

        for f in os.listdir(fullpath):
            os.remove(f"{fullpath}/{f}")

        for c, channel_lines in enumerate(self.channel_groupings):
            if not channel_lines:
                continue

            strlines = []
            for line in channel_lines:
                line.clear_singles()
                strlines.append(line.to_string())

            n = "0123456789ABCDEF"[c]
            with open(f"{fullpath}/channel_{n}", "w") as fp:
                fp.write("\n".join(strlines))

        json_compat_map = {}
        for k, target in self.linked_beat_map.items():
            new_key = f"{k[0]}.{k[1]}.{k[2]}"
            json_compat_map[new_key] = target

        with open(f"{fullpath}/linkedbeats.json", "w") as fp:
            fp.write(json.dumps(json_compat_map, indent=4))

    def kill(self):
        self.flag_kill = True

    def export(self, *, path=None, **kwargs):
        for i in range(16):
            kwargs[f"i{i}"] = int(kwargs.get(f"i{i}", 0))

        opus = MGrouping()
        opus.set_size(self.opus_beat_count)
        for groupings in self.channel_groupings:
            for grouping in groupings:
                for i, beat in enumerate(grouping):
                    opus[i].merge(beat)

        if path is None:
            if self.path is not None:
                path = self.path + ".mid"
            else:
                raise NoPathGiven()

        opus.to_midi(**kwargs).save(path)

    def get_grouping(self, position):
        grouping = self.get_line(position[0])
        try:
            for i in position[1:]:
                grouping = grouping[i]
        except BadStateError as e:
            raise InvalidPosition(position)

        return grouping

    def set_event(self, value, position, *, relative=False):
        for absolute_position in self._build_positions_list(position):
            self._set_event_ignore_link(value, absolute_position, relative=relative)

    def _set_event_ignore_link(self, value, position, *, relative=False):
        channel, _index = self.get_channel_index(position[0])
        grouping = self.get_grouping(position)

        if grouping.is_structural():
            grouping.clear()
        elif grouping.is_event():
            grouping.clear_events()

        grouping.add_event(MGroupingEvent(
            value,
            radix=self.RADIX,
            channel=channel,
            relative=relative
        ))

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
        for i in range(self.opus_beat_count):
            new_grouping[i].set_size(1)

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

    def get_channel_index(self, y_index: int) -> Tuple[int, int]:
        """
            Given the y-index of a line (as in from the cursor),
            get the channel and index thereof
        """

        for channel in self.channel_order:
            for i, _ in enumerate(self.channel_groupings[channel]):
                if y_index == 0:
                    return (channel, i)
                y_index -= 1

        raise IndexError

    def get_y(self, c: int, i: Optional[int] = None) -> int:
        """
            Given a channel and index,
            get the y-index of the specified line displayed
        """
        if i is None:
            i = len(self.channel_groupings[c]) - 1

        y = 0
        for j in self.channel_order:
            for g, grouping in enumerate(self.channel_groupings[j]):
                if i == g and j == c:
                    return y
                y += 1
        return -1

    # TODO: Use a clearer name
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
        radix = self.RADIX

        channel_map = {}
        suffix_patt = re.compile(".*_(?P<suffix>([0-9A-Z]{1,3})?)(\\..*)?", re.I)
        filenames = os.listdir(path)
        filenames_clean = []

        # create a reference map of channels and remove non-suffixed files from the list
        for filename in filenames:
            if filename[filename.rfind("."):] in (".swp", ".json"):
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
                grouping = MGrouping.from_string(chunk, radix=radix, channel=channel)

                if grouping:
                    grouping.clear_singles()
                    beat_count = max(len(grouping), beat_count)
                    grouping.set_size(beat_count, True)

                    self.channel_groupings[channel].append(grouping)

        self.opus_beat_count = beat_count

        if os.path.isfile(f"{path}/linkedbeats.json"):
            with open(f"{path}/linkedbeats.json", "r") as fp:
                json_compat_map = json.loads(fp.read())
                self.linked_beat_map = {}
                for k, target in json_compat_map.items():
                    x,y,z = k.split(".")
                    new_key = (int(x), int(y), int(z))
                    self.linked_beat_map[new_key] = tuple(target)

        for beat, target in self.linked_beat_map.items():
            if target not in self.inv_linked_beat_map:
                self.inv_linked_beat_map[target] = set()
            self.inv_linked_beat_map[target].add(beat)


    def load_file(self, path: str) -> MGrouping:
        self.path = path
        radix = self.RADIX

        content = ""
        with open(path, 'r') as fp:
            content = fp.read()

        chunks = content.split("\n[")
        for i, chunk in enumerate(chunks):
            if i > 0:
                chunks[i] = f"[{chunk}"

        self.opus_beat_count = 1
        for x, chunk in enumerate(chunks):
            grouping = MGrouping.from_string(chunk, radix=radix, channel=x)
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

    def insert_after(self, position: List[int]):
        for position in self._build_positions_list(position):
            self._insert_after_ignore_linked(position)

    def _insert_after_ignore_linked(self, position: List[int]):
        grouping = self.get_grouping(position)

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

    def _build_positions_list(self, position):
        channel, index = self.get_channel_index(position[0])
        beat_key = (channel, index, position[1])

        if beat_key in self.inv_linked_beat_map:
            positions = [position]
            for i, j, k in self.inv_linked_beat_map[beat_key]:
                new_position = position.copy()
                new_position[0] = self.get_y(i, j)
                new_position[1] = k
                positions.append(new_position)
        elif beat_key in self.linked_beat_map:
            target_key = self.linked_beat_map[beat_key]
            new_position = position.copy()
            new_position[0] = self.get_y(target_key[0], target_key[1])
            new_position[1] = target_key[2]
            positions = [new_position]
            for i, j, k in self.inv_linked_beat_map[target_key]:
                new_position = position.copy()
                new_position[0] = self.get_y(i, j)
                new_position[1] = k
                positions.append(new_position)
        else:
            positions = [position]

        return positions

    def unset(self, position):
        for absolute_position in self._build_positions_list(position):
            self._unset_ignore_link(absolute_position)

    def _unset_ignore_link(self, position):
        grouping = self.get_grouping(position)
        if grouping.is_event():
            grouping.clear_events()
        elif grouping.is_structural():
            grouping.clear()

    def remove(self, initial_position: List[int]):
        for position in self._build_positions_list(initial_position):
            self._remove_ignore_link(position)

    def _remove_ignore_link(self, position: List[int]):
        grouping = self.get_grouping(position)
        parent = grouping.parent
        if len(position) < 3:
            return

        elif len(position) == 3 and len(parent) == 1:
            self.unset(position)
            return

        index = position[-1]
        new_size = len(parent) - 1

        if new_size > 0:
            for i, child in enumerate(parent):
                if i < index or i == len(parent) - 1:
                    continue
                parent[i] = parent[i + 1]
            parent.set_size(new_size, True)

            # replace the parent with the child
            if new_size == 1 and parent != self.get_grouping(position[0:2]):
                parent_index = position[-2]
                parent.parent[parent_index] = parent[0]

        else:
            self._remove_ignore_link(position[0:-1])


    def remove_beat(self, index):
        # Move all beats after removed index one left
        for i, channel in enumerate(self.channel_groupings):
            for j, line in enumerate(channel):
                self.unlink_beat(i, j, index)
                self.clear_links_to_beat(i, j, index)
                line.pop(index)
        self.set_beat_count(self.opus_beat_count - 1)

        # Re-Adjust existing links
        adjusted_links = []
        for (i,j,k), (ti, tj, tk) in self.linked_beat_map.items():
            new_key = (i, j, k)
            if k > index:
                new_key = (i, j, k - 1)

            new_target = (ti, tj, tk)
            if tk > index:
                new_target = (ti, tj, tk - 1)
            adjusted_links.append((new_key, new_target))

        self.linked_beat_map = {}
        self.inv_linked_beat_map = {}
        for beat, target in adjusted_links:
            if target not in self.inv_linked_beat_map:
                self.inv_linked_beat_map[target] = set()
            self.inv_linked_beat_map[target].add(beat)
            self.linked_beat_map[beat] = target


    # TODO: Should this be a Grouping Method?
    def insert_beat(self, index=None):
        original_beat_count = self.opus_beat_count
        self.opus_beat_count += 1

        # Move all beats after new one right
        for channel in self.channel_groupings:
            for i, line in enumerate(channel):
                new_beat = MGrouping()
                new_beat.set_size(1)
                line.insert_grouping(index, new_beat)

        # Re-Adjust existing links
        adjusted_links = []
        for (i,j,k), (ti, tj, tk) in self.linked_beat_map.items():
            new_key = (i, j, k)
            if k >= index:
                new_key = (i, j, k + 1)

            new_target = (ti, tj, tk)
            if tk >= index:
                new_target = (ti, tj, tk + 1)
            adjusted_links.append((new_key, new_target))
        self.linked_beat_map = {}
        self.inv_linked_beat_map = {}
        for beat, target in adjusted_links:
            if target not in self.inv_linked_beat_map:
                self.inv_linked_beat_map[target] = set()
            self.inv_linked_beat_map[target].add(beat)
            self.linked_beat_map[beat] = target

    def split_grouping(self, initial_position, splits):
        for position in self._build_positions_list(initial_position):
            self._split_grouping_ignore_linked(position, splits)

    def _split_grouping_ignore_linked(self, position, splits):
        grouping = self.get_grouping(position)
        reduced = False
        if grouping.is_event():
            new_grouping = MGrouping()
            new_grouping.set_size(splits)
            grouping.replace_with(new_grouping)

            new_grouping[0].replace_with(grouping)

            # Prevent redundant single-wrapper
            if len(position) > 2 and len(new_grouping.parent) == 1:
                new_grouping.parent.replace_with(new_grouping)
                reduced = True
        else:
            grouping.set_size(splits, True)
            # Prevent redundant single-wrapper
            if len(position) > 2 and len(grouping.parent) == 1:
                grouping.parent.replace_with(grouping)
                reduced = True

    def new_line(self, channel=0, index=None):
        new_grouping = MGrouping()
        new_grouping.set_size(self.opus_beat_count)
        for i in range(self.opus_beat_count):
            new_grouping[i].set_size(1)
        if index is not None:
            self.channel_groupings[channel].insert(index, new_grouping)
        else:
            self.channel_groupings[channel].append(new_grouping)

    def remove_line(self, channel, index=None):
        if index is None:
            index = len(self.channel_groupings[channel]) - 1

        self.channel_groupings[channel].pop(index)

    def add_channel(self, channel):
        self.new_line(channel)

    def remove_channel(self, channel):
        while self.channel_groupings[channel]:
            self.remove_line(channel, 0)

    def swap_channels(self, channel_a, channel_b):
        tmp = self.channel_groupings[channel_b]
        self.channel_groupings[channel_b] = self.channel_groupings[channel_a]
        self.channel_groupings[channel_a] = tmp


def split_by_channel(event, other_events):
    return event['channel']

def split_by_note_order(event, other_events):
    e_notes = [e.note for e in other_events]
    e_notes.sort()
    return e_notes.index(event.note)
