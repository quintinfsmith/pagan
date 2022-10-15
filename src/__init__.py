#!/usr/bin/env python3
# coding=utf-8
import sys
from .editorenvironment import EditorEnvironment

__version__ = "0.0.1"
__license__ = "GPL-2.0"
__author__ = "Quintin Smith"
__email__ = "smith.quintin@protonmail.com"
__url__ = "https://burnsomni.net/git/radixal"

def main():
    aa = EditorEnvironment()
    aa.opus_manager.load(sys.argv[1])
    aa.run()


if __name__ == "__main__":
    main()
