'''Allow subclasses to listen for keyboard input'''
import termios
import threading
import time
import tty
import sys
import select
import os

class ContextChange(Exception):
    '''Thrown when context is changed on interactor mid-read'''


class FunctionTreeNode(object):
    '''tree-like node to determine input-sequence'''
    def __init__(self):
        self.children = {}
        self.action = None
        self.args = None

    def get_spread(self):
        output = 0
        if self.action:
            output += 1
        else:
            for child in self.children.values():
                output += child.get_spread()

        return output

    def unset(self, path):
        if path:
            node = path[0]
            path = path[1:]
            if node in self.children.keys():
                self.children[node].unset(path)
                if self.children[node].get_spread() <= 1:
                    del self.children[node]
        else:
            self.action = None
            self.args = None

    def set(self, path, action, *args):
        '''Assign a function to an end-node in node-tree'''
        if path:
            node = path[0]
            path = path[1:]
            if not node in self.children.keys():
                self.children[node] = FunctionTreeNode()
            self.children[node].set(path, action, *args)
        else:
            self.action = action
            self.args = args


    def get(self, path):
        '''Return sequence at current node in sequence, if any'''
        if path:
            node = path[0]
            path = path[1:]
            if node in self.children.keys():
                return self.children[node].get(path)
            else:
                return None
        else:
            return self

class Interactor(object):
    '''Allows for interaction with keyboard input'''
    def __init__(self):
        self.active_context = 0
        self.cmd_nodes = { self.active_context: FunctionTreeNode() }

        self.active_node = self.cmd_nodes[self.active_context]

        self.backup = []
        self.checking_cmds = False

        self.ignoring_input = 0
        self.downtime = 1 / 60
        self.kill_flag = False

        self._init_fileno = None
        self._init_attr = None

    def read_character(self):
        '''Read character from stdin'''
        self._init_fileno = sys.stdin.fileno() # store original pipe n
        self._init_attr = termios.tcgetattr(self._init_fileno)  # store original input settings
        try:
            tty.setraw(sys.stdin.fileno()) # remove wait for "return"
            ch = None
            in_context = self.active_context
            while not self.kill_flag and ch is None:
                try:
                    ready, _, __ = select.select([sys.stdin], [], [], 0)
                except TypeError:
                    ready = []
                except ValueError:
                    ready = []

                if sys.stdin in ready:
                    try:
                        output = os.read(self._init_fileno, 1)
                        if output:
                            ch = chr(output[0])
                        else:
                            continue
                    except ValueError:
                        continue

                if self.active_context != in_context:
                    raise ContextChange()

                if ch is None:
                    time.sleep(.01)
        finally:
            self.restore_input_settings()

        return ch

    def restore_input_settings(self):
        if self._init_fileno is not None:
            termios.tcsetattr(self._init_fileno, termios.TCSADRAIN, self._init_attr) # reset input settings
        self._init_fileno = None
        self._init_attr = None


    def get_input(self):
        '''Send keypress to be handled'''
        try:
            new_chr = self.read_character()
            if time.time() - self.ignoring_input < self.downtime:
                pass
            else:
                self.check_cmd(new_chr)
        except ContextChange as e:
            self.active_node = self.cmd_nodes[self.active_context]


    def check_cmd(self, char):
        '''Add key-press to key-sequence, call function if any'''
        node = self.active_node.get(char)
        top = self.active_node == self.cmd_nodes[self.active_context]

        if node:
            if node.action:
                self.ignoring_input = time.time()
                if node.args:
                    node.action(*(node.args))
                else:
                    node.action()
                self.active_node = self.cmd_nodes[self.active_context]
            else:
                self.active_node = node
        else:
            # If an invalid path is given, then interactor
            # will check if the last given character is in
            # a path of its own and either call that function
            # or just get the input sequence started
            self.active_node = self.cmd_nodes[self.active_context]
            if not top:
                self.check_cmd(char)

    def assign_sequence(self, string_seq, func, *args):
        '''associate key-sequence with a function'''
        self.assign_context_sequence(
            self.active_context,
            string_seq,
            func,
            *args
        )

    def assign_context_sequence(self, context_key, string_seq, func, *args):
        if context_key not in self.cmd_nodes.keys():
            self.cmd_nodes[context_key] = FunctionTreeNode()
        self.cmd_nodes[context_key].set(string_seq, func, *args)

    def set_context(self, context_key):
        self.active_context = context_key
