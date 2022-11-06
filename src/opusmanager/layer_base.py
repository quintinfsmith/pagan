from __future__ import annotations
import os
import re
import json

from typing import Optional, Dict, List, Tuple, TypeAlias
from apres import MIDI

from .structures import BadStateError
from .mgrouping import MGrouping, MGroupingEvent
from .errors import NoPathGiven, InvalidPosition

BeatKey: TypeAlias = Tuple[int, int, int]

class OpusManagerBase:
    """ Pure form of the OpusManager. Made for functional control over the opus """
    RADIX = 12
    DEFAULT_PERCUSSION = 0x32
    def __init__(self):
        self.channel_groupings = [[] for i in range(16)]
        self.opus_beat_count = 1
        self.linked_beat_map = {}
        self.inv_linked_beat_map = {}
        self.path = None
        self.percussion_map = {}

    @classmethod
    def load(cls, path: str):
        """Build a new OpusManager object from a radix-notation file or midi"""
        new_manager = cls()
        new_manager._load(path)
        return new_manager

    @classmethod
    def new(cls):
        """Build a new OpusManager object"""
        new_manager = cls()
        new_manager._new()
        return new_manager

    def _get_all_linked(self, beat_key: BeatKey) -> List[BeatKey]:
        """
            Get a list of all positions of beats that will be affected
            by changes made to the one at the given position (ie, linked)
        """
        if beat_key in self.inv_linked_beat_map:
            positions = [(beat_key)]
            for linked_key in self.inv_linked_beat_map[beat_key]:
                positions.append(linked_key)
        elif beat_key in self.linked_beat_map:
            target_key = self.linked_beat_map[beat_key]
            positions = [target_key]
            for linked_key in self.inv_linked_beat_map[target_key]:
                positions.append(linked_key)
        else:
            positions = [beat_key]

        return positions

    def _load(self, path: str) -> None:
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

        for channel_index, channel in enumerate(self.channel_groupings):
            for line_offset, line in enumerate(channel):
                line.clear_singles()

                # Populate the percussion map
                if channel_index == 9:
                    stack = [line]
                    while stack:
                        grouping = stack.pop(0)
                        if grouping.is_structural():
                            for child in grouping:
                                stack.append(child)
                        elif grouping.is_event():
                            event = list(grouping.get_events())[0]
                            self.percussion_map[line_offset] = event.get_note()
                            break

    def _new(self) -> None:
        """Only called from new() class method"""
        new_line = MGrouping()
        new_line.set_size(4)
        for i in range(4):
            new_line[i].set_size(1)
        self.channel_groupings[0].append(new_line)
        self.opus_beat_count = 4

    def insert_after(self, beat_key: BeatKey, position: List[int]) -> None:
        """
            Public-facing wrapper.
            Applies '_directly' to all beats linked to this one.
        """
        for linked_key in self._get_all_linked(beat_key):
            self._insert_after_directly(linked_key, position)

    def _insert_after_directly(self, beat_key: BeatKey, position: List[int]):
        """Create an empty grouping next to the given one, expanding the parent"""
        if not position:
            raise InvalidPosition(position)
        grouping = self.get_grouping(beat_key, position)

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

    def remove(self, beat_key: BeatKey, position: List[int]):
        """
            Public-facing wrapper.
            Applies '_directly' to all beats linked to this one.
        """
        for linked_key in self._get_all_linked(beat_key):
            self._remove_directly(linked_key, position)

    def _remove_directly(self, beat_key: BeatKey, position: List[int]):
        """Remove the given grouping, shrinking the parent"""
        grouping = self.get_grouping(beat_key, position)
        parent = grouping.parent

        if position == [0] and len(grouping.parent) == 1:
            self.unset(beat_key, position)
            return

        index = position[-1]
        new_size = len(parent) - 1

        if new_size > 0:
            for i, _child in enumerate(parent):
                if i < index or i == len(parent) - 1:
                    continue
                parent[i] = parent[i + 1]
            parent.set_size(new_size, True)

            # replace the parent with the child
            if new_size == 1 and self.get_beat_grouping(beat_key) != parent:
                parent_index = position[-2]
                parent.parent[parent_index] = parent[0]

        else:
            self._remove_directly(beat_key, position[0:-1])

    def _set_beat_count(self, new_count: int) -> None:
        """Adjust the number of beats in the opus"""
        self.opus_beat_count = new_count
        for groupings in self.channel_groupings:
            for grouping in groupings:
                grouping.set_size(new_count, True)


    def set_percussion_event(self, beat_key: BeatKey, position: List[int]) -> None:
        """
            Public-facing wrapper.
            Applies '_directly' to all beats linked to this one.
        """

        for linked_key in self._get_all_linked(beat_key):
            self._set_percussion_event_directly(linked_key, position)

    def _set_percussion_event_directly(self, beat_key: BeatKey, position: List[int]) -> None:
        """
            Set the percussion event with a pre-assigned
            instrument at the given position
        """

        channel, line_offset, _ = beat_key
        if channel != 9:
            raise IndexError("Attempting to set non-percussion channel")

        grouping = self.get_grouping(beat_key, position)
        if grouping.is_structural():
            grouping.clear()
        elif grouping.is_event():
            grouping.clear_events()

        grouping.add_event(MGroupingEvent(
            self.percussion_map.get(line_offset, self.DEFAULT_PERCUSSION),
            radix=self.RADIX,
            channel=9,
            relative=False
        ))

    def set_event(
            self,
            beat_key: BeatKey,
            position: List[int],
            value: int,
            *,
            relative: bool = False) -> None:
        """
            Public-facing wrapper.
            Applies '_directly' to all beats linked to this one.
        """
        for linked_key in self._get_all_linked(beat_key):
            self._set_event_directly(linked_key, position, value, relative=relative)

    def _set_event_directly(
            self,
            beat_key: BeatKey,
            position: List[int],
            value: int,
            *,
            relative: bool = False) -> None:
        """Set event at given grouping."""

        channel, _, _ = beat_key

        # User set_percussion_event() method on channel 9
        if channel == 9:
            raise IndexError("Attempting to set percussion channel")

        grouping = self.get_grouping(beat_key, position)

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

    def split_grouping(
            self,
            beat_key: BeatKey,
            position: List[int],
            splits: int) -> None:
        """
            Public-facing wrapper.
            Applies '_directly' to all beats linked to this one.
        """
        for linked_key in self._get_all_linked(beat_key):
            self._split_grouping_directly(linked_key, position, splits)

    def _split_grouping_directly(
            self,
            beat_key: BeatKey,
            position: List[int],
            splits: int) -> None:
        """Divide the grouping at the given position into *splits* divisions"""
        if position:
            grouping = self.get_grouping(beat_key, position)
        else:
            grouping = self.get_beat_grouping(beat_key)

        reduced = False
        if grouping.is_event():
            new_grouping = MGrouping()
            new_grouping.set_size(splits)
            grouping.replace_with(new_grouping)

            new_grouping[0].replace_with(grouping)

            ## Prevent redundant single-wrapper
            #if len(position) > 1 and len(new_grouping.parent) == 1:
            #    new_grouping.parent.replace_with(new_grouping)
        else:
            grouping.set_size(splits, True)
            # Prevent redundant single-wrapper
            #if len(position) > 2 and len(grouping.parent) == 1:
            #    grouping.parent.replace_with(grouping)
            #    reduced = True


    def unset(
            self,
            beat_key: BeatKey,
            position: List[int]) -> None:
        """
            Public-facing wrapper.
            Applies '_directly' to all beats linked to this one.
        """

        for linked_key in self._get_all_linked(beat_key):
            self._unset_directly(linked_key, position)

    def _unset_directly(
            self,
            beat_key: BeatKey,
            position: List[int]) -> None:
        """
            Remove the event from the given grouping
            but keep the grouping itself.
        """

        grouping = self.get_grouping(beat_key, position)
        if grouping.is_event():
            grouping.clear_events()
        elif grouping.is_structural():
            grouping.clear()


    def add_channel(self, channel: int) -> None:
        """Insert an active line in the given channel."""
        self.new_line(channel)

    def change_line_channel(self, old_channel: int, line_index: int, new_channel: int) -> None:
        """Move an active line to a different channel."""
        grouping = self.channel_groupings[old_channel].pop(line_index)
        self.channel_groupings[new_channel].append(grouping)

    def clear_links_to_beat(self, channel: int, line: int, beat: int) -> None:
        """Remove all links pointing to the specific beat"""
        target = (channel, line, beat)
        if target not in self.inv_linked_beat_map:
            return

        while self.inv_linked_beat_map[target]:
            linked_key = self.inv_linked_beat_map[target].pop()
            del self.linked_beat_map[linked_key]
        del self.inv_linked_beat_map[target]

    def export(self, *, path: Optional[str] = None, **kwargs) -> None:
        """Export this opus to a .mid"""
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

    def get_beat_grouping(self, beat_key: BeatKey) -> MGrouping:
        channel, line_offset, beat_index = beat_key
        return self.channel_groupings[channel][line_offset][beat_index]

    def get_grouping(
            self,
            beat_key: Tuple[int, int,  int],
            position: List[int]) -> MGrouping:
        """Get the Grouping object at the given position."""
        if len(position) < 1:
            raise InvalidPosition(position)

        grouping = self.get_beat_grouping(beat_key)

        try:
            for i in position:
                grouping = grouping[i]
        except BadStateError as exception:
            raise InvalidPosition(position) from exception

        return grouping

    def get_working_dir(self):
        """Get the the path that this file would be saved or exported to"""
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

    def import_midi(self, path: str) -> None:
        """Create opus from midi file"""
        self.path = path
        midi = MIDI.load(path)
        opus = MGrouping.from_midi(midi)
        tracks = opus.split(split_by_channel)

        self.opus_beat_count = 1
        for i, mgrouping in enumerate(tracks):
            for _j, split_line in enumerate(mgrouping.split(split_by_note_order)):
                self.channel_groupings[i].append(split_line)
                self.opus_beat_count = max(self.opus_beat_count, len(split_line))

    def insert_beat(self, index: Optional[int] = None) -> None:
        """Insert an empty beat at the given index in every line"""
        self.opus_beat_count += 1

        # Move all beats after new one right
        for channel in self.channel_groupings:
            for i, line in enumerate(channel):
                new_beat = MGrouping()
                new_beat.set_size(1)
                line.insert_grouping(index, new_beat)

        # Re-Adjust existing links
        adjusted_links = []
        for (i,j,k), target_key in self.linked_beat_map.items():
            new_key = (i, j, k)
            if k >= index:
                new_key = (i, j, k + 1)

            new_target = target_key
            if target_key[2] >= index:
                new_target = (target_key[0], target_key[1], target_key[2] + 1)
            adjusted_links.append((new_key, new_target))
        self.linked_beat_map = {}
        self.inv_linked_beat_map = {}
        for beat, target in adjusted_links:
            if target not in self.inv_linked_beat_map:
                self.inv_linked_beat_map[target] = set()
            self.inv_linked_beat_map[target].add(beat)
            self.linked_beat_map[beat] = target

    def link_beats(self, beat: BeatKey, target: BeatKey) -> None:
        """
            Overwrites *beat* with a copy of *target*,
            then creates a link between the two such that
            any changes made to one are made to the other.
        """
        # Remove any existing link
        self.unlink_beat(beat)

        # Replace existing grouping with a copy of the target
        self.overwrite_beat(beat, target)

        self.linked_beat_map[beat] = target
        if target not in self.inv_linked_beat_map:
            self.inv_linked_beat_map[target] = set()
        self.inv_linked_beat_map[target].add(beat)

    def load_folder(self, path: str) -> None:
        """Load opus from folder of radix notation files"""
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
            with open(f"{path}/{filename}", 'r', encoding="utf-8") as fp:
                content = fp.read()

            chunks = content.split("\n[")
            for i, chunk in enumerate(chunks):
                if i > 0:
                    chunks[i] = f"[{chunk}"

            for chunk in chunks:
                grouping = MGrouping.from_string(chunk, radix=radix, channel=channel)

                if grouping:
                    grouping.clear_singles()
                    beat_count = max(len(grouping), beat_count)
                    grouping.set_size(beat_count, True)

                    self.channel_groupings[channel].append(grouping)

        self.opus_beat_count = beat_count

        if os.path.isfile(f"{path}/linkedbeats.json"):
            with open(f"{path}/linkedbeats.json", "r", encoding="utf-8") as fp:
                json_compat_map = json.loads(fp.read())
                self.linked_beat_map = {}
                for k, target in json_compat_map.items():
                    new_key = tuple(int(i) for i in k.split("."))
                    self.linked_beat_map[new_key] = tuple(target)

        for beat, target in self.linked_beat_map.items():
            if target not in self.inv_linked_beat_map:
                self.inv_linked_beat_map[target] = set()
            self.inv_linked_beat_map[target].add(beat)


    def load_file(self, path: str) -> MGrouping:
        """Load opus from a single radix-notation file"""
        self.path = path
        radix = self.RADIX

        content = ""
        with open(path, 'r', encoding="utf-8") as fp:
            content = fp.read()

        chunks = content.split("\n[")
        for i, chunk in enumerate(chunks):
            if i > 0:
                chunks[i] = f"[{chunk}"

        self.opus_beat_count = 1
        for channel, chunk in enumerate(chunks):
            grouping = MGrouping.from_string(chunk, radix=radix, channel=channel)
            self.channel_groupings[channel].append(grouping)
            self.opus_beat_count = max(self.opus_beat_count, len(grouping))


    def move_line(self, channel: int, old_index: int, new_index: int) -> None:
        """Within a channel, move a line to a different position"""
        if old_index == new_index:
            return

        # Adjust the new_index so it doesn't get confused
        # when we pop() the old_index
        if new_index < 0:
            new_index = len(self.channel_groupings[channel]) + new_index
        if new_index < 0:
            raise IndexError(new_index)

        if old_index > len(self.channel_groupings[channel]):
            raise IndexError(old_index)

        grouping = self.channel_groupings[channel].pop(old_index)
        self.channel_groupings[channel].insert(new_index, grouping)

    def new_line(self, channel: int = 0, index: Optional[int] = None) -> None:
        """Create an empty line in the given channel"""
        new_grouping = MGrouping()
        new_grouping.set_size(self.opus_beat_count)
        for i in range(self.opus_beat_count):
            new_grouping[i].set_size(1)

        if index is not None:
            self.channel_groupings[channel].insert(index, new_grouping)
        else:
            self.channel_groupings[channel].append(new_grouping)

    def overwrite_beat(
            self,
            old_beat: BeatKey,
            new_beat: BeatKey) -> None:
        """Overwrite a beat with a copy of the grouping of another"""
        new_grouping = self.channel_groupings[new_beat[0]][new_beat[1]][new_beat[2]].copy()
        old_grouping = self.channel_groupings[old_beat[0]][old_beat[1]][old_beat[2]]
        old_grouping.replace_with(new_grouping)
        self.channel_groupings[old_beat[0]][old_beat[1]][old_beat[2]] = new_grouping

    def remove_beat(self, index: int) -> None:
        """Removes the beat at the index of every active line"""
        # Move all beats after removed index one left
        for i, channel in enumerate(self.channel_groupings):
            for j, line in enumerate(channel):
                self.unlink_beat((i, j, index))
                self.clear_links_to_beat(i, j, index)
                line.pop(index)
        self._set_beat_count(self.opus_beat_count - 1)

        # Re-Adjust existing links
        adjusted_links = []
        for (i,j,k), target_key in self.linked_beat_map.items():
            new_key = (i, j, k)
            if k > index:
                new_key = (i, j, k - 1)

            new_target = target_key
            if target_key[2] > index:
                new_target = (target_key[0], target_key[1], target_key[2] - 1)
            adjusted_links.append((new_key, new_target))

        self.linked_beat_map = {}
        self.inv_linked_beat_map = {}
        for beat, target in adjusted_links:
            if target not in self.inv_linked_beat_map:
                self.inv_linked_beat_map[target] = set()
            self.inv_linked_beat_map[target].add(beat)
            self.linked_beat_map[beat] = target

    def remove_channel(self, channel: int) -> None:
        """Remove all of the active lines in a channel"""
        while self.channel_groupings[channel]:
            self.remove_line(channel, 0)

    def remove_line(self, channel: int, index: int = None) -> None:
        """Remove an active line in a given channel"""
        if index is None:
            index = len(self.channel_groupings[channel]) - 1

        self.channel_groupings[channel].pop(index)

    def replace_grouping(
            self,
            beat_key: BeatKey,
            position: List[int],
            grouping: MGrouping) -> None:
        """Swap out a Grouping at the position with the given Grouping"""
        self.get_grouping(beat_key, position).replace_with(grouping)

    def replace_beat(self, beat_key: BeatKey, grouping: MGrouping) -> None:
        """Swap out a Beat's Grouping"""
        channel, line_offset, beat_index = beat_key
        self.channel_groupings[channel][line_offset][beat_index].replace_with(grouping)

    def save(self, path: Optional[str] = None) -> None:
        """Obvs"""
        if path is None and self.path is None:
            raise NoPathGiven()

        if path is None:
            path = self.path

        self.path = path

        fullpath = self.get_working_dir()

        if not os.path.isdir(fullpath):
            os.mkdir(fullpath)

        for subpath in os.listdir(fullpath):
            os.remove(f"{fullpath}/{subpath}")

        for i, channel_lines in enumerate(self.channel_groupings):
            if not channel_lines:
                continue

            strlines = []
            for line in channel_lines:
                line.clear_singles()
                strlines.append(line.to_string())

            hexrep = hex(i)[2:].upper()
            with open(f"{fullpath}/channel_{hexrep}", "w", encoding="utf-8") as fp:
                fp.write("\n".join(strlines))

        json_compat_map = {}
        for k, target in self.linked_beat_map.items():
            new_key = f"{k[0]}.{k[1]}.{k[2]}"
            json_compat_map[new_key] = target

        with open(f"{fullpath}/linkedbeats.json", "w", encoding="utf-8") as fp:
            fp.write(json.dumps(json_compat_map, indent=4))

    def set_percussion_instrument(self, line_offset: int, instrument: int) -> None:
        """
            Register an instrument to use on a percussion line.
            Updates existing events.
        """
        self.percussion_map[line_offset] = instrument

        # Traverse the line and set all the events to the new instrument
        stack = [self.channel_groupings[9][line_offset]]
        while stack:
            grouping = stack.pop(0)
            if grouping.is_structural():
                for subgrouping in grouping:
                    stack.append(subgrouping)
            elif grouping.is_event():
                event = grouping.get_events().pop()
                event.note = instrument
                grouping.add_event(event)

    def swap_channels(self, channel_a: int, channel_b: int) -> None:
        """Swap the active lines of two channels."""
        tmp = self.channel_groupings[channel_b]
        self.channel_groupings[channel_b] = self.channel_groupings[channel_a]
        self.channel_groupings[channel_a] = tmp

    def unlink_beat(self, beat_key: BeatKey) -> None:
        """
            Removes the link from this beat to another.
            Leaves the beat unchanged.
        """
        if beat_key not in self.linked_beat_map:
            return

        channel, line, beat = beat_key

        target_key = self.linked_beat_map[beat_key]
        self.inv_linked_beat_map[target_key].remove(beat_key)
        if not self.inv_linked_beat_map[target_key]:
            del self.inv_linked_beat_map[target_key]

        del self.linked_beat_map[beat_key]


def split_by_channel(event, other_events):
    return event['channel']

def split_by_note_order(event, other_events):
    e_notes = [e.note for e in other_events]
    e_notes.sort()
    return e_notes.index(event.note)
