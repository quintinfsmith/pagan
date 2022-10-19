#!/usr/bin/env python3
# coding=utf-8
import sys
from .editorenvironment import EditorEnvironment

__version__ = "0.0.1"
__license__ = "GPL-2.0"
__author__ = "Quintin Smith"
__email__ = "smith.quintin@protonmail.com"
__url__ = "https://burnsomni.net/git/radixulous"

def main():
    aa = EditorEnvironment()
    if len(sys.argv) > 1:
        aa.load(sys.argv[1])
    else:
        aa.new()
    aa.run()


if __name__ == "__main__":
    main()
