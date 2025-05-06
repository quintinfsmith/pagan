//
// Created by pent on 5/5/25.
//

#ifndef PAGAN_SAMPLEDATA_H
#define PAGAN_SAMPLEDATA_H

class SampleData {
    public:
        short* data;
        int size;
        SampleData(short* data, int size)  {
            this->data = data;
            this->size = size;
        }
};

#endif //PAGAN_SAMPLEDATA_H
