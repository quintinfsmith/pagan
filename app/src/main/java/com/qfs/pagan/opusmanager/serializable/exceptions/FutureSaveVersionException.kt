package com.qfs.pagan.opusmanager.serializable.exceptions

class FutureSaveVersionException(version: Int): Exception("Attempting to load a project made with a newer version of Pagan (format: $version)")
