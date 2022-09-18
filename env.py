import os
import re
import wrecked
from apres import MIDI
from mgrouping import MGrouping

class NoterEnvironment:
    def __init__(self):
        self.root = wrecked.init()
        self.channel_rects = []
        for i in range(16):
            self.channel_rects.append([])

        self.opus_manager = OpusManager()

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
    def __init__(self):
        self.channel_groupings = []
        for i in range(16):
            self.channel_groupings.append([])

        self.channel_order = list(range(16))

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
            self.channel_groupings[i].append(mgrouping)

    def kill(self):
        wrecked.kill()

def split_by_channel(event, other_events):
    return event[3]
