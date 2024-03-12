package com.qfs.apres.soundfontplayer

class ReverbBuffer(var sample_rate: Int, var reverb: ReverbDynamics) {
    private val reverb_cache = HashMap<Int, MutableSet<Pair<Int, Float>>>()
    private val frame_delay = this.sample_rate * this.reverb.delay

    fun add_reverb_point(value: Float, start_frame: Int) {
        val next_frame = (start_frame.toFloat() + this.frame_delay).toInt()
        if (!this.reverb_cache.contains(next_frame)) {
            this.reverb_cache[next_frame] = mutableSetOf()
        }
        this.reverb_cache[next_frame]!!.add(
            Pair(
                this.reverb.bounces,
                value * reverb.factor
            )
        )
    }

    fun get_frame(frame: Int): Float {
        val next_frame = (frame.toFloat() + this.frame_delay).toInt()
        if (!this.reverb_cache.contains(next_frame)) {
            this.reverb_cache[next_frame] = mutableSetOf()
        }

        val values = this.reverb_cache.remove(frame) ?: setOf()

        var output = 0f
        for ((bounces_remaining, value) in values) {
            output += value
            if (bounces_remaining == 0) {
                continue
            }

            this.reverb_cache[next_frame]!!.add(
                Pair(
                    bounces_remaining - 1,
                    value * this.reverb.factor
                )
            )
        }

        if (this.reverb_cache[next_frame]!!.isEmpty()) {
            this.reverb_cache.remove(next_frame)
        }

        return output
    }
}