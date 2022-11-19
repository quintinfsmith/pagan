"""Cursor Layer of the OpusManager and the classes required to make it work"""
from __future__ import annotations
from typing import List, Optional, Tuple, Union, Any
from collections.abc import Callable
from .opusmanager.miditree import MIDITree, MIDITreeEvent

class EventRegister:
    RADIX = 12
    def __init__(self):
        self.register: Optional[MIDITreeEvent] = None

    def add_digit(self, value: int) -> None:
        """Pass a digit to the register"""

        if self.register is None:
            self.register = MIDITreeEvent(
                value,
                base=self.RADIX,
                relative=False
            )
        elif self.register.relative:
            self.register.note *= value
        else:
            self.register.note *= self.RADIX
            self.register.note += value

    def is_ready(self) -> bool:
        # If the register is ready, apply it
        return self.register is not None \
            and (
                self.register.relative \
                or self.register.note >= self.RADIX
            )

    def clear(self) -> None:
        """Cancel event being input."""
        self.register = None

    def fetch(self) -> Optional[MIDITreeEvent]:
        """Get the register, unsetting it"""
        output = self.register
        self.register = None
        return output

    def relative_add_entry(self) -> None:
        """Let the register know the user is inputting a relative event, moving up some keys"""
        self.register = MIDITreeEvent(1, base=self.RADIX, relative=True)

    def relative_subtract_entry(self) -> None:
        """Let the register know the user is inputting a relative event, moving down some keys"""
        self.register = MIDITreeEvent(-1, base=self.RADIX, relative=True)

    def relative_downshift_entry(self) -> None:
        """Let the register know the user is inputting a relative event, shifting down octaves"""
        self.register = MIDITreeEvent(-1 * self.RADIX, base=self.RADIX, relative=True)

    def relative_upshift_entry(self) -> None:
        """Let the register know the user is inputting a relative event, shifting up octaves"""
        self.register = MIDITreeEvent(self.RADIX, base=self.RADIX, relative=True)
