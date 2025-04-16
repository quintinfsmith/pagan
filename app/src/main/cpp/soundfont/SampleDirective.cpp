#include <jni.h>
#include <iostream>
#include <string>
#include <exception>

#include "SampleDirective.h"
#include "Generated.h"
#include "Sample.h"

class SampleDirective : public Generated {
    std::optional<Sample*> sample;
    std::optional<int> sampleStartOffset;
    std::optional<int> sampleEndOFfset;
    std::optional<int> loopStartOffset;
    std::optional<int> loopEndOffset;
    std::optional<int> sampleMode;
    std::optional<int> root_key;
    std::optional<int> exclusive_class;
    std::optional<int> keynum;
    std::optional<int> velocity;
    // val modulators = HashMap<Generator.Operation, MutableSet<Modulator>>()
};