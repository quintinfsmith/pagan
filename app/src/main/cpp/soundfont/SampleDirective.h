#ifndef PAGAN_SAMPLEDIRECTIVE_H
#define PAGAN_SAMPLEDIRECTIVE_H

#include "Generated.h"
#include "Sample.h"

class SampleDirective : public Generated {
    std::optional<Sample*> sample;
    std::optional<int> sampleStartOffset;
    std::optional<int> sampleEndOffset;
    std::optional<int> loopStartOffset;
    std::optional<int> loopEndOffset;
    std::optional<int> sampleMode;
    std::optional<int> root_key;
    std::optional<int> exclusive_class;
    std::optional<int> keynum;
    std::optional<int> velocity;
    // val modulators = HashMap<Generator.Operation, MutableSet<Modulator>>()
};

#endif //PAGAN_SAMPLEDIRECTIVE_H
