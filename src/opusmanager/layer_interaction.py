from __future__ import annotations
import os
import re
import json

from inspect import signature
from typing import Optional, Dict, List, Tuple
from enum import Enum, auto

from .layer_cursor import CursorLayer
from .interactor import Interactor

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


    def run(self, **static_kwargs):
        if not self.is_open():
            return

        cmd_parts = self.register.split(" ")
        while "" in cmd_parts:
            cmd_parts.remove("")

        # Add to history even if it's bad, so the user can see what they did wrong
        self.history.append(self.register)
        if not cmd_parts:
            pass
        elif cmd_parts[0] in self.command_map:
            try:
                hook = self.command_map[cmd_parts[0]]
                params = list(signature(hook).parameters)
                try:
                    args, kwargs = parse_kwargs(cmd_parts[1:])
                except Exception:
                    self.set_error_msg(f"Bad String")
                    return

                # Apply preset static values to keywords
                for k, static_value in static_kwargs.items():
                    if k in kwargs:
                        continue
                    kwargs[k] = static_value

                non_kw_params = params.copy()
                if 'kwargs' in non_kw_params:
                    non_kw_params.remove('kwargs')

                for i, arg in enumerate(args):
                    # Convert Ranges
                    if ":" in arg:
                        part_a = arg[0:arg.find(":")]
                        part_b = arg[arg.find(":") + 1:]
                        try:
                            part_a = int(part_a)
                            part_b = int(part_b)
                            args[i] = range(part_a, part_b)
                        except ValueError:
                            pass
                    # Attempt to cast arguments that look like integers
                    try:
                        args[i] = int(arg)
                    except ValueError:
                        pass

                for k, kwarg in kwargs.items():
                    if not isinstance(kwarg, str):
                        continue

                    # Convert Ranges
                    if ":" in kwarg:
                        part_a = kwarg[0:kwarg.find(":")]
                        part_b = kwarg[kwarg.find(":") + 1:]
                        try:
                            part_a = int(part_a)
                            part_b = int(part_b)
                            kwargs[k] = range(part_a, part_b)
                        except ValueError:
                            pass

                    try:
                        kwargs[k] = int(kwarg)
                    except ValueError:
                        pass


                if len(args) > len(non_kw_params):
                    self.set_error_msg(f"Too many arguments")
                    return

                req_params = params.copy()

                if hook.__kwdefaults__ is not None:
                    if 'kwargs' in req_params:
                        req_params.remove('kwargs')
                    for k in hook.__kwdefaults__:
                        if k in req_params:
                            req_params.remove(k)

                arg_defaults = []
                if hook.__defaults__ is not None:
                    arg_defaults = hook.__defaults__

                if len(args) < len(req_params) - len(arg_defaults):
                    missing_args = req_params[len(args):]
                    self.set_error_msg(f"Missing: {', '.join(missing_args)}")
                else:
                    try:
                        hook(*args, **kwargs)
                    except Exception as e:
                        self.set_error_msg(f"{repr(e)}")

            except Exception as exception:
                raise exception
        else:
            self.set_error_msg(f"Command not found: '{cmd_parts[0]}'")

    def get_register(self):
        return self.register

class InteractionLayer(CursorLayer):
    '''Adds Interaction and Commands to the OpusManager'''
    def daemon_input(self):
        while not self.flag_kill:
            self.interactor.get_input()
        self.interactor.restore_input_settings()

    def kill(self):
        super().kill()
        self.interactor.kill()

    def __init__(self):
        super().__init__()

        self.static_kwargs = {}

        self.command_ledger = CommandLedger({
            'w': self.save,
            'q': self.kill,
            'c+': self.add_channel,
            'c-': self.remove_channel,
            'export': self.export,
            'swap': self.swap_channels,
            'jump': self.jump_to_beat,
            'link': self.link_beat_at_cursor,
            'unlink': self.cmd_unlink_beats,
            'ow': self.overwrite_beat_at_cursor,
            'set':  self.save_kwarg_value
        })

        self.interactor = Interactor()
        self.interactor.set_context(InputContext.Default)
        self.interactor.assign_context_sequence(
            InputContext.ConfirmOnly,
            b"\r",
            self.command_ledger_close
        )

        self.interactor.assign_context_batch(
            InputContext.Default,
            (b'l', self.cursor.move_right),
            (b'h', self.cursor.move_left),
            (b'j', self.cursor.move_down),
            (b'k', self.cursor.move_up),
            (b"\x1B[A", self.cursor.move_up),
            (b"\x1B[B", self.cursor.move_down),
            (b"\x1B[C", self.cursor.move_right),
            (b"\x1B[D", self.cursor.move_left),
            (b"x", self.remove_grouping_at_cursor),
            (b'.', self.unset_at_cursor),
            (b'i', self.insert_after_cursor),
            (b'I', self.insert_beat_at_cursor),
            (b'X', self.remove_beat_at_cursor),
            (b'G', self.open_command_ledger_and_set, 'jump '),
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

    def cmd_unlink_beats(self, beats=None):
        if beats is None:
            self.unlink_beat_at_cursor()
        else:
            cursor = self.cursor.get_triplet()
            for i in range(beats):
                self.unlink_beat(cursor[0], cursor[1], cursor[2] + i)

    def save_kwarg_value(self, cmd, key, value):
        if not cmd in self.static_kwargs:
            self.static_kwargs[cmd] = {}
        self.static_kwargs[cmd][key] = value

    def open_command_ledger_and_set(self, new_value):
        self.command_ledger_open()
        self.command_ledger.register = new_value

    def command_ledger_open(self):
        self.command_ledger.open()
        self.interactor.set_context(InputContext.Text)

    def command_ledger_close(self):
        self.command_ledger.close()
        self.interactor.set_context(InputContext.Default)

    def command_ledger_run(self):
        register = self.command_ledger.get_register()
        if ' ' in register:
            cmd = register[0:register.find(" ")]
        else:
            cmd = register

        self.command_ledger.run(**self.static_kwargs.get(cmd, {}))

        if self.command_ledger.is_in_err():
            self.interactor.set_context(InputContext.ConfirmOnly)
        else:
            self.command_ledger_close()

    def command_ledger_backspace(self):
        self.command_ledger.backspace()
        if not self.command_ledger.is_open():
            self.interactor.set_context(InputContext.Default)

    def save(self, path=None):
        super().save(path)

        fullpath = self.get_working_dir()
        json_static_kwargs = {}
        for cmd, kwargs in self.static_kwargs.items():
            json_static_kwargs[cmd] = {}
            for key, value in kwargs.items():
                if isinstance(value, range):
                    value = f"{value.start}:{value.stop}"
                json_static_kwargs[cmd][key] = value

        with open(f"{fullpath}/static_kwargs.json", "w") as fp:
            fp.write(json.dumps(json_static_kwargs, indent=4))
    def load(self, path):
        super().load(path)
        fullpath = self.get_working_dir()
        filepath = f"{fullpath}/static_kwargs.json"
        if not os.path.isfile(filepath):
            return

        json_kwargs = {}
        with open(filepath, "r") as fp:
            self.static_kwargs = json.loads(fp.read())


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

