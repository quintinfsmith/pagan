package com.qfs.pagan.structure.opusmanager.base.effectcontrol.event

import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType

class DelayEvent(var numerator: Int, var denominator: Int, var echo: Int, var fade: Float, duration: Int = 1, transition: EffectTransition = EffectTransition.Instant): EffectEvent(duration, transition) {
    override val event_type: EffectType = EffectType.Delay
    override fun to_float_array(): FloatArray {
        return floatArrayOf(
            (this.denominator / this.numerator).toFloat(), // convert frequency to wave length
            this.echo.toFloat(),
            this.fade
        )
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other) && other is DelayEvent && other.numerator == this.numerator && other.denominator == this.denominator && other.echo == this.echo && this.fade == other.fade
    }

    override fun copy(): DelayEvent {
        return DelayEvent(
            this.numerator,
            this.denominator,
            this.echo,
            this.fade,
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