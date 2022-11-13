""" Class definition of requisite component classes of the Flag Layer. """
from __future__ import annotations
from typing import Optional, Dict, List, Tuple, Any

from .miditree import MIDITree
from .layer_base import OpusManagerBase, BeatKey

class UpdatesCache:
    """
        Dict of changes with a few ease-of-use functions.
        Used by FlagLayer to cache the changes to the OpusManager
        such so a GUI can more quickly and easily update itself.
    """
    def __init__(self):
        self.cache: Dict = {}

    def flag(self, key: str, value: Any) -> None:
        """Flag a change, caching the specifics."""
        if key not in self.cache:
            self.cache[key] = []
        self.cache[key].append(value)

    def unflag(self, key: str, value: Any) -> None:
        """Cancel a flagged changed."""
        if key not in self.cache:
            return
        self.cache[key].remove(value)

    def fetch(self, key: str, noclobber: bool = False) -> List[Any]:
        """
            Get the flagged changes as a list.
            Clears out the list unless *noclobber* is True
        """
        output = self.cache.get(key, [])
        if not noclobber:
            self.cache[key] = []
        return output

class FlagLayer(OpusManagerBase):
    """
        Flags changes made to the OpusManager
        so a GUI can update itself accordingly
    """
    def __init__(self):
        super().__init__()
        self.updates_cache = UpdatesCache()

    ## Layer-Specific Functions
    def flag(self, key: str, value: Any) -> None:
        """Wrapper for UpdatesCache's flag() method"""
        self.updates_cache.flag(key, value)

    def fetch(self, key: str, noclobber: bool = False) -> List[Any]:
        """Wrapper for UpdatesCache's fetch() method"""
        return self.updates_cache.fetch(key, noclobber)

    ## OpusManagerBase Functions
    def replace_tree(
            self,
            beat_key: BeatKey,
            position: List[int],
            tree: MIDITree) -> None:
        super().replace_tree(beat_key, position, tree)
        self.flag('beat_change', beat_key)

    def overwrite_beat(
            self,
            old_beat: BeatKey,
            new_beat: BeatKey) -> None:
        super().overwrite_beat(old_beat, new_beat)
        self.flag('beat_change', old_beat)

    def unlink_beat(self, beat_key: BeatKey) -> None:
        super().unlink_beat(beat_key)
        self.flag('beat_change', beat_key)

    def link_beats(
            self,
            beat: BeatKey,
            target: BeatKey) -> None:
        super().link_beats(beat, target)
        self.flag('beat_change', beat)

    def insert_after(self, beat_key: BeatKey, position: List[int]) -> None:
        super().insert_after(beat_key, position)
        self.flag('beat_change', beat_key)

    def split_tree(
            self,
            beat_key: BeatKey,
            position: List[int],
            splits: int) -> None:
        super().split_tree(beat_key, position, splits)
        self.flag('beat_change', beat_key)

    def swap_channels(self, channel_a: int, channel_b: int) -> None:
        super().swap_channels(channel_a, channel_b)

        len_a = len(self.channel_trees[channel_a])
        len_b = len(self.channel_trees[channel_b])
        for i in range(len_b):
            self.flag('line', (channel_a, len_b - 1 - i, 'pop'))

        for i in range(len_a):
            self.flag('line', (channel_b, len_a - 1 - i, 'pop'))

        for i in range(len_a):
            self.flag('line', (channel_a, i, 'new'))

        for i in range(len_b):
            self.flag('line', (channel_b, i, 'new'))

    def _new(self) -> None:
        super()._new()

        for i in range(self.opus_beat_count):
            self.flag('beat', (i, 'new'))

        for i, channel in enumerate(self.channel_trees):
            for j, _line in enumerate(channel):
                self.flag('line', (i, j, 'init'))

    def _load(self, path: str) -> None:
        super()._load(path)

        for i in range(self.opus_beat_count):
            self.flag('beat', (i, 'new'))

        for i, channel in enumerate(self.channel_trees):
            for j, _line in enumerate(channel):
                self.flag('line', (i, j, 'init'))

    def set_event(
            self,
            beat_key: BeatKey,
            position: List[int],
            value: int,
            *,
            relative: bool = False) -> None:
        super().set_event(beat_key, position, value, relative=relative)
        self.flag('beat_change', beat_key)

    def set_percussion_event(
            self,
            beat_key: BeatKey,
            position: List[int]) -> None:
        super().set_percussion_event(beat_key, position)
        self.flag('beat_change', beat_key)

    def set_percussion_instrument(self, line_offset: int, instrument: int) -> None:
        super().set_percussion_instrument(line_offset, instrument)
        for i in range(self.opus_beat_count):
            self.flag('beat_change', (9, line_offset, i))

    def unset(self, beat_key: BeatKey, position: List[int]) -> None:
        super().unset(beat_key, position)
        self.flag('beat_change', beat_key)

    def insert_beat(self, index: Optional[int] = None) -> None:
        if index is None:
            rindex = self.opus_beat_count - 1
        else:
            rindex = index

        super().insert_beat(index)

        self.flag('beat', (rindex, 'new'))

    def new_line(self,
            channel: int = 0,
            index: Optional[int] = None) -> None:
        super().new_line(channel, index)

        if index is None:
            line_index = len(self.channel_trees[channel]) - 1
        else:
            line_index = index

        self.flag('line', (channel, line_index, 'new'))

    def remove(self, beat_key: BeatKey, position: List[int]) -> None:
        super().remove(beat_key, position)
        self.flag('beat_change', beat_key)

    def remove_beat(self, index: None) -> None:
        if index is None:
            rindex = self.opus_beat_count - 1
        else:
            rindex = index

        super().remove_beat(index)

        self.flag('beat', (rindex, 'pop'))

    def remove_line(
            self,
            channel: int,
            index: Optional[int] = None) -> None:
        super().remove_line(channel, index)

        if index is None:
            index = len(self.channel_trees[channel])

        # Flag changes to cache
        self.flag('line', (channel, index, 'pop'))
