from __future__ import annotations
from typing import Optional, List, Tuple, Dict
from apres import NoteOn, NoteOff, PitchWheelChange, MIDI, SetTempo, ProgramChange, TimeSignature
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
        msg += "Can't place notes in structural subgrouping or vice versa. "
        msg += f"You likely missed a \"{MGrouping.CH_NEXT}\" Here:\n"
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

        msg = f"Unmatched \"{MGrouping.CH_OPEN}\" or \"{MGrouping.CH_CLOPEN}\" "
        msg += f"at position {index}: \n"

        if bound_a > 0:
            chunk = "..." + chunk[3:]
        if bound_b < len(repstring):
            chunk = chunk[0:-3] + "..."
        msg += ("-" * (index - bound_a)) + "!\n"
        msg += chunk + "\n"
        msg += ("-" * (index - bound_a)) + "^"
        super().__init__(msg)

def is_note_off(event):
    ''' checks if event is *effectively* a noteOff event '''
    # NoteOn with 0 velocity are treated as note off

    return (
        isinstance(event, NoteOn) and
        event.channel != 9 and
        event.velocity == 0
    ) or (
        isinstance(event, NoteOff) and
        event.channel != 9
    )

def is_note_on(event):
    ''' checks if event is *effectively* a noteOn event '''
    return (
        isinstance(event, NoteOn) and
        event.channel != 9 and
        event.velocity > 0
    )

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

    # REL_CHARS are the the characters that flag a relative note
    REL_CHARS = (
        CH_ADD,
        CH_SUBTRACT,
        CH_UP,
        CH_DOWN,
        CH_HOLD,
        CH_REPEAT
    )

    SPECIAL_CHARS = (
        CH_OPEN,
        CH_CLOSE,
        CH_NEXT,
        CH_CLOPEN,
        CH_ADD,
        CH_SUBTRACT,
        CH_UP,
        CH_DOWN,
        CH_HOLD,
        CH_REPEAT
    )

    @staticmethod
    def _channel_splitter(event):
        return event[3]

    def split(self, default_func=None):
        if default_func is None:
            default_func = self._channel_splitter
        return super().split(default_func)

    def to_midi(self, **kwargs) -> MIDI:
        tempo = int(kwargs.get('tempo', 80))
        start = int(kwargs.get('start', 0))
        end = kwargs.get('end', None)
        if end is not None:
            end = int(end)

        if end is None:
            slice_end = len(self)
        else:
            slice_end = end

        new_opus = MGrouping()

        if slice_end - start < len(self):
            new_opus.set_size(slice_end - start)
            sliced = self[start:min(len(self), slice_end)]
            for j, subgrouping in enumerate(sliced):
                new_opus[j].merge(subgrouping)
        else:
            new_opus = self

        midi = MIDI()

        for i in range(16):
            midi.add_event(
                ProgramChange(
                    int(kwargs.get(f"i{i}", 0)),
                    channel=i
                ),
                tick=0
            )

        midi.add_event( SetTempo.from_bpm(tempo) )
        tracks = new_opus.split()
        for track, grouping in enumerate(tracks):
            if not grouping.is_structural():
                continue

            current_tick = 0
            running_note_off = None
            running_pitchwheel_revert = None
            for m, beat in enumerate(grouping):
                if not beat.is_structural():
                    parent_grouping = MGrouping()
                    parent_grouping.set_size(1)
                    parent_grouping[0] = beat
                    beat = parent_grouping

                if not beat.is_flat():
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
                                    velocity=int(kwargs.get(f"v{channel}", 100))
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
                            #tick=int(current_tick + ((i + 1) * div_size)),
                            tick=int(current_tick + 120),
                        )

                current_tick += midi.ppqn

        return midi

    @staticmethod
    def from_string(repstring: str, **kwargs):
        # NOTE: Should the pitch bend be solely based on the
        #   fraction or should it consider the (1 / base)?
        base = kwargs.get("base", 12)
        channel = kwargs.get("channel", 0)

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
                parent = grouping_stack[-1].get_parent()
                if parent is not None:
                    to_copy = parent[repeat]
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

    def to_string(self, base=12, depth=0) -> str:
        if self.is_structural():
            strreps = []
            for i in range(len(self)):
                strreps.append(self[i].to_string(base, depth+1))

            if (depth == 0):
                output = ""
                for i, chunk in enumerate(strreps):
                    #Special Case
                    if depth == 0 and chunk == "__":
                        chunk = f"{self.CH_OPEN}{chunk}{self.CH_CLOSE}"

                    output += chunk + self.CH_NEXT
                    if False and i % 4 == 3:
                        output += "\n"

                while output[-1] in f"\n{self.CH_NEXT}":
                    output = output[0:-1]
            else:
                output = self.CH_NEXT.join(strreps)

            if depth > 0:
                output = f"{self.CH_OPEN}{output}{self.CH_CLOSE}"

        elif self.is_event():
            output = ""
            for note, _, new_press,_ in self.get_events():
                if new_press:
                    bignum = (note + 3) // base
                    littlenum = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"[(note + 3) % base]
                    output += f"{bignum}{littlenum}"
                else:
                    output += f"{self.CH_HOLD}{self.CH_HOLD}"
        else:
            output = "__"

