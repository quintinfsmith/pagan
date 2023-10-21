package com.qfs.apres

import com.qfs.apres.event.ActiveSense
import com.qfs.apres.event.AllControllersOff
import com.qfs.apres.event.AllNotesOff
import com.qfs.apres.event.AllSoundOff
import com.qfs.apres.event.Balance
import com.qfs.apres.event.BalanceLSB
import com.qfs.apres.event.BalanceMSB
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.BankSelectLSB
import com.qfs.apres.event.BankSelectMSB
import com.qfs.apres.event.BreathController
import com.qfs.apres.event.BreathControllerLSB
import com.qfs.apres.event.BreathControllerMSB
import com.qfs.apres.event.CelesteLevel
import com.qfs.apres.event.ChannelPrefix
import com.qfs.apres.event.ChannelPressure
import com.qfs.apres.event.ChorusLevel
import com.qfs.apres.event.CopyRightNotice
import com.qfs.apres.event.CuePoint
import com.qfs.apres.event.DataDecrement
import com.qfs.apres.event.DataEntry
import com.qfs.apres.event.DataEntryLSB
import com.qfs.apres.event.DataEntryMSB
import com.qfs.apres.event.DataIncrement
import com.qfs.apres.event.EffectControl1
import com.qfs.apres.event.EffectControl1LSB
import com.qfs.apres.event.EffectControl1MSB
import com.qfs.apres.event.EffectControl2
import com.qfs.apres.event.EffectControl2LSB
import com.qfs.apres.event.EffectControl2MSB
import com.qfs.apres.event.EffectsLevel
import com.qfs.apres.event.EndOfTrack
import com.qfs.apres.event.Expression
import com.qfs.apres.event.ExpressionLSB
import com.qfs.apres.event.ExpressionMSB
import com.qfs.apres.event.FootPedal
import com.qfs.apres.event.FootPedalLSB
import com.qfs.apres.event.FootPedalMSB
import com.qfs.apres.event.GeneralPurpose1
import com.qfs.apres.event.GeneralPurpose1LSB
import com.qfs.apres.event.GeneralPurpose1MSB
import com.qfs.apres.event.GeneralPurpose2
import com.qfs.apres.event.GeneralPurpose2LSB
import com.qfs.apres.event.GeneralPurpose2MSB
import com.qfs.apres.event.GeneralPurpose3
import com.qfs.apres.event.GeneralPurpose3LSB
import com.qfs.apres.event.GeneralPurpose3MSB
import com.qfs.apres.event.GeneralPurpose4
import com.qfs.apres.event.GeneralPurpose4LSB
import com.qfs.apres.event.GeneralPurpose4MSB
import com.qfs.apres.event.GeneralPurpose5
import com.qfs.apres.event.GeneralPurpose6
import com.qfs.apres.event.GeneralPurpose7
import com.qfs.apres.event.GeneralPurpose8
import com.qfs.apres.event.Hold2Pedal
import com.qfs.apres.event.HoldPedal
import com.qfs.apres.event.InstrumentName
import com.qfs.apres.event.KeySignature
import com.qfs.apres.event.Legato
import com.qfs.apres.event.LocalControl
import com.qfs.apres.event.Lyric
import com.qfs.apres.event.MIDIClock
import com.qfs.apres.event.MIDIContinue
import com.qfs.apres.event.MIDIEvent
import com.qfs.apres.event.MIDIStart
import com.qfs.apres.event.MIDIStop
import com.qfs.apres.event.MTCQuarterFrame
import com.qfs.apres.event.Marker
import com.qfs.apres.event.ModulationWheel
import com.qfs.apres.event.ModulationWheelLSB
import com.qfs.apres.event.ModulationWheelMSB
import com.qfs.apres.event.MonophonicOperation
import com.qfs.apres.event.NonRegisteredParameterNumber
import com.qfs.apres.event.NonRegisteredParameterNumberLSB
import com.qfs.apres.event.NonRegisteredParameterNumberMSB
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.OmniOff
import com.qfs.apres.event.OmniOn
import com.qfs.apres.event.Pan
import com.qfs.apres.event.PanLSB
import com.qfs.apres.event.PanMSB
import com.qfs.apres.event.PhaserLevel
import com.qfs.apres.event.PitchWheelChange
import com.qfs.apres.event.PolyphonicKeyPressure
import com.qfs.apres.event.PolyphonicOperation
import com.qfs.apres.event.Portamento
import com.qfs.apres.event.PortamentoTime
import com.qfs.apres.event.PortamentoTimeLSB
import com.qfs.apres.event.PortamentoTimeMSB
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.RegisteredParameterNumber
import com.qfs.apres.event.RegisteredParameterNumberLSB
import com.qfs.apres.event.RegisteredParameterNumberMSB
import com.qfs.apres.event.Reset
import com.qfs.apres.event.SMPTEOffset
import com.qfs.apres.event.SequenceNumber
import com.qfs.apres.event.SequencerSpecific
import com.qfs.apres.event.SetTempo
import com.qfs.apres.event.SoftPedal
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.event.SongSelect
import com.qfs.apres.event.SoundAttack
import com.qfs.apres.event.SoundBrightness
import com.qfs.apres.event.SoundControl1
import com.qfs.apres.event.SoundControl2
import com.qfs.apres.event.SoundControl3
import com.qfs.apres.event.SoundControl4
import com.qfs.apres.event.SoundControl5
import com.qfs.apres.event.SoundReleaseTime
import com.qfs.apres.event.SoundTimbre
import com.qfs.apres.event.SoundVariation
import com.qfs.apres.event.Sustenuto
import com.qfs.apres.event.SystemExclusive
import com.qfs.apres.event.Text
import com.qfs.apres.event.TimeCode
import com.qfs.apres.event.TimeSignature
import com.qfs.apres.event.TrackName
import com.qfs.apres.event.TremuloLevel
import com.qfs.apres.event.TuneRequest
import com.qfs.apres.event.Volume
import com.qfs.apres.event.VolumeLSB
import com.qfs.apres.event.VolumeMSB

abstract class VirtualMidiInputDevice {
    private var midi_controller: MidiController? = null
    fun set_midi_controller(midi_controller: MidiController) {
        this.midi_controller = midi_controller
    }
    fun unset_midi_controller() {
        this.midi_controller = null
    }
    fun is_connected(): Boolean {
        return this.midi_controller != null
    }
    fun send_event(event: MIDIEvent) {
        // TODO: Throw error?
        if (is_connected()) {
            this.midi_controller!!.broadcast_event(event)
        }
    }
}
