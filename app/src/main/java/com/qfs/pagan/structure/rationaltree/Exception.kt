package com.qfs.pagan.structure.rationaltree

class InvalidGetCall : Exception("Can't call get() on leaf")
class UntrackedInParentException: Exception("Parent assigned to tree has no record of the child")