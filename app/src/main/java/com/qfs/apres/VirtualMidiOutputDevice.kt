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
import com.qfs.apres.event.GeneralMIDIEvent
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
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79

interface VirtualMidiOutputDevice {
    fun receiveMessage(event: GeneralMIDIEvent) {
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

            is NoteOn79 -> this.onNoteOn79(event)
            is NoteOff79 -> this.onNoteOff79(event)
        }
    }

    // abstract functions
    fun onSequenceNumber(event: SequenceNumber) { }
    fun onText(event: Text) { }
    fun onCopyRightNotice(event: CopyRightNotice) { }
    fun onTrackName(event: TrackName) { }
    fun onInstrumentName(event: InstrumentName) { }
    fun onLyric(event: Lyric) { }
    fun onMarker(event: Marker) { }
    fun onCuePoint(event: CuePoint) { }
    fun onEndOfTrack(event: EndOfTrack) { }
    fun onChannelPrefix(event: ChannelPrefix) { }
    fun onSetTempo(event: SetTempo) { }
    fun onSMPTEOffset(event: SMPTEOffset) { }
    fun onTimeSignature(event: TimeSignature) { }
    fun onKeySignature(event: KeySignature) { }
    fun onSequencerSpecific(event: SequencerSpecific) { }
    fun onNoteOn(event: NoteOn) { }
    fun onNoteOff(event: NoteOff) { }
    fun onPolyphonicKeyPressure(event: PolyphonicKeyPressure) { }
    fun onHoldPedal(event: HoldPedal) { }
    fun onPortamento(event: Portamento) { }
    fun onSustenuto(event: Sustenuto) { }
    fun onSoftPedal(event: SoftPedal) { }
    fun onLegato(event: Legato) { }
    fun onHold2Pedal(event: Hold2Pedal) { }
    fun onSoundVariation(event: SoundVariation) { }
    fun onSoundTimbre(event: SoundTimbre) { }
    fun onSoundReleaseTime(event: SoundReleaseTime) { }
    fun onSoundAttack(event: SoundAttack) { }
    fun onSoundBrightness(event: SoundBrightness) { }
    fun onSoundControl1(event: SoundControl1) { }
    fun onSoundControl2(event: SoundControl2) { }
    fun onSoundControl3(event: SoundControl3) { }
    fun onSoundControl4(event: SoundControl4) { }
    fun onSoundControl5(event: SoundControl5) { }
    fun onEffectsLevel(event: EffectsLevel) { }
    fun onTremuloLevel(event: TremuloLevel) { }
    fun onChorusLevel(event: ChorusLevel) { }
    fun onCelesteLevel(event: CelesteLevel) { }
    fun onPhaserLevel(event: PhaserLevel) { }
    fun onLocalControl(event: LocalControl) { }
    fun onMonophonicOperation(event: MonophonicOperation) { }
    fun onBankSelect(event: BankSelect) { }
    fun onBankSelectLSB(event: BankSelectLSB) { }
    fun onBankSelectMSB(event: BankSelectMSB) { }
    fun onModulationWheel(event: ModulationWheel) { }
    fun onModulationWheelLSB(event: ModulationWheelLSB) { }
    fun onModulationWheelMSB(event: ModulationWheelMSB) { }
    fun onBreathController(event: BreathController) { }
    fun onBreathControllerLSB(event: BreathControllerLSB) { }
    fun onBreathControllerMSB(event: BreathControllerMSB) { }
    fun onFootPedal(event: FootPedal) { }
    fun onFootPedalLSB(event: FootPedalLSB) { }
    fun onFootPedalMSB(event: FootPedalMSB) { }
    fun onPortamentoTime(event: PortamentoTime) { }
    fun onPortamentoTimeLSB(event: PortamentoTimeLSB) { }
    fun onPortamentoTimeMSB(event: PortamentoTimeMSB) { }
    fun onDataEntry(event: DataEntry) { }
    fun onDataEntryLSB(event: DataEntryLSB) { }
    fun onDataEntryMSB(event: DataEntryMSB) { }
    fun onVolume(event: Volume) { }
    fun onVolumeLSB(event: VolumeLSB) { }
    fun onVolumeMSB(event: VolumeMSB) { }
    fun onBalance(event: Balance) { }
    fun onBalanceLSB(event: BalanceLSB) { }
    fun onBalanceMSB(event: BalanceMSB) { }
    fun onPan(event: Pan) { }
    fun onPanLSB(event: PanLSB) { }
    fun onPanMSB(event: PanMSB) { }
    fun onExpression(event: Expression) { }
    fun onExpressionLSB(event: ExpressionLSB) { }
    fun onExpressionMSB(event: ExpressionMSB) { }
    fun onNonRegisteredParameterNumber(event: NonRegisteredParameterNumber) { }
    fun onNonRegisteredParameterNumberLSB(event: NonRegisteredParameterNumberLSB) { }
    fun onNonRegisteredParameterNumberMSB(event: NonRegisteredParameterNumberMSB) { }
    fun onRegisteredParameterNumber(event: RegisteredParameterNumber) { }
    fun onRegisteredParameterNumberLSB(event: RegisteredParameterNumberLSB) { }
    fun onRegisteredParameterNumberMSB(event: RegisteredParameterNumberMSB) { }
    fun onEffectControl1(event: EffectControl1) { }
    fun onEffectControl1LSB(event: EffectControl1LSB) { }
    fun onEffectControl1MSB(event: EffectControl1MSB) { }
    fun onEffectControl2(event: EffectControl2) { }
    fun onEffectControl2LSB(event: EffectControl2LSB) { }
    fun onEffectControl2MSB(event: EffectControl2MSB) { }
    fun onGeneralPurpose1(event: GeneralPurpose1) { }
    fun onGeneralPurpose1LSB(event: GeneralPurpose1LSB) { }
    fun onGeneralPurpose1MSB(event: GeneralPurpose1MSB) { }
    fun onGeneralPurpose2(event: GeneralPurpose2) { }
    fun onGeneralPurpose2LSB(event: GeneralPurpose2LSB) { }
    fun onGeneralPurpose2MSB(event: GeneralPurpose2MSB) { }
    fun onGeneralPurpose3(event: GeneralPurpose3) { }
    fun onGeneralPurpose3LSB(event: GeneralPurpose3LSB) { }
    fun onGeneralPurpose3MSB(event: GeneralPurpose3MSB) { }
    fun onGeneralPurpose4(event: GeneralPurpose4) { }
    fun onGeneralPurpose4LSB(event: GeneralPurpose4LSB) { }
    fun onGeneralPurpose4MSB(event: GeneralPurpose4MSB) { }
    fun onGeneralPurpose5(event: GeneralPurpose5) { }
    fun onGeneralPurpose6(event: GeneralPurpose6) { }
    fun onGeneralPurpose7(event: GeneralPurpose7) { }
    fun onGeneralPurpose8(event: GeneralPurpose8) { }
    fun onDataIncrement(event: DataIncrement) { }
    fun onDataDecrement(event: DataDecrement) { }
    fun onAllControllersOff(event: AllControllersOff) { }
    fun onAllNotesOff(event: AllNotesOff) { }
    fun onAllSoundOff(event: AllSoundOff) { }
    fun onOmniOff(event: OmniOff) { }
    fun onOmniOn(event: OmniOn) { }
    fun onPolyphonicOperation(event: PolyphonicOperation) { }
    fun onProgramChange(event: ProgramChange) { }
    fun onChannelPressure(event: ChannelPressure) { }
    fun onPitchWheelChange(event: PitchWheelChange) { }
    fun onSystemExclusive(event: SystemExclusive) { }
    fun onMTCQuarterFrame(event: MTCQuarterFrame) { }
    fun onSongPositionPointer(event: SongPositionPointer) { }
    fun onSongSelect(event: SongSelect) { }
    fun onTuneRequest(event: TuneRequest) { }
    fun onMIDIClock(event: MIDIClock) { }
    fun onMIDIStart(event: MIDIStart) { }
    fun onMIDIContinue(event: MIDIContinue) { }
    fun onMIDIStop(event: MIDIStop) { }
    fun onActiveSense(event: ActiveSense) { }
    fun onReset(event: Reset) { }
    fun onTimeCode(event: TimeCode) { }

    fun onNoteOn79(event: NoteOn79) { }
    fun onNoteOff79(event: NoteOff79) { }
}
