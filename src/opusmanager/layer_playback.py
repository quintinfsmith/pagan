import os
import time
import threading
from typing import Optional
from apres import NoteOn, NoteOff
from .layer_interaction import InteractionLayer

class PlaybackLayer(InteractionLayer):
    def __init__(self):
        super().__init__()

    def get_absolute_note(self, position) -> Optional[int]:
        grouping = self.get_grouping(position)
        output = None
        if grouping.is_event():
            output = grouping.get_absolute_note()

        return output

    def set_event(self, value, position, *, relative=False):
        super().set_event(value, position, relative=relative)
        if relative:
            return
        channel, _, _ = self.cursor.get_triplet()
        abs_value = self.get_absolute_note(position)
        if abs_value is not None:
            self.play(abs_value, channel)

    def _play(self, value, channel):
        note_on = NoteOn(note=value, channel=channel)
        note_off = NoteOff(note=value, channel=channel)
        with open("/dev/snd/midiC1D0", "wb") as fp:
            fp.write(bytes(note_on))
        time.sleep(.25)
        with open("/dev/snd/midiC1D0", "wb") as fp:
            fp.write(bytes(note_off))

    def play(self, value, channel):
        thread = threading.Thread(target=self._play, args=[value, channel])
        thread.start()
