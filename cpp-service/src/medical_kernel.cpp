#include "medical_kernel.h"
#include <cmath>
#include <algorithm>
#include <omp.h>

// Thread-local state: No locks needed for high-concurrency streaming
static thread_local float t_spatial_kernel[25];
static thread_local float t_last_sigmaS = -1.0f;

void update_spatial_kernel(float sigmaS) {
    if (std::abs(sigmaS - t_last_sigmaS) < 1e-5f) return;

    float factor = -1.0f / (2.0f * sigmaS * sigmaS);
    for (int i = 0; i < 25; i++) {
        int ky = (i / 5) - 2;
        int kx = (i % 5) - 2;
        t_spatial_kernel[i] = std::exp((kx * kx + ky * ky) * factor);
    }
    t_last_sigmaS = sigmaS;
}

extern "C" {
    void process_medical_image(const uint16_t* input, uint16_t* output,
                                int width, int height,
                                float sigmaS, float sigmaR) {

        update_spatial_kernel(sigmaS);
        float range_factor = -1.0f / (2.0f * sigmaR * sigmaR);

        // OpenMP: Parallelizes the workload across your M4 Pro's 14 cores
        #pragma omp parallel for collapse(2) schedule(static)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float sum_weights = 0.0f;
                float sum_pixels = 0.0f;
                uint16_t central_p = input[y * width + x];

                for (int ky = -2; ky <= 2; ky++) {
                    int ny = std::clamp(y + ky, 0, height - 1);
                    for (int kx = -2; kx <= 2; kx++) {
                        int nx = std::clamp(x + kx, 0, width - 1);

                        uint16_t neighbor_p = input[ny * width + nx];
                        float s_w = t_spatial_kernel[(ky + 2) * 5 + (kx + 2)];

                        float diff = (float)neighbor_p - (float)central_p;
                        float r_w = std::exp((diff * diff) * range_factor);

                        float weight = s_w * r_w;
                        sum_weights += weight;
                        sum_pixels += weight * (float)neighbor_p;
                    }
                }
                output[y * width + x] = (uint16_t)(sum_pixels / sum_weights);
            }
        }
    }
}