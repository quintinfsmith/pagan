from __future__ import annotations
import os
import json

from typing import Optional, Dict, List, Tuple, TypeAlias

from .miditree import MIDITreeEvent
from .layer_base import OpusManagerBase, BeatKey

class LinksLayer(OpusManagerBase):
    """Adds linked beats."""
    def __init__(self):
        super().__init__()
        self.linked_beat_map = {}
        self.inv_linked_beat_map = {}

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

    def clear_links_to_beat(self, target: BeatKey) -> None:
        """Remove all links pointing to the specific beat"""
        if target not in self.inv_linked_beat_map:
            return

        while self.inv_linked_beat_map[target]:
            linked_key = self.inv_linked_beat_map[target].pop()
            del self.linked_beat_map[linked_key]
        del self.inv_linked_beat_map[target]

    def link_beats(self, beat: BeatKey, target: BeatKey) -> None:
        """
            Overwrites *beat* with a copy of *target*,
            then creates a link between the two such that
            any changes made to one are made to the other.
        """
        # Remove any existing link
        self.unlink_beat(beat)

        # Replace existing tree with a copy of the target
        self.overwrite_beat(beat, target)

        self.linked_beat_map[beat] = target
        if target not in self.inv_linked_beat_map:
            self.inv_linked_beat_map[target] = set()
        self.inv_linked_beat_map[target].add(beat)


    def _get_all_linked(self, beat_key: BeatKey) -> List[BeatKey]:
        """
            Get a list of all positions of beats that will be affected
            by changes made to the one at the given position (ie, linked)
        """
        if beat_key in self.inv_linked_beat_map:
            positions = [beat_key]
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

    def insert_after(self, beat_key: BeatKey, position: List[int]) -> None:
        for linked_key in self._get_all_linked(beat_key):
            super().insert_after(linked_key, position)

    def remove(self, beat_key: BeatKey, position: List[int]):
        for linked_key in self._get_all_linked(beat_key):
            super().remove(linked_key, position)

    def set_percussion_event(self, beat_key: BeatKey, position: List[int]) -> None:
        for linked_key in self._get_all_linked(beat_key):
            super().set_percussion_event(linked_key, position)

    def set_event(
            self,
            beat_key: BeatKey,
            position: List[int],
            event: MIDITreeEvent) -> None:
        for linked_key in self._get_all_linked(beat_key):
            super().set_event(linked_key, position, event)

    def split_tree(
            self,
            beat_key: BeatKey,
            position: List[int],
            splits: int) -> None:
        for linked_key in self._get_all_linked(beat_key):
            super().split_tree(linked_key, position, splits)

    def unset(
            self,
            beat_key: BeatKey,
            position: List[int]) -> None:
        for linked_key in self._get_all_linked(beat_key):
            super().unset(linked_key, position)

    def remap_links(self, remap_hook, *args):
        """Adjust the linked beats according to the remap hook"""
        new_link_map = {}
        self.inv_linked_beat_map = {}

        for beat, target in self.linked_beat_map.items():
            new_beat = remap_hook(beat, *args)
            new_target = remap_hook(target, *args)

            if new_beat is None or new_target is None:
                continue

            new_link_map[new_beat] = new_target
            if new_target not in self.inv_linked_beat_map:
                self.inv_linked_beat_map[new_target] = []
            self.inv_linked_beat_map[new_target].append(new_beat)
        self.linked_beat_map = new_link_map

    def change_line_channel(self, old_channel: int, line_index: int, new_channel: int) -> None:
        super().change_line_channel(old_channel, line_index, new_channel)

        def remap_hook(beat, old_channel, line_index, new_channel, new_offset):
            new_beat = beat
            if beat[0] == old_channel:
                if beat[1] == line_index:
                    new_beat = (new_channel, new_offset, beat[2])
                elif beat[1] > line_index:
                    new_beat = (beat[0], beat[1] - 1, beat[2])
            return new_beat

        new_offset = len(self.channel_trees[new_channel]) - 1
        self.remap_links(remap_hook, old_channel, line_index, new_channel, new_offset)

    def move_line(self, channel: int, old_index: int, new_index: int) -> None:
        super().move_line(channel, old_index, new_index)

        def remap_hook(beat, channel, old_index, new_index):
            new_beat = beat
            if beat[0] == channel:
                if beat[1] == old_index:
                    new_beat = (beat[0], new_index, beat[2])
                elif old_index < beat[1] < new_index:
                    new_beat = (beat[0], beat[1] - 1, beat[2])
            return new_beat

        self.remap_links(remap_hook, channel, old_index, new_index)

    def insert_beat(self, index: Optional[int] = None) -> None:
        super().insert_beat(index)

        def remap_hook(beat, index):
            if beat[2] >= index:
                new_beat = (beat[0], beat[1], beat[2] + 1)
            else:
                new_beat = beat

            return new_beat

        self.remap_links(remap_hook, index)

    def remove_beat(self, index: int) -> None:
        super().remove_beat(index)

        def remap_hook(beat, index):
            if beat[2] > index:
                new_beat = (beat[0], beat[1], beat[2] - 1)
            else:
                new_beat = beat
            return new_beat

        self.remap_links(remap_hook, index)

    def remove_channel(self, channel: int) -> None:
        super().remove_channel(channel)

        def remap_hook(beat, channel):
            if beat[0] == channel:
                new_beat = None
            else:
                new_beat = beat
            return new_beat

        self.remap_links(remap_hook, channel)

    def remove_line(self, channel: int, index: Optional[int] = None) -> None:
        super().remove_line(channel, index)

        def remap_hook(beat, channel, index):
            new_beat = beat
            if beat[0] == channel:
                if beat[1] == index:
                    new_beat = None
                elif beat[1] > index:
                    new_beat = (beat[0], beat[1] - 1, beat[2])

            return new_beat

        self.remap_links(remap_hook, channel, index)

    def swap_channels(self, channel_a: int, channel_b: int) -> None:
        super().swap_channels(channel_a, channel_b)

        def remap_hook(beat, channel_a, channel_b):
            if beat[0] == channel_a:
                new_beat = (channel_b, beat[1], beat[2])
            elif beat[0] == channel_b:
                new_beat = (channel_a, beat[1], beat[2])
            else:
                new_beat = beat
            return new_beat

        self.remap_links(remap_hook, channel_a, channel_b)

    def load_folder(self, path: str) -> None:
        super().load_folder(path)

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

    def save(self, path: Optional[str] = None) -> None:
        super().save(path)

        fullpath = self.get_working_dir()

        json_compat_map = {}
        for k, target in self.linked_beat_map.items():
            new_key = f"{k[0]}.{k[1]}.{k[2]}"
            json_compat_map[new_key] = target

        with open(f"{fullpath}/linkedbeats.json", "w", encoding="utf-8") as fp:
            fp.write(json.dumps(json_compat_map, indent=4))
