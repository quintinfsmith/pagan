//
// Created by pent on 4/16/25.
//

#ifndef PAGAN_GENERATOROPERATION_H
#define PAGAN_GENERATOROPERATION_H

namespace GeneratorOperation {
    const int ModLFOPitch = 0x05;
    const int VibLFOPitch = 0x06;
    const int ModEnvPitch = 0x07;
    const int FilterCutoff = 0x08;
    const int FilterResonance = 0x09;
    const int ModLFOFilter = 0x0A;
    const int ModEnvFilter = 0x0B;
    const int ModLFOToVolume = 0x0D;
    const int Chorus = 0x0F;
    const int Reverb = 0x10;
    const int Pan = 0x11;
    const int ModLFODelay = 0x15;
    const int ModLFOFrequency = 0x16;
    const int VibLFODelay = 0x17;
    const int VibLFOFrequency = 0x18;
    const int ModEnvDelay = 0x19;
    const int ModEnvAttack = 0x1A;
    const int ModEnvHold = 0x1B;
    const int ModEnvDecay = 0x1C;
    const int ModEnvSustain = 0x1D;
    const int ModEnvRelease = 0x1E;
    const int KeyModEnvHold = 0x1F;
    const int KeyModEnvDecay = 0x20;
    const int VolEnvDelay = 0x21;
    const int VolEnvAttack = 0x22;
    const int VolEnvHold = 0x23;
    const int VolEnvDecay = 0x24;
    const int VolEnvSustain = 0x25;
    const int VolEnvRelease = 0x26;
    const int KeyVolEnvHold = 0x27;
    const int KeyVolEnvDecay = 0x28;
    const int KeyRange = 0x2B;
    const int VelocityRange = 0x2C;
    const int Attenuation = 0x30;
    const int TuningCoarse = 0x33;
    const int TuningFine = 0x34;
    const int ScaleTuning = 0x38;
};


#endif //PAGAN_GENERATOROPERATION_H
