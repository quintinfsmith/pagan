//
// Created by pent on 5/1/25.
//

#ifndef PAGAN_LISTCHUNKHEADER_H
#define PAGAN_LISTCHUNKHEADER_H

#include <string>

struct ListChunkHeader {
    int index;
    std::string tag;
    int size;
    std::string type;
};


#endif //PAGAN_LISTCHUNKHEADER_H
