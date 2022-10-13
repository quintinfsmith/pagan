from __future__ import annotations

from typing import Optional, Dict, List, Tuple
from wrecked import Rect

class RectFrame:
    def __init__(self, parent_rect: Rect, border: List[chr] = None):
        self.frame = parent_rect.new_rect()
        self.wrapper = self.frame.new_rect()
        self.content = self.wrapper.new_rect()
        self.rendered_size = None
        self.parent = parent_rect
        self.view_offset = (0, 0)
        if border is None:
            self.border = [
                chr(9581),
                chr(9582),
                chr(9583),
                chr(9584),
                chr(9472),
                chr(9474)
            ]
        else:
            self.border = border

    def resize(self, width: int, height: int):
        '''Wrap the resize function for the wrapper'''
        if self.border:
            self.frame.resize(width + 2, height + 2)
            self.wrapper.move(1, 1)
        else:
            self.frame.resize(width, height)
            self.wrapper.move(0, 0)

        self.wrapper.resize(width, height)
        self.draw_border()

    @property
    def full_height(self) -> int:
        return self.frame.height

    @property
    def full_width(self) -> int:
        return self.frame.width

    @property
    def height(self) -> int:
        return self.wrapper.height
    @property
    def width(self) -> int:
        return self.wrapper.width

    @property
    def size(self) -> Tuple[int, int]:
        return (self.full_width, self.full_height)

    def detach(self) -> None:
        self.frame.detach()

    def attach(self) -> None:
        self.parent.attach(self.frame)

    def move(self, x: int, y: int) -> None:
        self.frame.move(x, y)

    def move_inner(self, x: int, y: int) -> None:
        self.view_offset = (x, y)
        self.content.move(x, y)

    def get_content_rect(self) -> None:
        return self.content

    def get_view_offset(self) -> Tuple[int, int]:
        return self.view_offset

    def draw_border(self) -> None:
        if not self.border:
            return

        width = self.frame.width
        height = self.frame.height
        for y in range(height):
            self.frame.set_string(0, y, self.border[5])
            self.frame.set_string(width - 1, y, self.border[5])

        for x in range(width):
            self.frame.set_string(x, 0, self.border[4])
            self.frame.set_string(x, height - 1, self.border[4])

        self.frame.set_string(0, 0, chr(9581))
        self.frame.set_string(width - 1, 0, chr(9582))
        self.frame.set_string(width - 1, height - 1, chr(9583))
        self.frame.set_string(0, height - 1, chr(9584))

