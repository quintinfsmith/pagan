package com.qfs.pagan.opusmanager
import kotlinx.serialization.Serializable

interface OpusEvent {}

open class OpusControlEvent(var transition: Transition): OpusEvent { }

enum class Transition {
    Instantaneous,
    Linear,
    Exponential
}

class VolumeEvent(
    val new_value: Int,
    transition: Transition = Transition.Instantaneous
): OpusControlEvent(transition)

class ReverbEvent(
    val new_value: Float,
    transition: Transition = Transition.Instantaneous
): OpusControlEvent(transition)


@Serializable
data class OpusEventSTD(
    var note: Int,
    var channel: Int,
    var relative: Boolean,
    var duration: Int = 1
): OpusEvent

