package com.qfs.apres.soundfont

data class Sample(
    var name: String,
    var loopStart: Int,
    var loopEnd: Int,
    var sampleRate: Int,
    var originalPitch: Int,
    var pitchCorrection: Int,
    var linked_sample: Sample?,
    var sampleType: SampleType, // TODO: Use SfSampleType
    var data_placeholder: Pair<Int, Int>,
    var data: ShortArray? = null
)