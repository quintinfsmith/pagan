package com.qfs.pagan

import android.Manifest
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.test.rule.GrantPermissionRule
import com.qfs.pagan.ComponentActivity.ComponentActivityEditor
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import com.qfs.pagan.structure.opusmanager.base.OpusLinePercussion
import com.qfs.pagan.structure.opusmanager.base.PercussionEvent
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.EffectController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.HighPassEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.LowPassEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs


class ComposeTest {
    @get:Rule
    val editor_test_rule = createAndroidComposeRule<ComponentActivityEditor>()

    @JvmField
    @Rule
    var mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule
        .grant(Manifest.permission.POST_NOTIFICATIONS)

    private fun get_manager(): OpusLayerInterface {
        return editor_test_rule.activity.controller_model.opus_manager
    }

    private fun check_undo_redo(callback: () -> Unit) {
        val opus_manager = this.get_manager()

        val original = OpusLayerBase()
        original.import_from_other(opus_manager)

        val original_size = opus_manager.history_cache.size()

        callback()

        val altered = OpusLayerBase()
        altered.import_from_other(opus_manager)
        val altered_size = opus_manager.history_cache.size()
        assertNotEquals(original_size, altered_size)

        click_elm(TestTag.Undo)

        assertEquals(original, opus_manager)
        assertEquals(original_size, opus_manager.history_cache.size())

        click_elm(TestTag.Redo)

        assertEquals(altered_size, opus_manager.history_cache.size())
        assertEquals(altered, opus_manager)
    }

    private fun input_text(text: String, tag: TestTag, vararg args: Any?) {
        val interaction = get_interaction(tag, *args)
        interaction.performTextInput(text)
    }
    private fun submit_text(text: String, tag: TestTag, vararg args: Any?) {
        val interaction = get_interaction(tag, *args)
        interaction.performTextInput(text)
        interaction.performKeyInput { this.pressKey(Key.Enter) }
    }
    private fun click_elm(tag: TestTag, vararg args: Any?) {
        val interaction = get_interaction(tag, *args)
        this.editor_test_rule.waitUntil(10000) {
            interaction.isDisplayed()
        }
        interaction.performClick()
    }

    private fun long_click_elm(tag: TestTag, vararg args: Any?) {
        get_interaction(tag, *args).performTouchInput {
            longClick(Offset(this.width / 2F,this.height / 2F))
        }
    }

    private fun get_interaction(tag: TestTag, vararg args: Any?): SemanticsNodeInteraction {
        when (tag) {
            TestTag.Leaf -> get_interaction(TestTag.MainRow).performScrollToIndex(args[3] as Int)
            TestTag.BeatLabel -> get_interaction(TestTag.MainRow).performScrollToIndex(args[0] as Int)
            else -> {}
        }
        return editor_test_rule.onNodeWithTag(test_tag_to_string(tag, *args), true)
    }

    @Test
    fun test_new_project() {
        click_elm(TestTag.TopBarKebab)
        editor_test_rule.onNodeWithText("New Project").performClick()
    }

