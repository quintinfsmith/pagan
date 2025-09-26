package com.qfs.pagan

import com.qfs.apres.soundfontplayer.SampleHandleManager
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test


class PlaybackFrameMapUnitTests {
    val sample_handle_manager = mockk<SampleHandleManager>()
    val opus_manager = OpusLayerBase()
    @Before
    fun setup() {
        this.opus_manager._project_change_new()
        val channel_volume_controller = this.opus_manager.get_channel_controller<OpusVolumeEvent>(EffectType.Volume, 0)
        val tempo_controller = this.opus_manager.get_global_controller<OpusTempoEvent>(EffectType.Tempo)

        tempo_controller.set_initial_event(OpusTempoEvent(110F))

        every { this@PlaybackFrameMapUnitTests.sample_handle_manager.sample_rate } returns 44100
    }

    @Test
    fun test() {
        val frame_map = PlaybackFrameMap(this.opus_manager, this.sample_handle_manager)
        frame_map.parse_opus()
        //assertEquals(
        //)
    }
}