#        needs_convert = f"{self.CH_CLOSE}{self.CH_NEXT}{self.CH_OPEN}"
#        while needs_convert in output:
#            output = output.replace(needs_convert, self.CH_CLOPEN)

        return output

    def __str__(self):
        return self.to_string()

    def set_note(self, note: int) -> None:
        if self.is_event():
            self.clear_events()

        self.add_event(note)

    def unset_note(self) -> None:
        self.clear_events()

    @staticmethod
    def from_midi(midi) -> MGrouping:
        beat_size = midi.ppqn
        total_beat_offset = 0
        last_ts_change = 0

        beat_values = {}
        max_tick = 0
        press_map = {}
        for tick, event in midi.get_all_events():
            max_tick = max(tick, tick)
            beat_index = ((tick - last_ts_change) // beat_size) + total_beat_offset
            inner_beat_offset = (tick - last_ts_change) % beat_size

            if is_note_on(event):
                if event.note not in beat_values:
                    beat_values[event.note] = []

                while len(beat_values[event.note]) <= beat_index:
                    new_grouping = MGrouping()
                    new_grouping.set_size(beat_size)
                    beat_values[event.note].append(new_grouping)

                grouping = beat_values[event.note][beat_index]
                grouping[inner_beat_offset].add_event((event.note, 0, True, event.channel))
                press_map[event.note] = (beat_index, inner_beat_offset)

            elif is_note_off(event):
                if not press_map.get(event.note, False):
                    continue

                if event.note not in beat_values:
                    beat_values[event.note] = []

                while len(beat_values[event.note]) <= beat_index:
                    new_grouping = MGrouping()
                    new_grouping.set_size(beat_size)
                    beat_values[event.note].append(new_grouping)

                original_index = press_map[event.note]

                # Add filler holds for all the groupings in between press and the release beat
                for i in range(original_index[0] + 1, beat_index):
                    grouping = beat_values[event.note][i]
                    for j in range(len(grouping)):
                        grouping[j].add_event((event.note, 0, False, event.channel))

                grouping = beat_values[event.note][beat_index]
                if original_index[0] != beat_index:
                    # Add holds on the current beat up to the current inner beat_offset
                    for i in range(inner_beat_offset):
                        grouping[i].add_event((event.note, 0, False, event.channel))
                else:
                    # Add holds for the remainder of the inner grouping
                    for i in range(original_index[1] + 1, len(beat_values[event.note][original_index[0]])):
                        grouping = beat_values[event.note][original_index[0]]
                        grouping[i].add_event((event.note, 0, False, event.channel))

                    # Add holds between the inner offsets
                    for i in range(original_index[1] + 1, inner_beat_offset):
                        grouping[i].add_event((event.note, 0, False, event.channel))

                press_map[event.note] = False

            elif isinstance(event, TimeSignature):
                total_beat_offset += (tick - last_ts_change) // beat_size
                last_ts_change = tick
                beat_size = int(midi.ppqn // ((2 ** event.denominator) / 4))

            elif isinstance(event, SetTempo):
                # TODO?
                pass

        total_beat_offset += (max_tick - last_ts_change) // beat_size
        # Add an extra beat for the midis where the final note isn't on the end of the final beat
        total_beat_offset += 1

        opus = MGrouping()
        opus.set_size(1)

        ordered_keys = list(beat_values.keys())
        ordered_keys.sort()
        for note in ordered_keys:
            beats = beat_values[note]
            new_grouping = MGrouping()
            new_grouping.set_size(total_beat_offset)
            for i, beat_grouping in enumerate(beats):
                new_grouping[i] = beat_grouping

            opus.merge(new_grouping)

        return opus


def get_bend_values(offset, base) -> Tuple[int, float]:
    """Convert an odd-based note to base-12 note with pitch bend"""
    v = (12 * offset) / base
    note = int(v)
    bend = v - note
    return (note, bend)
