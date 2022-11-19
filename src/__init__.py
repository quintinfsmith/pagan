#!/usr/bin/env python3
# coding=utf-8
import sys
from .display import EditorEnvironment
from .control import Controller
from .opusmanager import OpusManager
from .commandinterface import CommandInterface

__version__ = "0.0.4"
__license__ = "GPL-2.0"
__author__ = "Quintin Smith"
__email__ = "smith.quintin@protonmail.com"
__url__ = "https://burnsomni.net/git/radixulous"

def main():
    if len(sys.argv) > 1:
        opusmanager = OpusManager.load(sys.argv[1])
    else:
        opusmanager = OpusManager.new()

    command_interface = CommandInterface(opusmanager)
    control = Controller(opusmanager, command_interface)
    control.listen()

    display = EditorEnvironment(opusmanager, command_interface)
    display.run()

    control.kill()


if __name__ == "__main__":
    main()
