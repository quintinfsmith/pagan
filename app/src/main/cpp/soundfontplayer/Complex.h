//
// Created by pent on 5/26/25.
//

#ifndef PAGAN_COMPLEX_H
#define PAGAN_COMPLEX_H
#include <cmath>
#include <vector>
using namespace std;

class Complex {
    public:
        float real;
        float imaginary;
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

        void operator*=(Complex const& c) {
            this->real = (this->real * c.real) - (this->imaginary * c.imaginary);
            this->imaginary = (this->real * c.imaginary) + (this->imaginary * c.real);
        }
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

void _fft(Complex* input, int size, Complex* output) {
    if (size == 1) {
        output[0] = input[0];
        return;
    }

    int half_size = size / 2;
    Complex* input_evens = (Complex*)malloc(sizeof(Complex) * half_size);
    Complex* input_odds = (Complex*)malloc(sizeof(Complex) * half_size);

    for (int i = 0; i < half_size; i++) {
        input_evens[i] = input[i * 2];
        input_odds[i] = input[(i * 2) + 1];
    }

    Complex* result_evens = (Complex*)malloc(sizeof(Complex) * half_size);
    Complex* result_odds = (Complex*)malloc(sizeof(Complex) * half_size);
    _fft(input_evens, half_size, result_evens);
    _fft(input_odds, half_size, result_odds);

    Complex* twiddle_factors = (Complex*)malloc(sizeof(Complex) * half_size);
    float ratio = float(2 * M_PI) / (float)size;
    for (int i = 0; i < half_size; i++) {
        float v = (float)i * ratio;
        twiddle_factors[i] = Complex(cos(v), sin(v));
    }

    for (int i = 0; i < half_size; i++) {
        output[i] = result_evens[i] + (twiddle_factors[i] * result_odds[i]);
        output[i + half_size] = result_evens[i] - (twiddle_factors[i] * result_odds[i]);
    }

    free(twiddle_factors);
    free(input_evens);
    free(input_odds);
    free(result_evens);
    free(result_odds);
}

void _ifft(Complex* input, int size, Complex* output) {
    if (size == 1) {
        output[0] = input[0];
        return;
    }

    int half_size = size / 2;
    Complex* input_evens = (Complex*)malloc(sizeof(Complex) * half_size);
    Complex* input_odds = (Complex*)malloc(sizeof(Complex) * half_size);

    for (int i = 0; i < half_size; i++) {
        input_evens[i] = input[i * 2];
        input_odds[i] = input[(i * 2) + 1];
    }

    Complex* result_evens = (Complex*)malloc(sizeof(Complex) * half_size);
    Complex* result_odds = (Complex*)malloc(sizeof(Complex) * half_size);
    _ifft(input_evens, half_size, result_evens);
    _ifft(input_odds, half_size, result_odds);

    Complex* twiddle_factors = (Complex*)malloc(sizeof(Complex) * half_size);
    float ratio = float(-2 * M_PI) / (float)size;
    for (int i = 0; i < half_size; i++) {
        float v = (float)i * ratio;
        twiddle_factors[i] = Complex(cos(v), sin(v));
    }

    for (int i = 0; i < half_size; i++) {
        output[i] = (result_evens[i] + (twiddle_factors[i] * result_odds[i])) / 2;
        output[i + half_size] = (result_evens[i] - (twiddle_factors[i] * result_odds[i])) / 2;
    }

    free(twiddle_factors);
    free(input_evens);
    free(input_odds);
    free(result_evens);
    free(result_odds);
}

Complex* fft(float* input, int input_size, int new_size) {

    auto new_input = (Complex*)malloc(sizeof(Complex) * new_size);
    for (int i = 0; i < input_size; i++) {
        new_input[i] = Complex(input[i], 0);
    }

    for (int i = input_size; i < new_size; i++) {
        new_input[i] = Complex(0, 0);
    }

    Complex* output = (Complex*)malloc(sizeof(Complex) * new_size);
    _fft(new_input, new_size, output);

    return output;
}

Complex* ifft(Complex* input, int input_size) {
    Complex* output = (Complex*)malloc(sizeof(Complex) * input_size);
    _ifft(input, input_size, output);
    return output;
}

#endif //PAGAN_COMPLEX_H
