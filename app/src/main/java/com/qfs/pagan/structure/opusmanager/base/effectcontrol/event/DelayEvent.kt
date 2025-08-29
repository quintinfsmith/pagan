package com.qfs.pagan.structure.opusmanager.base.effectcontrol.event

import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType

class DelayEvent(var frequency: Rational, var repeat: Int, var repeat_decay: Float, duration: Int = 1, transition: EffectTransition = EffectTransition.Instant): EffectEvent(duration, transition) {
    override val event_type: EffectType = EffectType.Delay
    override fun to_float_array(): FloatArray {
        // DEBUG
        return floatArrayOf(1F / 1F, 3F, .6F)


        //return floatArrayOf(
        //    (this.frequency.denominator / this.frequency.numerator).toFloat(), // convert frequency to wave length
        //    this.repeat.toFloat(),
        //    this.repeat_decay
        //)
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other) && other is DelayEvent && other.frequency == this.frequency && other.repeat == this.repeat && this.repeat_decay == other.repeat_decay
    }

    override fun copy(): DelayEvent {
        return DelayEvent(
            this.frequency,
            this.repeat,
            this.repeat_decay,
            this.duration,
            this.transition
        )
    }

    override fun get_event_instant(
        position: Rational,
        preceding_event: EffectEvent
    ): DelayEvent {
        val copy_event = this.copy()
        // Not implementing transitions for delay. doesn't seem useful. could be wrong but i'm not worrying about it right now.
        //when (this.transition) {
        //    EffectTransition.Linear -> {
        //        val frequency_diff = this.frequency - (preceding_event as DelayEvent).frequency
        //        val repeat_diff = this.repeat - preceding_event.repeat
        //        val repeat_decay_diff = this.repeat_decay - preceding_event.repeat_decay
        //        copy_event.frequency = preceding_event.frequency + (frequency_diff * position)
        //        copy_event.repeat = preceding_event.repeat + (repeat_diff * position)
        //        copy_event.repeat_decay = preceding_event.repeat_decay + (repeat_decay_diff * position)
        //    }
        //    EffectTransition.RLinear -> {
        //        val frequency_diff = (preceding_event as DelayEvent).frequency - this.frequency
        //        val repeat_diff = preceding_event.repeat - this.repeat
        //        val repeat_decay_diff = preceding_event.repeat_decay - this.repeat_decay
        //        val diff = (preceding_event as SingleFloatEvent).value - this.value
        //        copy_event.value = this.value + (diff * position.toFloat())
        //    }
        //    EffectTransition.Instant -> {}
        //    EffectTransition.RInstant -> {}
        //}

        return copy_event
    }
}