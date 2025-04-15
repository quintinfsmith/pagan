package com.qfs.apres.soundfont

enum class SampleType(val i: Int) {
    Mono(0x0001),
    Right(0x0002),
    Left(0x0004),
    Linked(0x0008),
    RomMono(0x8001),
    RomRight(0x8002),
    RomLeft(0x8004),
    RomLinked(0x8008)
}
