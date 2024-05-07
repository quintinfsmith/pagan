package com.qfs.pagan.opusmanager
import kotlinx.serialization.Serializable

interface OpusEvent
@Serializable
sealed class OpusControlEvent: OpusEvent

enum class ControlEventType {
    Tempo,
    Volume,
    Reverb,
}

@Serializable
data class OpusTempoEvent(var value: Float): OpusControlEvent()

@Serializable
data class OpusVolumeEvent(var value: Int, var transition: Int = 0): OpusControlEvent()

@Serializable
data class OpusReverbEvent(var value: Float): OpusControlEvent()

@Serializable
data class OpusEventSTD(
    var note: Int,
    var channel: Int,
    var relative: Boolean,
    var duration: Int = 1
): OpusEvent

