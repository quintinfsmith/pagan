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

open class VirtualMidiDevice {
    private var midi_controller: MidiController? = null
    fun setMidiController(controller: MidiController) {
        this.midi_controller = controller
    }

    fun is_registered(): Boolean {
        return this.midi_controller != null
    }

    fun sendEvent(event: MIDIEvent) {
        // TODO: Throw error?
        if (is_registered()) {
            this.midi_controller!!.receiveMessage(event, this)
        }
    }

    fun receiveMessage(event: MIDIEvent) {
        when (event) {
            is SequenceNumber -> this.onSequenceNumber(event)
            is Text -> this.onText(event)
            is CopyRightNotice -> this.onCopyRightNotice(event)
            is TrackName -> this.onTrackName(event)
            is InstrumentName -> this.onInstrumentName(event)
            is Lyric -> this.onLyric(event)
            is Marker -> this.onMarker(event)
            is CuePoint -> this.onCuePoint(event)
            is EndOfTrack -> this.onEndOfTrack(event)
            is ChannelPrefix -> this.onChannelPrefix(event)
            is SetTempo -> this.onSetTempo(event)
            is SMPTEOffset -> this.onSMPTEOffset(event)
            is TimeSignature -> this.onTimeSignature(event)
            is KeySignature -> this.onKeySignature(event)
            is SequencerSpecific -> this.onSequencerSpecific(event)
            is NoteOn -> this.onNoteOn(event)
            is NoteOff -> this.onNoteOff(event)
            is PolyphonicKeyPressure -> this.onPolyphonicKeyPressure(event)
            is HoldPedal -> this.onHoldPedal(event)
            is Portamento -> this.onPortamento(event)
            is Sustenuto -> this.onSustenuto(event)
            is SoftPedal -> this.onSoftPedal(event)
            is Legato -> this.onLegato(event)
            is Hold2Pedal -> this.onHold2Pedal(event)
            is SoundVariation -> this.onSoundVariation(event)
            is SoundTimbre -> this.onSoundTimbre(event)
            is SoundReleaseTime -> this.onSoundReleaseTime(event)
            is SoundAttack -> this.onSoundAttack(event)
            is SoundBrightness -> this.onSoundBrightness(event)
            is SoundControl1 -> this.onSoundControl1(event)
            is SoundControl2 -> this.onSoundControl2(event)
            is SoundControl3 -> this.onSoundControl3(event)
            is SoundControl4 -> this.onSoundControl4(event)
            is SoundControl5 -> this.onSoundControl5(event)
            is EffectsLevel -> this.onEffectsLevel(event)
            is TremuloLevel -> this.onTremuloLevel(event)
            is ChorusLevel -> this.onChorusLevel(event)
            is CelesteLevel -> this.onCelesteLevel(event)
            is PhaserLevel -> this.onPhaserLevel(event)
            is LocalControl -> this.onLocalControl(event)
            is MonophonicOperation -> this.onMonophonicOperation(event)
            is BankSelect -> this.onBankSelect(event)
            is BankSelectLSB -> this.onBankSelectLSB(event)
            is BankSelectMSB -> this.onBankSelectMSB(event)
            is ModulationWheel -> this.onModulationWheel(event)
            is ModulationWheelLSB -> this.onModulationWheelLSB(event)
            is ModulationWheelMSB -> this.onModulationWheelMSB(event)
            is BreathController -> this.onBreathController(event)
            is BreathControllerLSB -> this.onBreathControllerLSB(event)
            is BreathControllerMSB -> this.onBreathControllerMSB(event)
            is FootPedal -> this.onFootPedal(event)
            is FootPedalLSB -> this.onFootPedalLSB(event)
            is FootPedalMSB -> this.onFootPedalMSB(event)
            is PortamentoTime -> this.onPortamentoTime(event)
            is PortamentoTimeLSB -> this.onPortamentoTimeLSB(event)
            is PortamentoTimeMSB -> this.onPortamentoTimeMSB(event)
            is DataEntry -> this.onDataEntry(event)
            is DataEntryLSB -> this.onDataEntryLSB(event)
            is DataEntryMSB -> this.onDataEntryMSB(event)
            is Volume -> this.onVolume(event)
            is VolumeLSB -> this.onVolumeLSB(event)
            is VolumeMSB -> this.onVolumeMSB(event)
            is Balance -> this.onBalance(event)
            is BalanceLSB -> this.onBalanceLSB(event)
            is BalanceMSB -> this.onBalanceMSB(event)
            is Pan -> this.onPan(event)
            is PanLSB -> this.onPanLSB(event)
            is PanMSB -> this.onPanMSB(event)
            is Expression -> this.onExpression(event)
            is ExpressionLSB -> this.onExpressionLSB(event)
            is ExpressionMSB -> this.onExpressionMSB(event)
            is NonRegisteredParameterNumber -> this.onNonRegisteredParameterNumber(event)
            is NonRegisteredParameterNumberLSB -> this.onNonRegisteredParameterNumberLSB(event)
            is NonRegisteredParameterNumberMSB -> this.onNonRegisteredParameterNumberMSB(event)
            is RegisteredParameterNumber -> this.onRegisteredParameterNumber(event)
            is RegisteredParameterNumberLSB -> this.onRegisteredParameterNumberLSB(event)
            is RegisteredParameterNumberMSB -> this.onRegisteredParameterNumberMSB(event)
            is EffectControl1 -> this.onEffectControl1(event)
            is EffectControl1LSB -> this.onEffectControl1LSB(event)
            is EffectControl1MSB -> this.onEffectControl1MSB(event)
            is EffectControl2 -> this.onEffectControl2(event)
            is EffectControl2LSB -> this.onEffectControl2LSB(event)
            is EffectControl2MSB -> this.onEffectControl2MSB(event)
            is GeneralPurpose1 -> this.onGeneralPurpose1(event)
            is GeneralPurpose1LSB -> this.onGeneralPurpose1LSB(event)
            is GeneralPurpose1MSB -> this.onGeneralPurpose1MSB(event)
            is GeneralPurpose2 -> this.onGeneralPurpose2(event)
            is GeneralPurpose2LSB -> this.onGeneralPurpose2LSB(event)
            is GeneralPurpose2MSB -> this.onGeneralPurpose2MSB(event)
            is GeneralPurpose3 -> this.onGeneralPurpose3(event)
            is GeneralPurpose3LSB -> this.onGeneralPurpose3LSB(event)
            is GeneralPurpose3MSB -> this.onGeneralPurpose3MSB(event)
            is GeneralPurpose4 -> this.onGeneralPurpose4(event)
            is GeneralPurpose4LSB -> this.onGeneralPurpose4LSB(event)
            is GeneralPurpose4MSB -> this.onGeneralPurpose4MSB(event)
            is GeneralPurpose5 -> this.onGeneralPurpose5(event)
            is GeneralPurpose6 -> this.onGeneralPurpose6(event)
            is GeneralPurpose7 -> this.onGeneralPurpose7(event)
            is GeneralPurpose8 -> this.onGeneralPurpose8(event)
            is DataIncrement -> this.onDataIncrement(event)
            is DataDecrement -> this.onDataDecrement(event)
            is AllControllersOff -> this.onAllControllersOff(event)
            is AllNotesOff -> this.onAllNotesOff(event)
            is AllSoundOff -> this.onAllSoundOff(event)
            is OmniOff -> this.onOmniOff(event)
            is OmniOn -> this.onOmniOn(event)
            is PolyphonicOperation -> this.onPolyphonicOperation(event)
            is ProgramChange -> this.onProgramChange(event)
            is ChannelPressure -> this.onChannelPressure(event)
            is PitchWheelChange -> this.onPitchWheelChange(event)
            is SystemExclusive -> this.onSystemExclusive(event)
            is MTCQuarterFrame -> this.onMTCQuarterFrame(event)
            is SongPositionPointer -> this.onSongPositionPointer(event)
            is SongSelect -> this.onSongSelect(event)
            is TuneRequest -> this.onTuneRequest(event)
            is MIDIClock -> this.onMIDIClock(event)
            is MIDIStart -> this.onMIDIStart(event)
            is MIDIContinue -> this.onMIDIContinue(event)
            is MIDIStop -> this.onMIDIStop(event)
            is ActiveSense -> this.onActiveSense(event)
            is Reset -> this.onReset(event)
            is TimeCode -> this.onTimeCode(event)
        }
    }

