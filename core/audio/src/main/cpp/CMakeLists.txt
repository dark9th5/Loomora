cmake_minimum_required(VERSION 3.22.1)
project(loomora_rnnoise C)

add_library(
    loomora-rnnoise
    SHARED
    rnnoise_jni.c
    rnnoise/denoise.c
    rnnoise/rnn.c
    rnnoise/rnn_data.c
    rnnoise/pitch.c
    rnnoise/kiss_fft.c
    rnnoise/celt_lpc.c
)

target_include_directories(
    loomora-rnnoise
    PRIVATE
    include
    rnnoise
)

target_compile_options(loomora-rnnoise PRIVATE -O3 -fvisibility=hidden)
target_link_libraries(loomora-rnnoise m)