    @Test
    fun test_build_song() {
        val opus_manager = this.get_manager()
        click_elm(TestTag.OuterInsertBeat)
        get_interaction(TestTag.DialogNumberInput).performTextInput("200")
        click_elm(TestTag.DialogPositive)
        assertEquals(204, opus_manager.length)

        // Build Drums
        this.check_undo_redo {
            click_elm(TestTag.LineLabel, 1, 0, null)
            click_elm(TestTag.LineNew)
        }
        this.check_undo_redo {
            click_elm(TestTag.LineNew)
        }
        assertEquals(3, opus_manager.get_channel(1).lines.size)

        this.check_undo_redo {
            click_elm(TestTag.InstrumentSet)
            click_elm(TestTag.MenuItem, 8)
        }
        assertEquals(
            8,
            (opus_manager.get_channel(1).lines[2] as OpusLinePercussion).instrument
        )

        this.check_undo_redo {
            click_elm(TestTag.LineLabel, 1, 1, null)
            click_elm(TestTag.InstrumentSet)
            click_elm(TestTag.MenuItem, 13)
        }
        assertEquals(
            13,
            (opus_manager.get_channel(1).lines[1] as OpusLinePercussion).instrument
        )

        this.check_undo_redo {
            click_elm(TestTag.LineLabel, 1, 0, null)
            click_elm(TestTag.InstrumentSet)
            click_elm(TestTag.MenuItem, 15)
        }
        assertEquals(
            15,
            (opus_manager.get_channel(1).lines[0] as OpusLinePercussion).instrument
        )

        this.check_undo_redo {
            click_elm(TestTag.Leaf, null, 1, 0, 0)
            click_elm(TestTag.LeafSplit)
        }
        assertEquals(2, opus_manager.get_tree(BeatKey(1, 0, 0)).size)

        click_elm(TestTag.Leaf, null, 1, 0, 0, 1)
        click_elm(TestTag.LeafSplit)
        assertEquals(2, opus_manager.get_tree(BeatKey(1, 0, 0), listOf(1)).size)

        this.check_undo_redo {
            click_elm(TestTag.PercussionToggle)
        }
        assertNotEquals(
            null,
            opus_manager.get_tree(BeatKey(1, 0, 0), listOf(1, 0)).event
        )

        click_elm(TestTag.Leaf, null, 1, 0, 0, 1, 1)
        click_elm(TestTag.PercussionToggle)
        assertNotEquals(
            null,
            opus_manager.get_tree(BeatKey(1, 0, 0), listOf(1, 1)).event
        )

        this.check_undo_redo {
            long_click_elm(TestTag.Leaf, null, 1, 0, 0, 1, 1)
            click_elm(TestTag.LineLabel, 1, 0, null)
            get_interaction(TestTag.DialogNumberInput).performTextInput("28")
            click_elm(TestTag.DialogPositive)
        }
        for (i in 0 until 28) {
            assertEquals(
                opus_manager.get_tree(BeatKey(1, 0, 0)),
                opus_manager.get_tree(BeatKey(1, 0, i))
            )
        }

        this.check_undo_redo {
            click_elm(TestTag.Leaf, null, 1, 2, 0)
            click_elm(TestTag.PercussionToggle)
        }
        this.check_undo_redo {
            click_elm(TestTag.Leaf, null, 1, 1, 1)
            click_elm(TestTag.PercussionToggle)
        }
        this.check_undo_redo {
            long_click_elm(TestTag.Leaf, null, 1, 1, 1)
            long_click_elm(TestTag.Leaf, null, 1, 2, 0)
            click_elm(TestTag.LineLabel, 1, 1, null)
            get_interaction(TestTag.DialogNumberInput).performTextInput("14")
            click_elm(TestTag.DialogPositive)
        }
        for (i in 1 until 14) {
            assertEquals(
                opus_manager.get_tree(BeatKey(1, 1, 0)).event,
                opus_manager.get_tree(BeatKey(1, 1, (i * 2))).event
            )
            assertEquals(
                opus_manager.get_tree(BeatKey(1, 1, 1)).event,
                opus_manager.get_tree(BeatKey(1, 1, 1 + (i * 2))).event
            )
            assertEquals(
                opus_manager.get_tree(BeatKey(1, 2, 0)).event,
                opus_manager.get_tree(BeatKey(1, 2, i * 2)).event
            )
            assertEquals(
                opus_manager.get_tree(BeatKey(1, 2, 1)).event,
                opus_manager.get_tree(BeatKey(1, 2, 1 + (i * 2))).event
            )
        }

        for (i in 0 until 13) {
            click_elm(TestTag.Undo)
        }

        assertEquals(204, opus_manager.length)
        click_elm(TestTag.Undo)

        assertEquals(4, opus_manager.length)
        assertEquals(4, opus_manager.channels[1].get_beat_count())
        assertEquals(1, opus_manager.channels[1].lines.size)

        for (i in 0 until 3) {
            click_elm(TestTag.Redo)
        }
        assertEquals(204, opus_manager.length)
        assertEquals(204, opus_manager.channels[0].lines[0].beats.size)
    }

