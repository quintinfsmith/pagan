from __future__ import annotations
from typing import Optional, List, Tuple, Dict
from apres import NoteOn, NoteOff, PitchWheelChange, MIDI, SetTempo, ProgramChange
from structures import Grouping, BadStateError
import re

ERROR_CHUNK_SIZE = 19

class MissingCommaError(Exception):
    """Thrown when MGrouping.from_string() fails"""
    def __init__(self, repstring, fail_index, beat):
        bound_a = max(0, fail_index - ERROR_CHUNK_SIZE)
        bound_b = min(len(repstring), fail_index + ERROR_CHUNK_SIZE)
        chunk = repstring[bound_a:fail_index] + " " + repstring[fail_index:bound_b]
        if bound_a > 0:
            chunk = "..." + chunk[3:]
        if bound_b < len(repstring):
            chunk = chunk[0:-3] + "..."
        msg = f"\nError in beat {beat}\n"
        msg += f"Can't place notes in structural subgrouping or vice versa. You likely missed a \"{MGrouping.CH_NEXT}\" Here:\n"
        msg += ("-" * (fail_index - bound_a)) + "!\n"
        msg += chunk + "\n"
        msg += ("-" * (fail_index - bound_a)) + "^"
        super().__init__(msg)

class UnclosedGroupingError(Exception):
    """Thrown when a grouping is opened but not closed"""
    def __init__(self, repstring, index):
        bound_a = max(0, index - ERROR_CHUNK_SIZE)
        bound_b = min(len(repstring), index + ERROR_CHUNK_SIZE)
        chunk = repstring[bound_a:bound_b]
        msg = f"Unmatched \"{MGrouping.CH_OPEN}\" or \"{MGrouping.CH_CLOPEN}\" at position {index}: \n"
        if bound_a > 0:
            chunk = "..." + chunk[3:]
        if bound_b < len(repstring):
            chunk = chunk[0:-3] + "..."
        msg += ("-" * (index - bound_a)) + "!\n"
        msg += chunk + "\n"
        msg += ("-" * (index - bound_a)) + "^"
        super().__init__(msg)


