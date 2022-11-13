from __future__ import annotations
import os
import re
import json

from typing import Optional, Dict, List, Tuple, TypeAlias
from apres import MIDI

from .structures import BadStateError
from .miditree import MIDITree, MIDITreeEvent
from .errors import NoPathGiven, InvalidPosition

BeatKey: TypeAlias = Tuple[int, int, int]

class OpusManagerBase:
    """ Pure form of the OpusManager. Made for functional control over the opus """
    RADIX = 12
    DEFAULT_PERCUSSION = 0x32
    def __init__(self):
        self.channel_trees = [[] for i in range(16)]
        self.opus_beat_count = 1
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

        for channel_index, channel in enumerate(self.channel_trees):
            for line_offset, line in enumerate(channel):
                for beat in line:
                    if beat.is_leaf():
                        middle = MIDITree()
                        beat.replace_with(middle)
                        middle[0].replace_with(beat)
                    for subbeat in beat:
                        beat.clear_singles()

                # Populate the percussion map
                if channel_index == 9:
                    stack = [line]
                    while stack:
                        tree = stack.pop(0)
                        if tree.is_event():
                            event = tree.get_event()
                            self.percussion_map[line_offset] = event.get_note()
                            break
                        if not tree.is_open():
                            for child in tree:
                                stack.append(child)

    def _new(self) -> None:
        """Only called from new() class method"""
        new_line = MIDITree()
        new_line.set_size(4)
        for i in range(4):
            new_line[i].set_size(1)
        self.channel_trees[0].append(new_line)
        self.opus_beat_count = 4

    def insert_after(self, beat_key: BeatKey, position: List[int]):
        """Create an empty tree next to the given one, expanding the parent"""
        if not position:
            raise InvalidPosition(position)
        tree = self.get_tree(beat_key, position)

        parent = tree.parent
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
        """Remove the given tree, shrinking the parent"""
        tree = self.get_tree(beat_key, position)
        parent = tree.parent

        if position == [0] and len(tree.parent) == 1:
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
            if new_size == 1 and self.get_beat_tree(beat_key) != parent:
                parent_index = position[-2]
                parent.parent[parent_index] = parent[0]

        else:
            self.remove(beat_key, position[0:-1])

    def _set_beat_count(self, new_count: int) -> None:
        """Adjust the number of beats in the opus"""
        self.opus_beat_count = new_count
        for trees in self.channel_trees:
            for tree in trees:
                tree.set_size(new_count, True)


    def set_percussion_event(self, beat_key: BeatKey, position: List[int]) -> None:
        """
            Set the percussion event with a pre-assigned
            instrument at the given position
        """

        channel, line_offset, _ = beat_key
        if channel != 9:
            raise IndexError("Attempting to set non-percussion channel")

        tree = self.get_tree(beat_key, position)
        if tree.is_event():
            tree.clear_events()
        else:
            tree.clear()

        tree.set_event(MIDITreeEvent(
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
        """Set event at given tree."""

        channel, _, _ = beat_key

        # User set_percussion_event() method on channel 9
        if channel == 9:
            raise IndexError("Attempting to set percussion channel")

        tree = self.get_tree(beat_key, position)

        if tree.is_event():
            tree.clear_events()
        else:
            tree.clear()

        tree.set_event(MIDITreeEvent(
            value,
            radix=self.RADIX,
            channel=channel,
            relative=relative
        ))

    def split_tree(
            self,
            beat_key: BeatKey,
            position: List[int],
            splits: int) -> None:
        """Divide the tree at the given position into *splits* divisions"""
        beat_tree = self.get_beat_tree(beat_key)
        if position:
            if position == [0] and len(beat_tree) == 1:
                tree = beat_tree
            else:
                tree = self.get_tree(beat_key, position)
        else:
            tree = beat_tree

        if tree.is_event():
            new_tree = MIDITree()
            new_tree.set_size(splits)
            tree.replace_with(new_tree)
            new_tree[0].replace_with(tree)
        else:
            tree.set_size(splits, True)

    def unset(
            self,
            beat_key: BeatKey,
            position: List[int]) -> None:
        """
            Remove the event from the given tree
            but keep the tree itself.
        """

        tree = self.get_tree(beat_key, position)
        if tree.is_event():
            tree.clear_events()
        else:
            tree.clear()


    def add_channel(self, channel: int) -> None:
        """Insert an active line in the given channel."""
        self.new_line(channel)

    def change_line_channel(self, old_channel: int, line_index: int, new_channel: int) -> None:
        """Move an active line to a different channel."""
        tree = self.channel_trees[old_channel].pop(line_index)
        self.channel_trees[new_channel].append(tree)


    def export(self, *, path: Optional[str] = None, **kwargs) -> None:
        """Export this opus to a .mid"""
        for i in range(16):
            kwargs[f"i{i}"] = int(kwargs.get(f"i{i}", 0))

        opus = MIDITree()
        opus.set_size(self.opus_beat_count)
        for trees in self.channel_trees:
            for tree in trees:
                for i, beat in enumerate(tree):
                    opus[i].merge(beat)

        if path is None:
            if self.path is not None:
                path = self.path + ".mid"
            else:
                raise NoPathGiven()

        opus.to_midi(**kwargs).save(path)

    def get_beat_tree(self, beat_key: BeatKey) -> MIDITree:
        channel, line_offset, beat_index = beat_key
        if line_offset < 0:
            line_offset = len(self.channel_trees[channel]) + line_offset
        return self.channel_trees[channel][line_offset][beat_index]

    def get_tree(
            self,
            beat_key: Tuple[int, int,  int],
            position: List[int]) -> MIDITree:
        """Get the OpusTree object at the given position."""
        if len(position) < 1:
            raise InvalidPosition(position)

        tree = self.get_beat_tree(beat_key)

        try:
            for i in position:
                tree = tree[i]
        except BadStateError as exception:
            raise InvalidPosition(position) from exception

        return tree

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
        opus = MIDITree.from_midi(midi)
        tracks = opus.split(split_by_channel)

        self.opus_beat_count = 1
        for i, mtree in enumerate(tracks):
            for _j, split_line in enumerate(mtree.split(split_by_note_order)):
                self.channel_trees[i].append(split_line)
                self.opus_beat_count = max(self.opus_beat_count, len(split_line))

    def insert_beat(self, index: Optional[int] = None) -> None:
        """Insert an empty beat at the given index in every line"""
        self.opus_beat_count += 1

        # Move all beats after new one right
        for channel in self.channel_trees:
            for i, line in enumerate(channel):
                new_beat = MIDITree()
                new_beat.set_size(1)
                line.insert_tree(index, new_beat)


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
                tree = MIDITree.from_string(chunk, radix=radix, channel=channel)

                if tree:
                    tree.clear_singles()
                    beat_count = max(len(tree), beat_count)
                    tree.set_size(beat_count, True)

                    self.channel_trees[channel].append(tree)

        self.opus_beat_count = beat_count


    def load_file(self, path: str) -> MIDITree:
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
            tree = MIDITree.from_string(chunk, radix=radix, channel=channel)
            self.channel_trees[channel].append(tree)
            self.opus_beat_count = max(self.opus_beat_count, len(tree))


    def move_line(self, channel: int, old_index: int, new_index: int) -> None:
        """Within a channel, move a line to a different position"""
        if old_index == new_index:
            return

        # Adjust the new_index so it doesn't get confused
        # when we pop() the old_index
        if new_index < 0:
            new_index = len(self.channel_trees[channel]) + new_index
        if new_index < 0:
            raise IndexError(new_index)

        if old_index > len(self.channel_trees[channel]):
            raise IndexError(old_index)

        tree = self.channel_trees[channel].pop(old_index)
        self.channel_trees[channel].insert(new_index, tree)

    def new_line(self, channel: int = 0, index: Optional[int] = None) -> None:
        """Create an empty line in the given channel"""
        new_tree = MIDITree()
        new_tree.set_size(self.opus_beat_count)
        for i in range(self.opus_beat_count):
            new_tree[i].set_size(1)

        if index is not None:
            self.channel_trees[channel].insert(index, new_tree)
        else:
            self.channel_trees[channel].append(new_tree)

    def overwrite_beat(
            self,
            old_beat: BeatKey,
            new_beat: BeatKey) -> None:
        """Overwrite a beat with a copy of the tree of another"""
        new_tree = self.channel_trees[new_beat[0]][new_beat[1]][new_beat[2]].copy()
        old_tree = self.channel_trees[old_beat[0]][old_beat[1]][old_beat[2]]
        old_tree.replace_with(new_tree)
        self.channel_trees[old_beat[0]][old_beat[1]][old_beat[2]] = new_tree

    def remove_beat(self, index: int) -> None:
        """Removes the beat at the index of every active line"""
        # Move all beats after removed index one left
        for i, channel in enumerate(self.channel_trees):
            for j, line in enumerate(channel):
                line.pop(index)
        self._set_beat_count(self.opus_beat_count - 1)


    def remove_channel(self, channel: int) -> None:
        """Remove all of the active lines in a channel"""
        while self.channel_trees[channel]:
            self.remove_line(channel, 0)

    def remove_line(self, channel: int, index: int = None) -> None:
        """Remove an active line in a given channel"""
        if index is None:
            index = len(self.channel_trees[channel]) - 1

        self.channel_trees[channel].pop(index)

    def replace_tree(
            self,
            beat_key: BeatKey,
            position: List[int],
            tree: MIDITree) -> None:
        """Swap out a OpusTree at the position with the given OpusTree"""
        self.get_tree(beat_key, position).replace_with(tree)

    def replace_beat(self, beat_key: BeatKey, tree: MIDITree) -> None:
        """Swap out a Beat's OpusTree"""
        channel, line_offset, beat_index = beat_key
        self.channel_trees[channel][line_offset][beat_index].replace_with(tree)

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

        for i, channel_lines in enumerate(self.channel_trees):
            if not channel_lines:
                continue

            strlines = []
            for line in channel_lines:
                line.clear_singles()
                strlines.append(line.to_string())

            hexrep = hex(i)[2:].upper()
            with open(f"{fullpath}/channel_{hexrep}", "w", encoding="utf-8") as fp:
                fp.write("\n".join(strlines))

    def set_percussion_instrument(self, line_offset: int, instrument: int) -> None:
        """
            Register an instrument to use on a percussion line.
            Updates existing events.
        """
        self.percussion_map[line_offset] = instrument

        # Traverse the line and set all the events to the new instrument
        stack = [self.channel_trees[9][line_offset]]
        while stack:
            tree = stack.pop(0)
            if tree.is_event():
                event = tree.get_event()
                event.note = instrument
                tree.set_event(event)
            elif not tree.is_leaf():
                for subtree in tree:
                    stack.append(subtree)

    def swap_channels(self, channel_a: int, channel_b: int) -> None:
        """Swap the active lines of two channels."""
        tmp = self.channel_trees[channel_b]
        self.channel_trees[channel_b] = self.channel_trees[channel_a]
        self.channel_trees[channel_a] = tmp

def split_by_channel(event, other_events):
    return event['channel']

def split_by_note_order(event, other_events):
    e_notes = [e.note for e in other_events]
    e_notes.sort()
    return e_notes.index(event.note)