    @Test
    fun test_set_event() {
        var manager = this.get_manager()
        click_elm(TestTag.Leaf, null, 0, 0, 0)
        click_elm(TestTag.EventOffset, 4)
        click_elm(TestTag.EventOctave, 2)

        manager.get_tree(BeatKey(0, 0, 0)).event.let { event ->
            assert(event is AbsoluteNoteEvent)
            assertEquals((2 * 12) + 4, (event as AbsoluteNoteEvent).note)
        }

        click_elm(TestTag.EventDuration)
        submit_text("3", TestTag.DialogNumberInput)
        manager.get_tree(BeatKey(0, 0, 0)).event.let { event ->
            assertEquals(3, event!!.duration)
        }

        click_elm(TestTag.Leaf, null, 0, 0, 1)
        click_elm(TestTag.EventOffset, 4)
        manager.get_tree(BeatKey(0, 0, 1)).event.let { event ->
            assert(event == null)
        }

        long_click_elm(TestTag.EventDuration)
        manager.get_tree(BeatKey(0, 0, 0)).event.let { event ->
            assertEquals(1, event!!.duration)
        }


        click_elm(TestTag.Leaf, null, 0, 0, 1)
        long_click_elm(TestTag.EventOffset, 3)
        click_elm(TestTag.RelativeSetPositive)
        manager.get_tree(BeatKey(0, 0, 1)).event.let { event ->
            assert(event is RelativeNoteEvent)
            assertEquals(3, (event as RelativeNoteEvent).offset)
        }

        long_click_elm(TestTag.EventOctave, 1)
        click_elm(TestTag.RelativeSetNegative)
        manager.get_tree(BeatKey(0, 0, 1)).event.let { event ->
            assert(event is RelativeNoteEvent)
            assertEquals(-15, (event as RelativeNoteEvent).offset)
        }

        long_click_elm(TestTag.EventOctave, 0)
        click_elm(TestTag.RelativeSetAbsolute)
        manager.get_tree(BeatKey(0, 0, 1)).event.let { event ->
            assert(event is AbsoluteNoteEvent)
            assertEquals(4, (event as AbsoluteNoteEvent).note)
        }

        click_elm(TestTag.Leaf, null, 1, 0, 0)
        click_elm(TestTag.PercussionToggle)
        manager.get_tree(BeatKey(1, 0, 0)).event.let { event ->
            assert(event is PercussionEvent)
        }

        click_elm(TestTag.PercussionToggle)
        manager.get_tree(BeatKey(1, 0, 0)).event.let { event ->
            assertFalse(event is PercussionEvent)
        }
    }

   // @Test
   // fun test_duration() {
   //     var manager = this.get_manager()
   //     click_elm(TestTag.Leaf, null, 0, 0, 0)
   //     click_elm(TestTag.EventOffset,)
   //         ...
   // }

    @Test
    fun test_undo_global_effect_removal() {
        click_elm(TestTag.LineLabel, null, null, EffectType.Tempo)
        click_elm(TestTag.LineEffectRemove)
        click_elm(TestTag.Undo)
        click_elm(TestTag.LineLabel, null, null, EffectType.Tempo)
    }

    private fun open_line_effect_widget(effect_type: EffectType, channel: Int = 0, line_offset: Int = 0) {
        click_elm(TestTag.LineLabel, channel, line_offset, null)
        click_elm(TestTag.LineEffectsShow)
        click_elm(EffectResourceMap[effect_type].test_tag)
    }

    private fun open_channel_effect_widget(effect_type: EffectType, channel: Int = 0) {
        click_elm(TestTag.LineLabel, channel, 0, null)
        click_elm(TestTag.LineLabel, channel, 0, null)
        click_elm(TestTag.ChannelEffectsShow)
        click_elm(EffectResourceMap[effect_type].test_tag)
    }

    private fun open_global_effect_widget(effect_type: EffectType) {
        click_elm(TestTag.EffectsToggleGlobal)
        click_elm(EffectResourceMap[effect_type].test_tag)
    }


    private fun <T: EffectEvent> test_widgets(effect_type: EffectType, callback: (EffectController<T>) -> Unit) {
        val opus_manager = this.get_manager()
        if (OpusLayerInterface.line_controller_domain.contains(effect_type)) {
            this.open_line_effect_widget(effect_type, 0, 0)
            callback(opus_manager.get_line_controller<T>(effect_type, 0, 0))
        }
        if (OpusLayerInterface.channel_controller_domain.contains(effect_type)) {
            this.open_channel_effect_widget(effect_type, 0)
            callback(opus_manager.get_channel_controller<T>(effect_type, 0))
        }
        if (OpusLayerInterface.global_controller_domain.contains(effect_type)) {
            this.open_global_effect_widget(effect_type)
            callback(opus_manager.get_global_controller<T>(effect_type))
        }
    }

