//
// Created by pent on 5/26/25.
//

#ifndef PAGAN_COMPLEX_H
#define PAGAN_COMPLEX_H

#include <cmath>
#include <vector>
using namespace std;

class Complex {
    float real;
    float imaginary;
    public:
        Complex() {
            this->real = 0;
            this->imaginary = 0;
        }

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

Complex operator/(Complex const& c, float const& f) {
    return c / Complex(f, 0);
}

bool operator==(Complex const& c1, Complex const& c2) {
    return c1.real == c2.real && c1.imaginary == c2.imaginary;
}

vector<Complex> _fft(vector<Complex> input) {
    int size = input.size();
    if (size == 1) {
        return input;
    }

    int half_size = size / 2;
    Complex twiddle_factors[half_size];
    for (int i = 0; i < half_size; i++) {
        float v = (2 * (float)M_PI * (float)i) / (float)size;
        twiddle_factors[i] = Complex(cos(v), sin(v));
    }

    vector<Complex> input_evens;
    vector<Complex> input_odds;
    input_evens.reserve(half_size);
    input_odds.reserve(half_size);

    for (int i = 0; i < half_size; i++) {
        input_evens[i] = input[i * 2];
        input_odds[i] = input[(i * 2) + 1];
    }

    vector<Complex> result_evens = _fft(input_evens);
    vector<Complex> result_odds = _fft(input_odds);

    vector<Complex> output;
    output.reserve(size);
    for (int i = 0; i < size; i++) {
        int x = i % half_size;
        if (i < half_size) {
            output[i] = result_evens[x] + (twiddle_factors[x] * result_odds[x]);
        } else {
            output[i] = result_evens[x] - (twiddle_factors[x] * result_odds[x]);
        }
    }

    return output;
}

vector<Complex> _ifft(vector<Complex> input) {
    int size = input.size();
    if (size == 1) {
        return input;
    }

    int half_size = size / 2;
    Complex twiddle_factors[half_size];
    for (int i = 0; i < half_size; i++) {
        float v = (-2 * (float)M_PI * (float)i) / (float)size;
        twiddle_factors[i] = Complex(cos(v), sin(v));
    }

    vector<Complex> input_evens;
    input_evens.reserve(half_size);
    vector<Complex> input_odds;
    input_odds.reserve(half_size);

    for (int i = 0; i < half_size; i++) {
        input_evens[i] = input[i * 2];
        input_odds[i] = input[(i * 2) + 1];
    }

    vector<Complex> result_evens = _ifft(input_evens);
    vector<Complex> result_odds = _ifft(input_odds);

    vector<Complex> output;
    output.reserve(size);

    for (int i = 0; i < size; i++) {
        int x = i % half_size;
        if (i < half_size) {
            output[i] = (result_evens[x] + (twiddle_factors[x] * result_odds[x])) / 2;
        } else {
            output[i] = (result_evens[x] - (twiddle_factors[x] * result_odds[x])) / 2;
        }
    }

    return output;
}

vector<Complex> fft(float* input, int input_size) {
    int new_size = 1;
    while (new_size < input_size) {
        new_size *= 2;
    }

    vector<Complex> new_input;
    new_input.reserve(new_size);

    for (int i = 0; i < input_size; i++) {
        new_input[i] = Complex(input[i], 0);
    }

    for (int i = input_size; i < new_size; i++) {
        new_input[i] = Complex();
    }

    return _fft(new_input);
}

#endif //PAGAN_COMPLEX_H
