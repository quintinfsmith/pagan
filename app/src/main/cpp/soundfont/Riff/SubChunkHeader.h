//
// Created by pent on 5/1/25.
//

#ifndef PAGAN_SUBCHUNKHEADER_H
#define PAGAN_SUBCHUNKHEADER_H

#include <string>

struct SubChunkHeader {
    int index;
    std::string tag;
    int size;
};


#endif //PAGAN_SUBCHUNKHEADER_H