    @Test
    fun test_widget_volume() {
        this.test_widgets<OpusVolumeEvent>(EffectType.Volume) { controller ->
            val count = 8
            val acceptable_deviation = 1F / (count * 2).toFloat()
            for (i in 1 until count) {
                val position = i.toFloat() / count.toFloat()
                val expected_value = position * 1.27F
                // Test Slider //////////////////////////////////////////////////////////////////
                get_interaction(TestTag.VolumeSlider).performTouchInput {
                    down(Offset(position * this.width,this.height / 2F))
                    up()
                }

                assert(acceptable_deviation > abs(controller.initial_event.value - expected_value)) {
                    "Failure on Volume: ($i) Excepted: $expected_value | Actual ${controller.initial_event.value}"
                }
                ////////////////////////////////////////////////////////////////////////////////

                // Test Button Input ///////////////////////////////////////////////////////////
                click_elm(TestTag.VolumeButton)
                input_text((expected_value * 100).toInt().toString(), TestTag.DialogNumberInput)
                click_elm(TestTag.DialogPositive)
                assertEquals(
                    (expected_value * 100).toInt() / 100F,
                    controller.initial_event.value
                )
            }
            ////////////////////////////////////////////////////////////////////////////////
        }
    }

    @Test
    fun test_widget_pan() {
        this.test_widgets<OpusPanEvent>(EffectType.Pan) { controller ->
            val actual = mutableListOf<Float>()
            val expected = mutableListOf<Float>()
            for (i in 1 until 20) {
                val r_position = (i.toFloat() / 21F) + (1F / 30F)
                val test_value = (i - 10).toFloat() / -10F

                get_interaction(TestTag.PanSlider).performTouchInput {
                    down(Offset(r_position * this.width, this.height / 2F))
                    up()
                }

                expected.add(test_value)
                actual.add(controller.initial_event.value)
            }
            assertEquals(expected, actual)
        }
    }

    @Test
    fun test_widget_highpass() {
        this.test_widgets<HighPassEvent>(EffectType.HighPass) { controller ->
            val test_value = 5005.01F
            submit_text(test_value.toString(), TestTag.FilterInput)
            assertEquals(
                test_value,
                controller.initial_event.filter_cutoff
            )
            click_elm(TestTag.EventUnset)
            assertEquals(
                0F,
                controller.initial_event.filter_cutoff
            )
        }
    }

    @Test
    fun test_widget_lowpass() {
        this.test_widgets<LowPassEvent>(EffectType.LowPass) { controller ->
            val test_value = 5005.01F
            submit_text(test_value.toString(), TestTag.FilterInput)
            assertEquals(
                test_value,
                controller.initial_event.filter_cutoff
            )
            click_elm(TestTag.EventUnset)
            assertEquals(
                20000F,
                controller.initial_event.filter_cutoff
            )
        }
    }

    @Test
    fun test_widget_delay() {
        this.test_widgets<DelayEvent>(EffectType.Delay) { controller ->
            submit_text("40", TestTag.DelayHzNumerator)
            assertEquals(40, controller.initial_event.numerator)

            submit_text("3", TestTag.DelayHzDenominator)
            assertEquals(3, controller.initial_event.denominator)

            submit_text("12", TestTag.DelayEcho)
            assertEquals(12, controller.initial_event.echo)

            click_elm(TestTag.DelayFadeButton)
            get_interaction(TestTag.DelayFadeSlider).performTouchInput {
                down(Offset(this.width * .3F,this.height * .3F))
                up()
            }

            assertNotEquals(.5F, controller.initial_event.fade)
        }
    }

