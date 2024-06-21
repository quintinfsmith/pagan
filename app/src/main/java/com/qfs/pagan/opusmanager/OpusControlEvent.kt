package com.qfs.pagan.opusmanager

abstract class OpusControlEvent()

enum class ControlEventType {
    Tempo,
    Volume,
    Reverb,
}

class OpusTempoEvent(var value: Float): OpusControlEvent()
class OpusVolumeEvent(var value: Int, var transition: Int = 0): OpusControlEvent()
class OpusReverbEvent(var value: Float): OpusControlEvent()

