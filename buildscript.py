#!/usr/bin/env python3
# coding=utf-8

import os, sys

file_path = os.path.realpath(__file__)
os.chdir(file_path[0:file_path.rfind("/") + 1])


os.system("cp src radixal -rf")
if "--local" in sys.argv:
    os.system("python3 setup.py install --prefix ~/.local/")
elif "--publish" in sys.argv:
    os.system("python3 setup.py sdist bdist_wheel")
    os.system("python3 -m twine upload dist/*")

os.system("rm radixal.egg-info -rf")
os.system("rm build -rf")
os.system("rm dist -rf")
os.system("rm radixal -rf")
