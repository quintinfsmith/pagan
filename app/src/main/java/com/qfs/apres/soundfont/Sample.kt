package com.qfs.apres.soundfont

data class Sample(val ptr: Long) {
    constructor(
        name: String,
        loopStart: Int,
        loopEnd: Int,
        sampleRate: Int,
        originalPitch: Int,
        pitchCorrection: Int,
        sampleType: Int,
        data_placeholder: Pair<Int, Int>,
    ): this(
        create(
            name,
            loopStart,
            loopEnd,
            sampleRate,
            originalPitch,
            pitchCorrection,
            sampleType,
            data_placeholder.first,
            data_placeholder.second
        )
    )
    init {
        println("SAMPLE CREATED ${this.name}")
    }

    companion object {
        external fun create(
            name: String,
            loopStart: Int,
            loopEnd: Int,
            sampleRate: Int,
            originalPitch: Int,
            pitchCorrection: Int,
            sampleType: Int,
            data_placeholder_start: Int,
            data_placeholder_end: Int
        ): Long
    }

    val name: String
        get() = this.get_name_inner(this.ptr).toString()

    val data_placeholder: Pair<Int, Int>
        get() = this._get_placeholder()

    val sample_type: Int
        get() = this.get_sample_type_inner(this.ptr)

    var data: ShortArray
        get() = this.get_data_inner(this.ptr)
        set(value) = this.set_data_inner(this.ptr, value)

    val sample_rate: Int
        get() = this.get_sample_rate_inner(this.ptr)

    val original_pitch: Int
        get() = this.get_original_pitch(this.ptr)

    val pitch_correction: Int
        get() = this.get_pitch_correction(this.ptr)

    val loop_start: Int
        get() = this.get_loop_start(this.ptr)

    val loop_end: Int
        get() = this.get_loop_end(this.ptr)

    external fun get_original_pitch(ptr: Long): Int
    external fun get_sample_rate_inner(ptr: Long): Int
    external fun get_sample_type_inner(ptr: Long): Int
    external fun set_data_inner(ptr: Long, data: ShortArray)
    external fun jni_data_placeholders(ptr: Long): IntArray
    external fun get_name_inner(ptr: Long): String
    external fun get_data_inner(ptr: Long): ShortArray
    external fun get_pitch_correction(ptr: Long): Int
    external fun get_loop_start(ptr: Long): Int
    external fun get_loop_end(ptr: Long): Int

    fun set_data(data: ShortArray) {
        this.set_data_inner(this.ptr, data)
    }

    private fun _get_placeholder(): Pair<Int, Int> {
        val pair_as_array = this.jni_data_placeholders(this.ptr)
        return Pair(
            pair_as_array[0],
            pair_as_array[1]
        )
        //return Pair(
        //    this.get_placeholder_start(this.ptr),
        //    this.get_placeholder_end(this.ptr)
        //)
    }

    private fun get_data_placholders(ptr: Long) {

    }

}