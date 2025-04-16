//
// Created by pent on 4/15/25.
//

#ifndef PAGAN_SAMPLE_H
#define PAGAN_SAMPLE_H

struct Sample {
    jstring name;
    jint loop_start;
    jint loop_end;
    jint sample_rate;
    jint original_pitch;
    jint pitch_correction;
    jboolean is_linked;
    Sample* linked_sample_ref;
    jint sample_type;
    jint data_placeholder_start;
    jint data_placeholder_end;
    jshort* data;
};
#endif //PAGAN_SAMPLE_H