    // abstract functions
    open fun onSequenceNumber(event: SequenceNumber) { }
    open fun onText(event: Text) { }
    open fun onCopyRightNotice(event: CopyRightNotice) { }
    open fun onTrackName(event: TrackName) { }
    open fun onInstrumentName(event: InstrumentName) { }
    open fun onLyric(event: Lyric) { }
    open fun onMarker(event: Marker) { }
    open fun onCuePoint(event: CuePoint) { }
    open fun onEndOfTrack(event: EndOfTrack) { }
    open fun onChannelPrefix(event: ChannelPrefix) { }
    open fun onSetTempo(event: SetTempo) { }
    open fun onSMPTEOffset(event: SMPTEOffset) { }
    open fun onTimeSignature(event: TimeSignature) { }
    open fun onKeySignature(event: KeySignature) { }
    open fun onSequencerSpecific(event: SequencerSpecific) { }
    open fun onNoteOn(event: NoteOn) { }
    open fun onNoteOff(event: NoteOff) { }
    open fun onPolyphonicKeyPressure(event: PolyphonicKeyPressure) { }
    open fun onHoldPedal(event: HoldPedal) { }
    open fun onPortamento(event: Portamento) { }
    open fun onSustenuto(event: Sustenuto) { }
    open fun onSoftPedal(event: SoftPedal) { }
    open fun onLegato(event: Legato) { }
    open fun onHold2Pedal(event: Hold2Pedal) { }
    open fun onSoundVariation(event: SoundVariation) { }
    open fun onSoundTimbre(event: SoundTimbre) { }
    open fun onSoundReleaseTime(event: SoundReleaseTime) { }
    open fun onSoundAttack(event: SoundAttack) { }
    open fun onSoundBrightness(event: SoundBrightness) { }
    open fun onSoundControl1(event: SoundControl1) { }
    open fun onSoundControl2(event: SoundControl2) { }
    open fun onSoundControl3(event: SoundControl3) { }
    open fun onSoundControl4(event: SoundControl4) { }
    open fun onSoundControl5(event: SoundControl5) { }
    open fun onEffectsLevel(event: EffectsLevel) { }
    open fun onTremuloLevel(event: TremuloLevel) { }
    open fun onChorusLevel(event: ChorusLevel) { }
    open fun onCelesteLevel(event: CelesteLevel) { }
    open fun onPhaserLevel(event: PhaserLevel) { }
    open fun onLocalControl(event: LocalControl) { }
    open fun onMonophonicOperation(event: MonophonicOperation) { }
    open fun onBankSelect(event: BankSelect) { }
    open fun onBankSelectLSB(event: BankSelectLSB) { }
    open fun onBankSelectMSB(event: BankSelectMSB) { }
    open fun onModulationWheel(event: ModulationWheel) { }
    open fun onModulationWheelLSB(event: ModulationWheelLSB) { }
    open fun onModulationWheelMSB(event: ModulationWheelMSB) { }
    open fun onBreathController(event: BreathController) { }
    open fun onBreathControllerLSB(event: BreathControllerLSB) { }
    open fun onBreathControllerMSB(event: BreathControllerMSB) { }
    open fun onFootPedal(event: FootPedal) { }
    open fun onFootPedalLSB(event: FootPedalLSB) { }
    open fun onFootPedalMSB(event: FootPedalMSB) { }
    open fun onPortamentoTime(event: PortamentoTime) { }
    open fun onPortamentoTimeLSB(event: PortamentoTimeLSB) { }
    open fun onPortamentoTimeMSB(event: PortamentoTimeMSB) { }
    open fun onDataEntry(event: DataEntry) { }
    open fun onDataEntryLSB(event: DataEntryLSB) { }
    open fun onDataEntryMSB(event: DataEntryMSB) { }
    open fun onVolume(event: Volume) { }
    open fun onVolumeLSB(event: VolumeLSB) { }
    open fun onVolumeMSB(event: VolumeMSB) { }
    open fun onBalance(event: Balance) { }
    open fun onBalanceLSB(event: BalanceLSB) { }
    open fun onBalanceMSB(event: BalanceMSB) { }
    open fun onPan(event: Pan) { }
    open fun onPanLSB(event: PanLSB) { }
    open fun onPanMSB(event: PanMSB) { }
    open fun onExpression(event: Expression) { }
    open fun onExpressionLSB(event: ExpressionLSB) { }
    open fun onExpressionMSB(event: ExpressionMSB) { }
    open fun onNonRegisteredParameterNumber(event: NonRegisteredParameterNumber) { }
    open fun onNonRegisteredParameterNumberLSB(event: NonRegisteredParameterNumberLSB) { }
    open fun onNonRegisteredParameterNumberMSB(event: NonRegisteredParameterNumberMSB) { }
    open fun onRegisteredParameterNumber(event: RegisteredParameterNumber) { }
    open fun onRegisteredParameterNumberLSB(event: RegisteredParameterNumberLSB) { }
    open fun onRegisteredParameterNumberMSB(event: RegisteredParameterNumberMSB) { }
    open fun onEffectControl1(event: EffectControl1) { }
    open fun onEffectControl1LSB(event: EffectControl1LSB) { }
    open fun onEffectControl1MSB(event: EffectControl1MSB) { }
    open fun onEffectControl2(event: EffectControl2) { }
    open fun onEffectControl2LSB(event: EffectControl2LSB) { }
    open fun onEffectControl2MSB(event: EffectControl2MSB) { }
    open fun onGeneralPurpose1(event: GeneralPurpose1) { }
    open fun onGeneralPurpose1LSB(event: GeneralPurpose1LSB) { }
    open fun onGeneralPurpose1MSB(event: GeneralPurpose1MSB) { }
    open fun onGeneralPurpose2(event: GeneralPurpose2) { }
    open fun onGeneralPurpose2LSB(event: GeneralPurpose2LSB) { }
    open fun onGeneralPurpose2MSB(event: GeneralPurpose2MSB) { }
    open fun onGeneralPurpose3(event: GeneralPurpose3) { }
    open fun onGeneralPurpose3LSB(event: GeneralPurpose3LSB) { }
    open fun onGeneralPurpose3MSB(event: GeneralPurpose3MSB) { }
    open fun onGeneralPurpose4(event: GeneralPurpose4) { }
    open fun onGeneralPurpose4LSB(event: GeneralPurpose4LSB) { }
    open fun onGeneralPurpose4MSB(event: GeneralPurpose4MSB) { }
    open fun onGeneralPurpose5(event: GeneralPurpose5) { }
    open fun onGeneralPurpose6(event: GeneralPurpose6) { }
    open fun onGeneralPurpose7(event: GeneralPurpose7) { }
    open fun onGeneralPurpose8(event: GeneralPurpose8) { }
    open fun onDataIncrement(event: DataIncrement) { }
    open fun onDataDecrement(event: DataDecrement) { }
    open fun onAllControllersOff(event: AllControllersOff) { }
    open fun onAllNotesOff(event: AllNotesOff) { }
    open fun onAllSoundOff(event: AllSoundOff) { }
    open fun onOmniOff(event: OmniOff) { }
    open fun onOmniOn(event: OmniOn) { }
    open fun onPolyphonicOperation(event: PolyphonicOperation) { }
    open fun onProgramChange(event: ProgramChange) { }
    open fun onChannelPressure(event: ChannelPressure) { }
    open fun onPitchWheelChange(event: PitchWheelChange) { }
    open fun onSystemExclusive(event: SystemExclusive) { }
    open fun onMTCQuarterFrame(event: MTCQuarterFrame) { }
    open fun onSongPositionPointer(event: SongPositionPointer) { }
    open fun onSongSelect(event: SongSelect) { }
    open fun onTuneRequest(event: TuneRequest) { }
    open fun onMIDIClock(event: MIDIClock) { }
    open fun onMIDIStart(event: MIDIStart) { }
    open fun onMIDIContinue(event: MIDIContinue) { }
    open fun onMIDIStop(event: MIDIStop) { }
    open fun onActiveSense(event: ActiveSense) { }
    open fun onReset(event: Reset) { }
    open fun onTimeCode(event: TimeCode) { }
}