from __future__ import annotations
from typing import Optional, List, Tuple, Dict

import re
import os
from apres import MIDI
from mgrouping import MGrouping

def build_from_directory(path, **kwargs) -> MIDI:
    base = int(kwargs.get('base', 12))

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

    opus = MGrouping()
    opus.set_size(1)

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
                opus.set_size(beat_count, True)
                for i in range(beat_count):
                    opus[i].merge(grouping[i])

    return opus.to_midi(**kwargs)

def build_from_single_file(path, **kwargs) -> MIDI:
    base = int(kwargs.get('base', 12))

    content = ""
    with open(path, 'r') as fp:
        content = fp.read()

    chunks = content.split("\n[")
    for i, chunk in enumerate(chunks):
        if i > 0:
            chunks[i] = f"[{chunk}"

    opus = MGrouping()
    opus.set_size(1)
    for x, chunk in enumerate(chunks):
        grouping = MGrouping.from_string(chunk, base=base, channel=x)
        opus.merge(grouping)
    return opus.to_midi(**kwargs)

def get_sys_args():
    import sys
    args = []
    kwargs = {}
    if len(sys.argv) > 1:
        active_kwarg = None
        for value in sys.argv[1:]:
            if '--' == value[0:2]:
                active_kwarg = value[2:]
            elif active_kwarg is not None:
                kwargs[active_kwarg] = value
                active_kwarg = None
            else:
                args.append(value)

    return (args, kwargs)
def split_by_note(event):
    return event[0]

def main():
    args, kwargs = get_sys_args()
    path = args[0]
    if path[path.rfind("."):].lower() == ".mid":
        midi = MIDI.load(path)
        output_type = kwargs.get("output", "stdout")
        if output_type != "stdout":
            if os.path.isdir(output_type):
                os.system("rm " + output_type.replace(" ", "\\ ") + " -rf")
            os.mkdir(output_type)
        opus = MGrouping.from_midi(midi)
        tracks = opus.split(split_by_note)

        notes_map = {}
        notes = []
        for i, mgrouping in enumerate(tracks):
            was_set = False
            for beat in mgrouping:
                try:
                    for sub in beat:
                        events = sub.get_events()
                        if len(events):
                            notes_map[list(events)[0][0]] = i
                            notes.append(list(events)[0][0])
                            was_set = True
                            break
                except:
                    continue

                if was_set:
                    break

        notes.sort()
        for i in notes[::-1]:
            mgrouping = tracks[notes_map[i]]
            if output_type == "stdout":
                print(str(mgrouping), "\n")
            else:
                with open(f"{output_type}/channel_00", "a", encoding="utf-8") as fp:
                    fp.write(str(mgrouping) + "\n")
    else:
        if os.path.isfile(path):
            midi = build_from_single_file(path, **kwargs)
            if "." in path:
                midi_name = path[0:path.rfind('.')] + ".mid"
            else:
                midi_name = f"{path}.mid"
        elif os.path.isdir(path):
            midi = build_from_directory(path, **kwargs)
            midi_name = f"{path}.mid"

        midi.save(midi_name)

if __name__ == "__main__":
    main()
