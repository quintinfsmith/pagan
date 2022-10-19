from __future__ import annotations
import os
import re

from inspect import signature
from typing import Optional, Dict, List, Tuple
from enum import Enum, auto
from apres import MIDI

from .structures import BadStateError
from .mgrouping import MGrouping, MGroupingEvent
from .interactor import Interactor

class InvalidCursor(Exception):
    '''Raised when attempting to pass a cursor without enough arguments'''

class OpusManager:
    BASE = 12
    def __init__(self):
        self.channel_groupings = [[] for i in range(16)]
        self.channel_order = list(range(16))
        self.cursor_position = [0, 0] # Y, X,... deeper divisions
        self.opus_beat_count = 1
        self.path = None
        self.clipboard_grouping = None
        self.flag_kill = False
        self.register = None

        self.history_ledger = []

        self.command_ledger = CommandLedger({
            'w': self.save,
            'q': self.kill,
            'c+': self.add_channel,
            'c-': self.remove_channel,
            'export': self.export,
            'swap': self.swap_channels
        })

    def new(self):
        self.history_ledger = []
        self.channel_groupings = [[] for i in range(16)]
        self.channel_order = list(range(16))
        self.cursor_position = [0, 0] # Y, X,... deeper divisions

        new_line = MGrouping()
        new_line.set_size(4)
        self.channel_groupings[0].append(new_line)
        self.opus_beat_count = 4

    def increment_event_at_cursor(self):
        position = self.cursor_position
        grouping = self.get_grouping(position)
        if not grouping.is_event():
            return

        for event in grouping.get_events():
            new_value = event.note
            if event.relative:
                if (event.note >= event.base \
                or event.note < 0 - event.base) \
                and event.note < (event.base * (event.base - 1)):
                    new_value = event.note + event.base
                elif event.note < event.base:
                    new_value = event.note + 1
            elif event.note < 127:
                new_value = event.note + 1

            self.set_event(
                new_value,
                position,
                relative=event.relative
            )

    def decrement_event_at_cursor(self):
        position = self.cursor_position
        grouping = self.get_grouping(position)
        if not grouping.is_event():
            return

        for event in grouping.get_events():
            new_value = event.note
            if event.relative:
                if (event.note <= 0 - event.base \
                or event.note > event.base) \
                and event.note > 0 - (event.base * (event.base - 1)):
                    new_value = event.note - event.base
                elif event.note >= 0 - event.base:
                    new_value = event.note - 1
            elif event.note > 0:
                new_value = event.note - 1

            self.set_event(
                new_value,
                position,
                relative=event.relative
            )


    def relative_add_entry(self):
        self.register = ReadyEvent(1, relative=True)

    def relative_subtract_entry(self):
        self.register = ReadyEvent(-1, relative=True)

    def relative_downshift_entry(self):
        self.register = ReadyEvent(-1 * self.BASE, relative=True)

    def relative_upshift_entry(self):
        self.register = ReadyEvent(self.BASE, relative=True)

    def add_digit_to_register(self, value):
        if self.register is None:
            self.register = ReadyEvent(value, relative=False)
        elif self.register.relative:
            self.register.value *= value
            self.apply_register_at_cursor()
        else:
            self.register.value *= self.BASE
            self.register.value += value
            if self.register.value >= self.BASE:
                self.apply_register_at_cursor()

    def apply_register_at_cursor(self):
        register = self.fetch_register()
        if register is None:
            return

        self.set_event(
            register.value,
            self.cursor_position,
            relative=register.relative
        )

    def clear_register(self):
        self.register = None

    def fetch_register(self):
        output = self.register
        self.register = None
        return output

    def insert_after_cursor(self):
        position = self.cursor_position
        self.insert_after(position)

        if len(position) == 2:
            self.cursor_dive(0)

    def copy_grouping(self, position):
        self.clipboard_grouping = self.get_grouping(position)

    def replace_grouping(self, position, grouping):
        self.get_grouping(position).replace_with(grouping)

    def paste_grouping(self, position):
        if self.clipboard_grouping is None:
            return

        self.replace_grouping(position, self.clipboard_grouping)
        self.clipboard_grouping = None

    def save(self, path=None):
        if path is None:
            path = self.path

        if path.lower().endswith("mid"):
            working_path = path[0:path.rfind("/")]
            working_dir = path[path.rfind("/") + 1:path.rfind(".")]
        elif os.path.isfile(path):
            working_path = path[0:path.rfind("/")]
            working_dir = path[path.rfind("/") + 1:path.rfind(".")]
        elif os.path.isdir(path):
            tmp_split = path.split("/")
            working_path = "/".join(tmp_split[0:-1])
            working_dir = tmp_split[-1]
        else:
            working_path = path[0:path.rfind("/") + 1]
            working_dir = path[path.rfind("/") + 1:]

        fullpath = f"{working_path}/{working_dir}"
        if not os.path.isdir(fullpath):
            os.mkdir(fullpath)

        for f in os.listdir(fullpath):
            os.remove(f"{fullpath}/{f}")

        for c, channel_lines in enumerate(self.channel_groupings):
            if not channel_lines:
                continue

            strlines = []
            for line in channel_lines:
                strlines.append(line.to_string())
            n = "0123456789ABCDEF"[c]
            with open(f"{fullpath}/channel_{n}", "w") as fp:
                fp.write("\n".join(strlines))

    def kill(self):
        self.flag_kill = True

    def export(self, path, *, tempo=120, transpose=-3):
        opus = MGrouping()
        opus.set_size(self.opus_beat_count)
        for groupings in self.channel_groupings:
            for grouping in groupings:
                for i, beat in enumerate(grouping):
                    opus[i].merge(beat)

        opus.to_midi(tempo=tempo, transpose=transpose).save(path)

    def set_active_beat_size(self, size):
        self.set_beat_size(size, self.cursor_position)

    def get_active_line(self):
        return self.get_line(self.cursor_position[0])

    def set_cursor_position(self, position):
        self.cursor_position = position

    def cursor_left(self):
        fully_left = True
        position = self.cursor_position.copy()
        while True:
            if position[-1] > 0:
                position[-1] -= 1
                fully_left = False
                break
            elif len(position) > 2:
                position.pop()
            else:
                break

        while (grouping := self.get_grouping(position)).is_structural():
            if not fully_left:
                position.append(len(grouping) - 1)
            else:
                # Move back to fully left position
                position.append(0)

        self.set_cursor_position(position)

    def cursor_right(self):
        fully_right = True
        position = self.cursor_position.copy()
        while True:
            parent_grouping = self.get_grouping(position[0:-1])
            if position[-1] < len(parent_grouping) - 1:
                position[-1] += 1
                fully_right = False
                break
            elif len(position) > 2:
                position.pop()
            else:
                break

        while (grouping := self.get_grouping(position)).is_structural():
            if not fully_right:
                position.append(0)
            else:
                # Move back to fully right position
                position.append(len(grouping) - 1)
        self.set_cursor_position(position)

    def cursor_up(self):
        working_position = self.cursor_position.copy()

        working_position[0] = max(0, working_position[0] - 1)
        working_position = working_position[0:2]

        while self.get_grouping(working_position).is_structural():
            working_position.append(0)

        self.set_cursor_position(working_position)

    def cursor_down(self):
        working_position = self.cursor_position.copy()

        if self.get_line(working_position[0] + 1) is not None:
            working_position[0] += 1

        working_position = working_position[0:2]

        while self.get_grouping(working_position).is_structural():
            working_position.append(0)

        self.set_cursor_position(working_position)

    def cursor_climb(self):
        position = self.cursor_position.copy()
        if len(position) > 2:
            position.pop()
        self.set_cursor_position(position)

    def cursor_dive(self, index):
        position = self.cursor_position.copy()
        if index >= len(self.get_grouping(position)):
            tmp = position.copy()
            tmp.append(index)
            raise InvalidCursor(tmp)

        position.append(index)

        self.set_cursor_position(position)

    def get_grouping(self, position):
        grouping = self.get_line(position[0])
        try:
            for i in position[1:]:
                grouping = grouping[i]
        except BadStateError as e:
            raise InvalidCursor from e

        return grouping

    def set_event(self, value, position, *, relative=False):
        channel, _index = self.get_channel_index(position[0])
        grouping = self.get_grouping(position)


        if grouping.is_structural():
            grouping.clear()
        elif grouping.is_event():
            grouping.clear_events()


        grouping.add_event(MGroupingEvent(
            value,
            base=self.BASE,
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

    def set_cursor(self, *position):
        if len(position) < 2:
            raise InvalidCursor(position)
        self.cursor_position = position

    def get_channel(self, y: int) -> int:
        return self.get_channel_index(y)[0]

    def get_channel_index(self, y_index: int) -> (int, int):
        '''
            Given the y-index of a line (as in from the cursor),
            get the channel and index thereof
        '''

        for channel in self.channel_order:
            for i, _ in enumerate(self.channel_groupings[channel]):
                if y_index == 0:
                    return (channel, i)
                y_index -= 1

        raise IndexError

    def get_y(self, c: int, i: Optional[int] = None) -> int:
        '''
            Given a channel and index,
            get the y-index of the specified line displayed
        '''
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

    def load(self, path: str) -> None:
        if os.path.isdir(path):
            self.load_folder(path)
        elif path[path.rfind("."):].lower() == ".mid":
            self.import_midi(path)
        else:
            self.load_file(path)

        # Adjust Cursor
        grouping = self.get_grouping(self.cursor_position)
        while True:
            try:
                grouping = grouping[0]
                self.cursor_position.append(0)
            except BadStateError:
                break

    def load_folder(self, path: str) -> None:
        self.path = path
        base = self.BASE

        channel_map = {}
        suffix_patt = re.compile(".*_(?P<suffix>([0-9A-Z]{1,3})?)(\\..*)?", re.I)
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
            channel = channel_map[filename]
            content = ""
            with open(f"{path}/{filename}", 'r') as fp:
                content = fp.read()

            chunks = content.split("\n[")
            for i, chunk in enumerate(chunks):
                if i > 0:
                    chunks[i] = f"[{chunk}"

            for x, chunk in enumerate(chunks):
                grouping = MGrouping.from_string(chunk, base=base, channel=channel)
                if grouping:
                    beat_count = max(len(grouping), beat_count)
                    grouping.set_size(beat_count, True)

                    self.channel_groupings[channel].append(grouping)

        self.opus_beat_count = beat_count


    def load_file(self, path: str) -> MGrouping:
        self.path = path
        base = 12

        content = ""
        with open(path, 'r') as fp:
            content = fp.read()

        chunks = content.split("\n[")
        for i, chunk in enumerate(chunks):
            if i > 0:
                chunks[i] = f"[{chunk}"

        self.opus_beat_count = 1
        for x, chunk in enumerate(chunks):
            grouping = MGrouping.from_string(chunk, base=base, channel=x)
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

    def split_grouping_at_cursor(self):
        cursor = self.cursor_position
        self.split_grouping(cursor, 2)
        self.cursor_position.append(0)

    def insert_after(self, position: List[int]):
        grouping = self.get_grouping(position)

        if len(position) == 2:
            if grouping.is_event() or grouping.is_open():
                parent = self.get_line(position[0])
                new_beat = MGrouping()
                parent[position[-1]] = new_beat
                new_beat.parent = parent
                new_beat.set_size(2)
                new_beat[0] = grouping
                grouping.parent = new_beat
        else:
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

    def unset_at_cursor(self):
        position = self.cursor_position
        self.unset(position)


    def unset(self, position):
        grouping = self.get_grouping(position)
        if grouping.is_event():
            grouping.clear_events()
        elif grouping.is_structural():
            grouping.clear()


    def remove(self, position: List[int]):
        index = position[-1]
        grouping = self.get_grouping(position)
        parent = grouping.parent
        new_size = len(parent) - 1

        # Attempting to remove beat
        if len(position) == 2:
            self.unset(position)
            return

        if new_size > 0:
            for i, child in enumerate(parent):
                if i < index or i == len(parent) - 1:
                    continue
                parent[i] = parent[i + 1]
            parent.set_size(new_size, True)

            # replace the parent with the child
            if new_size == 1:
                parent_index = position[-2]
                parent.parent[parent_index] = parent[0]
        else:
            self.remove(position[0:-1])


    def remove_beat_at_cursor(self):
        cursor = self.cursor_position
        self.remove_beat(cursor[1])

        cursor = cursor[0:2]
        # Adjust cursor
        while cursor[1] >= self.opus_beat_count:
            cursor[1] -= 1

        grouping = self.get_grouping(cursor)
        while grouping.is_structural():
            cursor.append(0)
            grouping = grouping[0]

        self.set_cursor_position(cursor)

    def remove_grouping_at_cursor(self):
        position = self.cursor_position
        self.remove(position)

        while True:
            try:
                self.get_grouping(position)
                break
            except InvalidCursor:
                position.pop()
            except IndexError:
                position[-1] -= 1

        self.set_cursor_position(position)


    def remove_beat(self, index):
        # Move all beats after removed index one left
        for channel in self.channel_groupings:
            for line in channel:
                line.pop(index)
        self.set_beat_count(self.opus_beat_count - 1)


    def insert_beat_at_cursor(self):
        cursor = self.cursor_position
        self.insert_beat(cursor[1] + 1)
        self.cursor_position = cursor[0:2]

    # TODO: Should this be a Grouping Method?
    def insert_beat(self, index=None):
        original_beat_count = self.opus_beat_count

        self.set_beat_count(original_beat_count + 1)
        if index >= original_beat_count - 1:
            return

        # Move all beats after new one right
        for channel in self.channel_groupings:
            for line in channel:
                tmp = line[-1]
                i = len(line) - 1
                while i > index:
                    line[i] = line[i - 1]
                    i -= 1
                line[i] = tmp

    def split_grouping(self, position, splits):
        grouping = self.get_grouping(position)
        if grouping.is_event():
            parent = self.get_grouping(position[0:-1])
            new_grouping = MGrouping()
            new_grouping.set_size(splits)
            new_grouping.parent = parent
            parent[position[-1]] = new_grouping
            new_grouping[0] = grouping
            grouping.parent = new_grouping
        else:
            grouping.set_size(splits, True)

    def new_line(self, channel=0, index=None):
        if not self.channel_groupings[channel]:
            try:
                current_channel, current_index = self.get_channel_index(self.cursor_position[0])
                if current_channel > channel:
                    self.cursor_position[0] += 1
            except IndexError:
                pass

        new_grouping = MGrouping()
        new_grouping.set_size(self.opus_beat_count)
        if index is not None:
            self.channel_groupings[channel].insert(index, new_grouping)
        else:
            self.channel_groupings[channel].append(new_grouping)

    def remove_line(self, channel, index=None):
        if index is None:
            index = len(self.channel_groupings[channel]) - 1

        cursor_channel, cursor_index = self.get_channel_index(self.cursor_position[0])
        if channel == cursor_channel and index == cursor_index:
            self.cursor_position[0] -= 1

        self.channel_groupings[channel].pop(index)
        new_position = self.cursor_position[0:2]
        while (grouping := self.get_grouping(new_position)).is_structural():
            new_position.append(0)

        self.set_cursor_position(new_position)

    def add_channel(self, channel):
        self.new_line(channel)

    def remove_channel(self, channel):
        if self.channel_groupings[channel]:
            current_channel, current_index = self.get_channel_index(self.cursor_position[0])
            if channel < current_channel:
                self.cursor_position[0] -= len(self.channel_groupings[channel])
            elif channel == current_channel:
                self.cursor_position[0] -= current_index + 1
            self.cursor_position[0] = max(0, self.cursor_position[0])

        while self.channel_groupings[channel]:
            self.remove_line(channel, 0)

    def swap_channels(self, channel_a, channel_b):
        o_channel, o_index = self.get_channel_index(self.cursor_position[0])

        tmp = self.channel_groupings[channel_b]
        self.channel_groupings[channel_b] = self.channel_groupings[channel_a]
        self.channel_groupings[channel_a] = tmp

        # Keep the cursor on the same channel, by moving to a lower index if necessary
        while o_index >= len(self.channel_groupings[o_channel]) - 1:
            o_index -= 1

        self.set_cursor_by_line(o_channel, o_index)

        position = self.cursor_position.copy()

        # Correct position
        while len(position) > 2:
            try:
                self.get_grouping(position)
                break
            except InvalidCursor:
                position.pop()
            except IndexError:
                position.pop()

        grouping = self.get_grouping(position)
        while grouping.is_structural():
            position.append(0)
            grouping = grouping[0]

        self.set_cursor_position(position)

    def set_cursor_by_line(self, target_channel, target_line):
        y = 0
        for i, channel in enumerate(self.channel_groupings):
            for j in range(len(channel)):
                if target_channel == i and target_line == j:
                    self.cursor_position[0] = y
                    break
                y += 1

    def new_line_at_cursor(self):
        y = self.cursor_position[0]
        channel, index = self.get_channel_index(y)
        self.new_line(channel)

    def remove_line_at_cursor(self):
        y = self.cursor_position[0]
        channel, index = self.get_channel_index(y)
        self.remove_line(channel, index)



class ReadyEvent:
    def __init__(self, initial_value, *, relative=False):
        self.value = initial_value
        self.relative = relative
        self.flag_changed = True

class InputContext(Enum):
    Default = auto()
    Text = auto()
    ConfirmOnly = auto()

class CommandLedger:
    def __init__(self, command_map):
        self.command_map = command_map
        self.history = []
        self.register = None
        self.active_entry = None
        self.register_bkp = None
        self.error_msg = None

    def get_error_msg(self):
        return self.error_msg

    def clear_error_msg(self):
        self.error_msg = None

    def set_error_msg(self, msg):
        self.error_msg = msg
        self.register = None
        self.active_entry = None
        self.register_bkp = None

    def go_to_prev(self):
        if self.active_entry is None:
            if self.history:
                self.active_entry = len(self.history) - 1
                self.register_bkp = self.register
                self.register = self.history[self.active_entry]
        elif self.active_entry > 0:
            self.active_entry -= 1
            self.register = self.history[self.active_entry]

    def go_to_next(self):
        if self.active_entry is None:
            return
        elif self.active_entry < len(self.history) - 2:
            self.active_entry += 1
            self.register = self.history[self.active_entry]
        elif self.active_entry == len(self.history) - 1:
            self.active_entry = None
            self.register = self.register_bkp
            self.register_bkp = None

    def open(self):
        self.register = ""
        self.active_entry = None
        self.register_bkp = None

    def close(self):
        self.register = None
        self.active_entry = None
        self.register_bkp = None
        self.error_msg = None

    def is_open(self):
        return self.register is not None

    def is_in_err(self):
        return self.error_msg is not None

    def input(self, character: str):
        if not self.is_open():
            return
        self.register += character

    def backspace(self):
        if not self.is_open():
            return

        if self.register:
            self.register = self.register[0:-1]
        else:
            self.close()

    def run(self):
        if not self.is_open():
            return

        cmd_parts = self.register.split(" ")
        if cmd_parts[0] in self.command_map:
            try:
                hook = self.command_map[cmd_parts[0]]
                params = list(signature(hook).parameters)
                args, kwargs = parse_kwargs(cmd_parts[1:])

                non_kw_params = params.copy()

                # Attempt to cast arguments that look like integers
                for i, arg in enumerate(args):
                    try:
                        args[i] = int(arg)
                    except ValueError:
                        pass

                for k, kwarg in kwargs.items():
                    if k not in params:
                        self.set_error_msg(f"Unknown argument: '{k}'")
                        return
                    non_kw_params.remove(k)

                    try:
                        kwargs[k] = int(kwarg)
                    except ValueError:
                        pass


                if len(args) > len(non_kw_params):
                    self.set_error_msg(f"Too many arguments")
                    return

                req_params = params.copy()

                if hook.__kwdefaults__ is not None:
                    for k in hook.__kwdefaults__:
                        if k in req_params:
                            req_params.remove(k)

                if len(args) < len(req_params):
                    missing_args = req_params[len(args):]
                    self.set_error_msg(f"Missing: {', '.join(missing_args)}")
                else:
                    hook(*args, **kwargs)
                    # add to history only after the command is successful
                    self.history.append(self.register)
            except Exception as exception:
                raise exception
        else:
            self.set_error_msg(f"Command not found: '{cmd_parts[0]}'")

    def get_register(self):
        return self.register


class HistoriedOpusManager(OpusManager):
    def __init__(self):
        super().__init__()
        self.history_ledger = []
        self.locked = False
        self.appending_multiple = False

    def apply_undo(self):
        if not self.history_ledger:
            return

        self.locked = True
        if isinstance(self.history_ledger[-1], list):
            for func, args, kwargs in self.history_ledger.pop():
                func(*args,**kwargs)
        else:
            func, args, kwargs = self.history_ledger.pop()
            func(*args,**kwargs)
        self.locked = False

    def append_undoer(self, func, *args, **kwargs):
        if self.locked:
            return

        if self.appending_multiple:
            self.history_ledger[-1].append((func, args, kwargs))
        else:
            self.history_ledger.append([
                (func, args, kwargs),
                (self.set_cursor_position, [self.cursor_position.copy()], {})
            ])

    def open_multi(self):
        self.history_ledger.append([])
        self.appending_multiple = True

    def close_multi(self):
        self.history_ledger[-1].append((self.set_cursor_position, [self.cursor_position.copy()], {}))
        self.appending_multiple = False

    def setup_repopulate(self, start_position):
        '''Traverse a grouping and setup the history to recreate it for remove functions'''

        already_in_multi = self.appending_multiple
        if not already_in_multi:
            self.open_multi()

        stack = [start_position]
        while stack:
            position = stack.pop(0)
            grouping = self.get_grouping(position)
            if grouping.is_structural():
                self.append_undoer(self.split_grouping, position, len(grouping))
                for k in range(len(grouping)):
                    next_position = position.copy()
                    next_position.append(k)
                    stack.append(next_position)
            elif grouping.is_event():
                event = list(grouping.get_events())[0]
                self.append_undoer(self.set_event, event.note, position, relative=event.relative)
            else:
                self.append_undoer(self.unset, position)

        if not already_in_multi:
            self.close_multi()

    def swap_channels(self, channel_a, channel_b):
        self.append_undoer(self.swap_channels, channel_a, channel_b)
        super().swap_channels(channel_a, channel_b)

    def new_line(self, channel=0, index=None):
        self.append_undoer(self.remove_line, channel, index)
        super().new_line(channel)

    def remove_line(self, channel, index=None):
        y = self.get_y(channel, index)
        self.open_multi()
        self.append_undoer(self.new_line, channel, index)
        for i in range(self.opus_beat_count):
            self.setup_repopulate([y, i])
        self.close_multi()

        super().remove_line(channel, index)

    def insert_after(self, position):
        rposition = position.copy()
        rposition[-1] += 1
        self.append_undoer(self.remove, rposition)

        super().insert_after(position)

    def remove(self, position):
        iposition = position.copy()
        iposition[-1] -= 1

        self.open_multi()
        self.setup_repopulate(position[0:2])
        self.close_multi()

        super().remove(position)

    def insert_beat(self, index):
        self.append_undoer(self.remove_beat, index)
        super().insert_beat(index)


    def remove_beat(self, index):
        self.open_multi()
        self.append_undoer(self.insert_beat, index)
        y = 0
        for i in self.channel_order:
            for j in range(len(self.channel_groupings[i])):
                self.setup_repopulate([y, index])
                y += 1
        self.close_multi()
        super().remove_beat(index)

    def set_event(self, value, position, *, relative=False):
        grouping = self.get_grouping(position)
        if not grouping.is_event():
            self.append_undoer(
                self.unset,
                position
            )
        else:
            original_event = list(grouping.get_events())[0]
            self.append_undoer(
                self.set_event,
                original_event.note,
                position,
                relative=original_event.relative
            )

        super().set_event(value, position, relative=relative)


    def unset(self, position):
        grouping = self.get_grouping(position)
        if grouping.is_event():
            original_event = list(grouping.get_events())[0]
            self.append_undoer(
                self.set_event,
                original_event.note,
                position,
                relative=original_event.relative
            )
        super().unset(position)



class CachedOpusManager(HistoriedOpusManager):
    def __init__(self):
        super().__init__()
        self.updates_cache = UpdatesCache()
        self.interactor = Interactor()
        self.interactor.set_context(InputContext.Default)
        self.interactor.assign_context_sequence(
            InputContext.ConfirmOnly,
            b"\r",
            self.command_ledger_close
        )

        self.interactor.assign_context_batch(
            InputContext.Default,
            (b'l', self.cursor_right),
            (b'h', self.cursor_left),
            (b'j', self.cursor_down),
            (b'k', self.cursor_up),
            (b"x", self.remove_grouping_at_cursor),
            (b'.', self.unset_at_cursor),
            (b'i', self.insert_after_cursor),
            (b'I', self.insert_beat_at_cursor),
            (b'X', self.remove_beat_at_cursor),
            (b'/', self.split_grouping_at_cursor),
            (b';]', self.new_line_at_cursor),
            (b';[', self.remove_line_at_cursor),
            (b'+', self.relative_add_entry),
            (b'-', self.relative_subtract_entry),
            (b'v', self.relative_downshift_entry),
            (b'^', self.relative_upshift_entry),
            (b'K', self.increment_event_at_cursor),
            (b'J', self.decrement_event_at_cursor),
            (b'u', self.apply_undo),
            (b"\x1B", self.clear_register),
            (b":", self.command_ledger_open)
        )

        for c in b"0123456789ab":
            self.interactor.assign_context_sequence(
                InputContext.Default,
                bytes([c]),
                self.add_digit_to_register,
                int(chr(c), 12)
            )

        for c in range(32, 127):
            self.interactor.assign_context_sequence(
                InputContext.Text,
                [c],
                self.command_ledger.input,
                chr(c)
            )

        self.interactor.assign_context_batch(
            InputContext.Text,
            (b"\x7F", self.command_ledger_backspace),
            (b"\x1B", self.command_ledger_close),
            (b"\r", self.command_ledger_run),
            (b"\x1B[A", self.command_ledger.go_to_prev), # Arrow Up
            (b"\x1B[B", self.command_ledger.go_to_next)  # Arrow Down
        )

    def command_ledger_open(self):
        self.command_ledger.open()
        self.interactor.set_context(InputContext.Text)

    def command_ledger_close(self):
        self.command_ledger.close()
        self.interactor.set_context(InputContext.Default)

    def command_ledger_run(self):
        self.command_ledger.run()
        if self.command_ledger.is_in_err():
            self.interactor.set_context(InputContext.ConfirmOnly)
        else:
            self.command_ledger_close()

    def command_ledger_backspace(self):
        self.command_ledger.backspace()
        if not self.command_ledger.is_open():
            self.interactor.set_context(InputContext.Default)

    def daemon_input(self):
        while not self.flag_kill:
            self.interactor.get_input()
        self.interactor.restore_input_settings()

    def insert_after(self, position: List[int]):
        super().insert_after(position)
        channel, line = self.get_channel_index(position[0])
        self.flag('beat_change', (channel, line, position[1]))

    def split_grouping(self, position, splits):
        super().split_grouping(position, splits)
        channel, line = self.get_channel_index(position[0])
        self.flag('beat_change', (channel, line, position[1]))

    def swap_channels(self, channel_a, channel_b):
        super().swap_channels(channel_a, channel_b)

        len_a = len(self.channel_groupings[channel_a])
        len_b = len(self.channel_groupings[channel_b])
        for i in range(len_b):
            self.flag('line', (channel_a, len_b - 1 - i, 'pop'))

        for i in range(len_a):
            self.flag('line', (channel_b, len_a - 1 - i, 'pop'))

        for i in range(len_a):
            self.flag('line', (channel_a, i, 'new'))

        for i in range(len_b):
            self.flag('line', (channel_b, i, 'new'))

    def new(self):
        super().new()

        # Flag changes to cache
        for i in range(self.opus_beat_count):
            self.flag('beat', (i, 'new'))

        for i, channel in enumerate(self.channel_groupings):
            for j, _line in enumerate(channel):
                self.flag('line', (i, j, 'init'))

    def load(self, path: str) -> None:
        super().load(path)

        # Flag changes to cache
        for i in range(self.opus_beat_count):
            self.flag('beat', (i, 'new'))

        for i, channel in enumerate(self.channel_groupings):
            for j, _line in enumerate(channel):
                self.flag('line', (i, j, 'init'))

    def set_event(self, value, position, *, relative=False):
        super().set_event(value, position, relative=relative)
        channel, index = self.get_channel_index(position[0])
        self.flag('beat_change', (channel, index, position[1]))

    def unset(self, position):
        super().unset(position)
        # Flag changes to cache
        channel, index = self.get_channel_index(position[0])
        self.flag('beat_change', (channel, index, position[1]))

    def insert_beat(self, index=None):
        if index is None:
            index = self.opus_beat_count - 1
        super().insert_beat(index)
        # Flag changes to cache
        self.flag('beat', (index, 'new'))

    def new_line(self, channel=0, index=None):
        super().new_line(channel, index)
        # Flag changes to cache
        if index is None:
            line_index = len(self.channel_groupings[channel]) - 1
        else:
            line_index = index

        self.flag('line', (channel, line_index, 'new'))

    def remove(self, position: List[int]):
        super().remove(position)
        # Flag changes to cache
        if len(position) > 2:
            channel, index = self.get_channel_index(position[0])
            self.flag('beat_change', (channel, index, position[1]))

    def remove_beat(self, index=None):
        if index is None:
            index = self.opus_beat_count - 1
        super().remove_beat(index)
        # Flag changes to cache
        self.flag('beat', (index, 'pop'))

    def remove_line(self, channel, index=None):
        super().remove_line(channel, index)

        if index is None:
            index = len(self.channel_groupings[channel])
        else:
            index = index

        # Flag changes to cache
        self.flag('line', (channel, index, 'pop'))

    def increment_event_at_cursor(self):
        super().increment_event_at_cursor()
        position = self.cursor_position
        channel, line = self.get_channel_index(position[0])
        beat = position[1]
        self.flag('beat_change', (channel, line, beat))

    def decrement_event_at_cursor(self):
        super().decrement_event_at_cursor()
        position = self.cursor_position
        channel, line = self.get_channel_index(position[0])
        beat = position[1]
        self.flag('beat_change', (channel, line, beat))

    def kill(self):
        super().kill()
        self.interactor.kill()

    def flag(self, key, value):
        self.updates_cache.flag(key, value)

    def fetch(self, key, noclobber=False):
        return self.updates_cache.fetch(key, noclobber)

class UpdatesCache:
    def __init__(self):
        self.cache = {}

    def flag(self, key, value):
        if key not in self.cache:
            self.cache[key] = []
        self.cache[key].append(value)

    def unflag(self, key, value):
        if key not in self.cache:
            return
        self.cache[key].remove(value)

    def fetch(self, key, noclobber=False):
        output = self.cache.get(key, [])
        if not noclobber:
            self.cache[key] = []
        return output

def parse_kwargs(args):
    o_kwargs = {}
    o_args = []
    skip_flag = False
    for i, arg in enumerate(args):
        if skip_flag:
            skip_flag = False
            continue

        if arg.startswith('--'):
            o_kwargs[arg[2:]] = args[i + 1]
            skip_flag = True
        else:
            o_args.append(arg)
    return (o_args, o_kwargs)


def split_by_channel(event, other_events):
    return event['channel']

def split_by_note_order(event, other_events):
    e_notes = [e.note for e in other_events]
    e_notes.sort()
    return e_notes.index(event.note)
