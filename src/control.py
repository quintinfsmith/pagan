from __future__ import annotations
import os
import re
import json
import time
import threading

from inspect import signature
from typing import Optional, Dict, List, Tuple
from enum import Enum, auto

from .interactor import Interactor
from .event_register import EventRegister

class InputContext(Enum):
    DEFAULT = auto()
    TEXT = auto()
    CONFIRMONLY = auto()

class Controller:
    def kill(self):
        self.kill_flag = True
        self.interactor.kill()

    def _listen(self):
        while not self.kill_flag:
            self.interactor.get_input()
        self.interactor.restore_input_settings()

    def listen(self):
        thread = threading.Thread(target=self._listen)
        thread.start()

    def __init__(self, opus_manager, command_interface):
        self.opus_manager = opus_manager
        self.command_interface = command_interface
        self.event_register = EventRegister()

        self.kill_flag = False
        self.interactor = Interactor()
        self.interactor.set_context(InputContext.DEFAULT)
        self.interactor.assign_context_sequence(
            InputContext.CONFIRMONLY,
            b"\r",
            self.command_ledger_close
        )

        self.interactor.assign_context_batch(
            InputContext.DEFAULT,
            (b'l', opus_manager.cursor_right),
            (b'h', opus_manager.cursor_left),
            (b'j', opus_manager.cursor_down),
            (b'k', opus_manager.cursor_up),
            (b"\x1B[A", opus_manager.cursor_up),
            (b"\x1B[B", opus_manager.cursor_down),
            (b"\x1B[C", opus_manager.cursor_right),
            (b"\x1B[D", opus_manager.cursor_left),
            (b"x", opus_manager.remove_tree_at_cursor),
            (b'.', opus_manager.unset_at_cursor),
            (b'i', opus_manager.insert_after_cursor),
            (b'I', opus_manager.insert_beat_at_cursor),
            (b'X', opus_manager.remove_beat_at_cursor),
            (b'G', self.command_ledger_open_and_set, 'jump '),
            (b'/', opus_manager.split_tree_at_cursor),
            (b' ', opus_manager.set_percussion_event_at_cursor),
            (b';]', opus_manager.new_line_at_cursor),
            (b';[', opus_manager.remove_line_at_cursor),
            (b'K', opus_manager.increment_event_at_cursor),
            (b'J', opus_manager.decrement_event_at_cursor),
            (b'u', opus_manager.apply_undo),
            (b'+', self.event_register.relative_add_entry),
            (b'-', self.event_register.relative_subtract_entry),
            (b'v', self.event_register.relative_downshift_entry),
            (b'^', self.event_register.relative_upshift_entry),
            (b"\x1B", self.event_register.clear),
            (b":", self.command_ledger_open)
        )

        for c in b"0123456789ab":
            self.interactor.assign_context_sequence(
                InputContext.DEFAULT,
                bytes([c]),
                self.add_digit_to_register,
                int(chr(c), 12)
            )

        for c in range(32, 127):
            self.interactor.assign_context_sequence(
                InputContext.TEXT,
                [c],
                command_interface.command_ledger_input,
                chr(c)
            )

        self.interactor.assign_context_batch(
            InputContext.TEXT,
            (b"\x7F", self.command_ledger_backspace),
            (b"\x1B", self.command_ledger_close),
            (b"\r", self.command_ledger_run),
            (b"\x1B[A", command_interface.command_ledger_prev), # Arrow Up
            (b"\x1B[B", command_interface.command_ledger_next)  # Arrow Down
        )

    def add_digit_to_register(self, value: int):
        self.event_register.add_digit(value)
        # is_ready() gets checked in the function
        self.apply_register_at_cursor()

    def apply_register_at_cursor(self) -> None:
        """
            Convert the temporary event to a real one and
            set the tree pointed to by the cursor.
        """

        event_register = self.event_register
        opus_manager = self.opus_manager

        if not event_register.is_ready():
            return

        register = event_register.fetch()

        channel, line_offset, _beat_offset = opus_manager.cursor.get_triplet()
        if channel == 9:
            opus_manager.set_percussion_instrument(line_offset, register.note)
        else:
            opus_manager.set_event(
                opus_manager.cursor.get_triplet(),
                opus_manager.cursor.position,
                register
            )

    def command_ledger_open(self):
        self.command_interface.command_ledger_open()
        self.interactor.set_context(InputContext.TEXT)

    def command_ledger_close(self):
        self.command_interface.command_ledger_close()
        self.interactor.set_context(InputContext.DEFAULT)

    def command_ledger_run(self):
        self.command_interface.command_ledger_run()
        if self.command_interface.command_ledger.is_in_err():
            self.interactor.set_context(InputContext.CONFIRMONLY)
        else:
            self.interactor.set_context(InputContext.DEFAULT)

    def command_ledger_backspace(self):
        self.command_interface.command_ledger_backspace()
        if not self.command_interface.command_ledger.is_open():
            self.interactor.set_context(InputContext.DEFAULT)

    def command_ledger_set(self, new_value: str) -> None:
        self.command_interface.command_ledger_set(new_value)

    def command_ledger_open_and_set(self, new_value: str) -> None:
        self.command_ledger_open()
        self.command_ledger_set(new_value)


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
