package com.qfs.pagan.opusmanager
import kotlinx.serialization.Serializable

interface OpusEvent {}

enum class Transition {
    Instantaneous,
    Linear,
    Exponential
}
enum class ControlEventType {
    Tempo,
    Volume,
    Reverb,
}

@Serializable
data class OpusControlEvent(
    var value: Float,
    var transition: Transition = Transition.Instantaneous,
    var duration: Int = 1
): OpusEvent

@Serializable
data class OpusEventSTD(
    var note: Int,
    var channel: Int,
    var relative: Boolean,
    var duration: Int = 1
): OpusEvent