class MGrouping(Grouping):
    CH_OPEN = "["
    CH_CLOSE = "]"
    CH_NEXT = ","
    CH_ADD = "+"
    CH_SUBTRACT = "-"
    CH_UP = "^"
    CH_DOWN ="v"
    CH_HOLD = "~"
    CH_REPEAT = "="

    # NOTE: CH_CLOPEN is a CH_CLOSE, CH_NEXT, and CH_OPEN in that order
    CH_CLOPEN = "|"
    REL_CHARS = (CH_ADD, CH_SUBTRACT, CH_UP, CH_DOWN, CH_HOLD, CH_REPEAT)

    SPECIAL_CHARS = (CH_OPEN, CH_CLOSE, CH_NEXT, CH_CLOPEN, CH_ADD, CH_SUBTRACT, CH_UP, CH_DOWN, CH_HOLD, CH_REPEAT)


    @staticmethod
    def from_string(repstring: str, **kwargs):
        # NOTE: Should the pitch bend be solely based on the fraction or should it consider the (1 / base)?
        base = kwargs.get("base", 12)
        channel = kwargs.get("channel", 0)

        print(repstring)
        # Remove all Whitespace
        repstring = repstring.strip()
        for character in " \n\t_":
            repstring = repstring.replace(character, "")

        output = MGrouping()
        output.set_size(1)
        grouping_stack: List[MGrouping] = [output]
        register: List[Optional[int], Optional[int], Optional[float]] = [None, None, 0]
        opened_indeces: List[int] = []
        previous_value: Optional[int] = None
        relative_flag: Optional[str] = None
        latest_grouping: Optional[MGrouping] = None

        for i, character in enumerate(repstring):
            if character in (MGrouping.CH_CLOSE, MGrouping.CH_CLOPEN):
                # Remove completed grouping from stack
                latest_grouping = grouping_stack.pop()
                opened_indeces.pop()

            if character in (MGrouping.CH_NEXT, MGrouping.CH_CLOPEN):
                # Back up existing divisions
                sub_divisions = grouping_stack[-1].divisions

                # Resize Active Grouping
                grouping_stack[-1].set_size(len(grouping_stack[-1]) + 1)

                # Replace Active Grouping's Divisions with backups
                grouping_stack[-1].divisions = sub_divisions

            if character in (MGrouping.CH_OPEN, MGrouping.CH_CLOPEN):
                new_grouping = grouping_stack[-1][-1]
                try:
                    new_grouping.set_size(1)
                except BadStateError as b:
                    raise MissingCommaError(repstring, i, len(output) - 1)
                grouping_stack.append(new_grouping)

                opened_indeces.append(i)

            elif relative_flag == MGrouping.CH_REPEAT:
                if character == MGrouping.CH_REPEAT:
                    repeat = -2
                else:
                    repeat = int(character, base)
                print(repeat)
                to_copy = grouping_stack[-1].parent[repeat]

                grouping_stack[-1][-1] = to_copy.copy()

                relative_flag = None
            elif relative_flag is not None:
                odd_note = previous_note
                if relative_flag == MGrouping.CH_SUBTRACT:
                    odd_note -= int(character, base)
                elif relative_flag == MGrouping.CH_ADD:
                    odd_note += int(character, base)
                elif relative_flag == MGrouping.CH_UP:
                    odd_note += int(character, base) * base
                elif relative_flag == MGrouping.CH_DOWN:
                    odd_note -= int(character, base) * base


                leaf = grouping_stack[-1][-1]
                try:
                    note, bend = get_bend_values(odd_note, base)
                    note -= 3 # Use A (-3) as first instead of C
                    leaf.add_event((note, bend, relative_flag != MGrouping.CH_HOLD, channel))
                except BadStateError as b:
                    raise MissingCommaError(repstring, i - 1, len(output) - 1) from b

                previous_note = odd_note
                relative_flag = None

            elif character in MGrouping.REL_CHARS:
                relative_flag = character

            elif character not in MGrouping.SPECIAL_CHARS:
                if register[0] is None:
                    register[0] = int(character, base)
                elif register[1] is None:
                    register[1] = int(character, base)

                    odd_note = (register[0] * base) + register[1]

                    leaf = grouping_stack[-1][-1]
                    try:
                        note, bend = get_bend_values(odd_note, base)
                        note -= 3 # Use A as first instead of C
                        leaf.add_event((note, bend, True, channel))
                    except BadStateError as b:
                        raise MissingCommaError(repstring, i - 1, len(output) - 1) from b
                    register = [None, None, 0]
                    previous_note = odd_note

        if len(grouping_stack) > 1:
            raise UnclosedGroupingError(repstring, opened_indeces.pop())

        return output

    def set_note(self, note: int) -> None:
        if self.is_event():
            self.clear_events()

        self.add_event(note)

    def unset_note(self) -> None:
        self.clear_events()

