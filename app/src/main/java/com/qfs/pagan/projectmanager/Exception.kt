package com.qfs.pagan.projectmanager
import android.net.Uri

class InvalidDirectoryException(path: Uri): Exception("Real Directory Required ($path)")
class MKDirFailedException(dir: String): Exception("Failed to create directory $dir")
class PathNotSetException(): Exception("Projects path has not been set.")
class NewFileFailException(): Exception("Failed To create new file")
