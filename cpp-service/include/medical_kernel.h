#ifndef MEDICAL_KERNEL_H
#define MEDICAL_KERNEL_H

#include <stdint.h>

extern "C" {
    /**
     * @param input Raw 16-bit pixel buffer from Java
     * @param output Destination buffer (Zero-copy)
     * @param width Image width
     * @param height Image height
     * @param sigmaS Spatial standard deviation (blur intensity)
     * @param sigmaR Range standard deviation (edge preservation)
     */
    void process_medical_image(const uint16_t* input, uint16_t* output,
                                int width, int height,
                                float sigmaS, float sigmaR);
}

#endif // MEDICAL_KERNEL_H