def to_midi(opus, **kwargs):
    tempo = 120
    if ('tempo' in kwargs):
        tempo = kwargs['tempo']

    midi = MIDI()

    imap = kwargs.get('imap', None)
    if imap is not None:
        for i, program in imap.items():
            midi.add_event(
                ProgramChange(
                    program,
                    channel=i
                ),
                tick=0
            )

    midi.add_event( SetTempo.from_bpm(tempo) )
    if opus.__class__ == list:
        tracks = opus
    else:
        tracks = [opus]

    for track, grouping in enumerate(tracks):
        if not grouping.is_structural():
            continue

        current_tick = 0
        running_note_off = None
        running_pitchwheel_revert = None
        for m, beat in enumerate(grouping):
            beat.flatten()
            div_size = midi.ppqn / len(beat)
            open_events = []
            for i, subgrouping in enumerate(beat):
                for event in subgrouping.events:
                    if not event:
                        continue

                    open_events.append(event)
                    note, pitch_bend, new_press, channel = event
                    if new_press:
                        if pitch_bend != 0:
                            midi.add_event(
                                PitchWheelChange(
                                    channel=channel,
                                    value=pitch_bend
                                ),
                                tick=int(current_tick + (i * div_size)),
                            )

                        midi.add_event(
                            NoteOn(
                                note=note,
                                channel=channel,
                                velocity=100
                            ),
                            tick=int(current_tick + (i * div_size)),
                        )
                    else:
                        if running_note_off is not None:
                            midi.detach_event(running_note_off.get_uuid())
                            running_note_off = None
                        if running_pitchwheel_revert is not None:
                            midi.detach_event(running_pitchwheel_revert.get_uuid())
                            running_pitchwheel_revert = None

                    if pitch_bend != 0:
                        running_pitchwheel_revert = PitchWheelChange(
                            channel=channel,
                            value=0
                        )
                        midi.add_event(
                            running_pitchwheel_revert,
                            tick=int(current_tick + ((i + 1) * div_size)),
                        )

                    running_note_off = NoteOff(
                        note=note,
                        channel=channel
                    )

                    midi.add_event(
                        running_note_off,
                        tick=int(current_tick + ((i + 1) * div_size)),
                    )

            current_tick += midi.ppqn

    return midi

def get_bend_values(offset, base) -> Tuple[int, float]:
    """Convert an odd-based note to base-12 note with pitch bend"""
    v = (12 * offset) / base
    note = int(v)
    bend = v - note
    #print(f"{offset}/{base} = {(note // 12)}{(note % 12)}/12 + {bend}")
    return (note, bend)

def build_from_directory(path, **kwargs):
    base = kwargs.get('base', 12)
    channel_map = {}
    suffix_patt = re.compile(".*_(?P<suffix>[0-9A-Z]?)(\..*)?", re.I)
    filenames = os.listdir(path)
    filenames_clean = []
    # create a reference map of channels and rremove non-suffixed files from the list
    for filename in filenames:
        channel = None
        for hit in suffix_patt.finditer(filename):
            channel = int(hit.group('suffix'), 16)
        if channel is not None:
            channel_map[filename] = channel
            filenames_clean.append(filename)

    output = []
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
            output.append(grouping)

    return output


def build_from_single_file(path, **kwargs):
    base = kwargs.get('base', 12)
    content = ""
    with open(path, 'r') as fp:
        content = fp.read()

    chunks = content.split("\n[")
    for i, chunk in enumerate(chunks):
        if i > 0:
            chunks[i] = f"[{chunk}"

    opus: List[MGrouping] = []

    for x, chunk in enumerate(chunks):
        grouping = MGrouping.from_string(chunk, base=base, channel=x)
        opus.append(grouping)

    return opus

def get_sys_args():
    import sys
    args = []
    kwargs = {}
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

if __name__ == "__main__":
    import os
    args, kwargs = get_sys_args()

    base = int(kwargs.get('base', 12))
    tempo = int(kwargs.get('tempo', 80))
    start = int(kwargs.get('start', 0))
    end = kwargs.get('end', None)
    if end is not None:
        end = int(end)

    path = args[0]
    if os.path.isfile(path):
        opus = build_from_single_file(path, base=base)
        if "." in path:
            midi_name = path[0:path.rfind('.')] + ".mid"
        else:
            midi_name = f"{path}.mid"
    elif os.path.isdir(path):
        opus = build_from_directory(path, base=base)
        midi_name = f"{path}.mid"



    for i, grouping in enumerate(opus):
        if end is None:
            slice_end = len(grouping)
        else:
            slice_end = end

        if slice_end - start < len(grouping):
            new_grouping = Grouping()
            new_grouping.set_size(slice_end - start)
            sliced = grouping[start:min(len(grouping), slice_end)]
            for i, subgrouping in enumerate(sliced):
                new_grouping[i] = subgrouping
            opus[i] = new_grouping


    imap={}
    for i in range(16):
        iset = int(kwargs.get(f"i{i}", 0))
        imap[i] = iset

    midi = to_midi(opus, tempo=tempo, imap=imap)

    midi.save(midi_name)
