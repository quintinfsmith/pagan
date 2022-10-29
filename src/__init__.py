#!/usr/bin/env python3
# coding=utf-8
import sys
from .display import EditorEnvironment
from .control import Controller
from .opusmanager import OpusManager

__version__ = "0.0.3"
__license__ = "GPL-2.0"
__author__ = "Quintin Smith"
__email__ = "smith.quintin@protonmail.com"
__url__ = "https://burnsomni.net/git/radixulous"

def main():
    if len(sys.argv) > 1:
        opusmanager = OpusManager.load(sys.argv[1])
    else:
        opusmanager = OpusManager.new()

    control = Controller(opusmanager)
    control.listen()

    display = EditorEnvironment(opusmanager)
    display.run()

    control.kill()


if __name__ == "__main__":
    main()
