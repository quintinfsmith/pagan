//
// Created by pent on 5/26/25.
//

#ifndef PAGAN_COMPLEX_H
#define PAGAN_COMPLEX_H

#include <cmath>
using namespace std;

class Complex {
    float real;
    float imaginary;
    public:
        Complex(float real, float imaginary) {
            this->real = real;
            this->imaginary = imaginary;
        }

        friend Complex operator+(Complex const& c1, Complex const& c2);
        friend Complex operator-(Complex const& c1, Complex const& c2);
        friend Complex operator*(Complex const& c1, Complex const& c2);
        friend Complex operator/(Complex const& c1, Complex const& c2);
        friend bool operator==(Complex const& c1, Complex const& c2);
};

Complex operator+(Complex const& c1, Complex const& c2) {
    return Complex(
        c1.real + c2.real,
        c1.imaginary + c2.imaginary
    );
}

Complex operator-(Complex const& c1, Complex const& c2) {
    return Complex(
        c1.real - c2.real,
        c1.imaginary - c2.imaginary
    );
}

Complex operator*(Complex const& c1, Complex const& c2) {
    return Complex(
        (c1.real * c2.real) - (c1.imaginary * c2.imaginary),
        (c1.real * c2.imaginary) + (c1.imaginary * c2.real)
    );
}

Complex operator/(Complex const& c1, Complex const& c2) {
    float divisor = pow(c2.real, 2) + pow(c2.imaginary, 2);
    return Complex(
        ((c1.real * c2.real) + (c1.imaginary * c2.imaginary)) / divisor,
        ((c1.imaginary * c2.real) - (c1.real * c2.imaginary)) / divisor
    );
}

bool operator==(Complex const& c1, Complex const& c2) {
    return c1.real == c2.real && c1.imaginary == c2.imaginary;
}

#endif //PAGAN_COMPLEX_H