    @Test
    fun test_widget_velocity() {
        this.test_widgets<OpusVelocityEvent>(EffectType.Velocity) { controller ->
            click_elm(TestTag.VelocitySlideToggle)
            val new_slide = 8
            submit_text(new_slide.toString(), TestTag.VelocitySlideDenominator)
            assertEquals(
                Pair(OpusVelocityEvent.SlideMaxWidth.Note, new_slide),
                controller.initial_event.slide
            )

            click_elm(TestTag.VelocitySlideDialogButton)
            editor_test_rule.onNodeWithText(editor_test_rule.activity.getString(R.string.velocity_widget_slide_beat), true).performClick()
            assertEquals(
                Pair(OpusVelocityEvent.SlideMaxWidth.Beat, new_slide),
                controller.initial_event.slide
            )

            click_elm(TestTag.VelocityButton)
            get_interaction(TestTag.VelocityVSlider).performTouchInput {
                down(Offset(this.width * .3F,this.height * .3F))
                up()
            }

            assertNotEquals(1F, controller.initial_event.value)

            // TODO: disable slide
        }
    }


    @Test
    fun reg_test_180() {
        val opus_manager = this.get_manager()

        //Enable an effect
        click_elm(TestTag.LineLabel, 0,0, null)
        click_elm(TestTag.LineEffectsShow)
        click_elm(TestTag.EffectMenuVolume)

        click_elm(TestTag.Leaf, EffectType.Volume, 0, 0, 0)
        click_elm(TestTag.LeafSplit)
        click_elm(TestTag.VolumeButton)
        input_text("50", TestTag.DialogNumberInput)
        click_elm(TestTag.DialogPositive)

        long_click_elm(TestTag.EventUnset)

        assert(
            opus_manager.get_line_ctl_tree_copy<OpusVolumeEvent>(
                EffectType.Volume,
                BeatKey(0,0,0),
                listOf()
            ).is_leaf()
        )

        // click twice since short click and long click have the same effect otherwise
        click_elm(TestTag.LeafSplit)
        click_elm(TestTag.LeafSplit)
        long_click_elm(TestTag.LeafRemove)
        assert(
            opus_manager.get_line_ctl_tree_copy<OpusVolumeEvent>(
                EffectType.Volume,
                BeatKey(0,0,0),
                listOf()
            ).is_leaf()
        )
    }

    @Test
    fun reg_test_185() {
        // Mute The last channel
        click_elm(TestTag.LineLabel, 1,0, null)
        click_elm(TestTag.LineLabel, 1,0, null)
        click_elm(TestTag.ChannelMute)

        click_elm(TestTag.EffectsToggleGlobal)
        click_elm(TestTag.EffectMenuDelay)

        val state_model = editor_test_rule.activity.state_model
        for (line in state_model.line_data) {
            if (line.channel.value != null) continue
            assert(!line.is_mute.value)
        }
    }

    @Test
    fun reg_test_186() {
        // Open Channel Menu & Remove it
        click_elm(TestTag.LineLabel, 0,0, null)
        click_elm(TestTag.LineLabel, 0,0, null)
        click_elm(TestTag.ChannelRemove)

        // Again, Open Channel Menu & Remove it
        click_elm(TestTag.LineLabel, 0,0, null)
        click_elm(TestTag.LineLabel, 0,0, null)
        click_elm(TestTag.ChannelRemove)

        val opus_manager = editor_test_rule.activity.controller_model.opus_manager
        assert(opus_manager.channels.isNotEmpty())
    }

    @Test
    fun reg_test_188() {
        click_elm(TestTag.EffectsToggleGlobal)
        click_elm(TestTag.EffectMenuDelay)
        click_elm(TestTag.Leaf, EffectType.Delay, null, null, 0)
        this.editor_test_rule.waitUntil {
            get_interaction(TestTag.DelayFadeButton).isDisplayed()
        }
    }

    @Test
    fun reg_test_189() {
        click_elm(TestTag.LineLabel, 0, 0, null)
        click_elm(TestTag.LineEffectsShow)
        click_elm(TestTag.EffectMenuDelay)
        click_elm(TestTag.LineLabel, 0, 0, null)
        click_elm(TestTag.LineLabel, 0, 0, null)

        click_elm(TestTag.ChannelEffectsShow)
        this.editor_test_rule.waitUntil {
            get_interaction(TestTag.EffectMenuDelay).isDisplayed()
        }
    }
}
