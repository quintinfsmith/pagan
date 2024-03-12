package com.qfs.apres.soundfontplayer

class ReverbBuffer(var sample_rate: Int, var reverb: ReverbDynamics) {
    private val reverb_delay = this.reverb.get_travel_duration()
    private val reverb_cache = FloatArray((this.sample_rate.toFloat() * this.reverb.decay).toInt()) { 0f }
}