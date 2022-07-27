from __future__ import annotations
from typing import Optional, List, Tuple, Dict
from apres import NoteOn, NoteOff, PitchWheelChange, MIDI, SetTempo
from structures import Grouping, BadStateError

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
    CH_DOWN ="!"
    CH_HOLD = "~"

    # NOTE: CH_CLOPEN is a CH_CLOSE, CH_NEXT, and CH_OPEN in that order
    CH_CLOPEN = "|"
    REL_CHARS = (CH_ADD, CH_SUBTRACT, CH_UP, CH_DOWN, CH_HOLD)

    SPECIAL_CHARS = (CH_OPEN, CH_CLOSE, CH_NEXT, CH_CLOPEN, CH_ADD, CH_SUBTRACT, CH_UP, CH_DOWN, CH_HOLD)

    @staticmethod
    def from_string(repstring: str, base: int = 12):
        # NOTE: Should the pitch bend be solely based on the fraction or should it consider the (1 / base)?

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

        for i, character in enumerate(repstring):
            if character in (MGrouping.CH_CLOSE, MGrouping.CH_CLOPEN):
                # Remove completed grouping from stack
                grouping_stack.pop()
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

            if relative_flag is not None:
                odd_note = previous_note
                if relative_flag == MGrouping.CH_SUBTRACT:
                    odd_note -= int(character, base)
                elif relative_flag == MGrouping.CH_ADD:
                    odd_note += int(character, base)
                elif relative_flag == MGrouping.CH_UP:
                    odd_note += int(character, base) * base
                elif relative_flag == MGrouping.CH_DOWN:
                    odd_note -= int(character, base) * base
                elif relative_flag == MGrouping.CH_HOLD:
                    pass

                leaf = grouping_stack[-1][-1]
                try:
                    note, bend = get_bend_values(odd_note, base)
                    note -= 3 # Use A as first instead of C
                    leaf.add_event((note, bend, relative_flag != MGrouping.CH_HOLD))
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
                        leaf.add_event((note, bend, True))
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
        for m in range(len(grouping)):
            beat = grouping[m]
            beat.flatten()
            div_size = midi.ppqn / len(beat)
            open_events = []
            for i, subgrouping in enumerate(beat):
                for event in subgrouping.events:
                    if not event:
                        continue

                    open_events.append(event)
                    note, pitch_bend, new_press = event
                    if new_press:
                        if pitch_bend != 0:
                            midi.add_event(
                                PitchWheelChange(
                                    channel=track,
                                    value=pitch_bend
                                ),
                                tick=int(current_tick + (i * div_size)),
                            )

                        midi.add_event(
                            NoteOn(
                                note=note,
                                channel=track,
                                velocity=64
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
                        running_pitchweel_revert = PitchWheelChange(
                            channel=track,
                            value=0
                        )
                        midi.add_event(
                            running_pitchwheel_revert,
                            tick=int(current_tick + ((i + 1) * div_size)),
                        )

                    running_note_off = NoteOff(
                        note=note,
                        channel=track
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


if __name__ == "__main__":
    import sys
    args = []
    kwargs = {}
    active_kwarg = None
    for value in sys.argv:
        if '--' == value[0:2]:
            active_kwarg = value[2:]
        elif active_kwarg is not None:
            kwargs[active_kwarg] = value
            active_kwarg = None
        else:
            args.append(value)

    base = kwargs.get('base', 12)
    tempo = int(kwargs.get('tempo', 80))
    start = kwargs.get('start', 0)
    end = kwargs.get('end', None)

    content = ""
    with open(sys.argv[1], 'r') as fp:
        content = fp.read()

    chunks = content.split("====")

    opus: List[MGrouping] = []
    for _ in range(16):
        opus.append(MGrouping())

    for x, chunk in enumerate(chunks):
        grouping = MGrouping.from_string(chunk)
        opus[x] = grouping

    midi = to_midi(opus, tempo=tempo)
    midi_name = sys.argv[1][0:sys.argv[1].rfind('.')] + ".mid"
    midi.save(midi_name)
