#include <jni.h>
#include <math.h>
#include <stdint.h>
#include <stdlib.h>

#include "rnnoise.h"

#define RNNOISE_FRAME_SIZE 480

JNIEXPORT jlong JNICALL
Java_com_loomora_core_audio_enhance_RnnoiseProcessor_nativeCreate(
        JNIEnv *env,
        jobject instance) {
    (void) env;
    (void) instance;
    return (jlong) (intptr_t) rnnoise_create();
}

JNIEXPORT void JNICALL
Java_com_loomora_core_audio_enhance_RnnoiseProcessor_nativeProcess(
        JNIEnv *env,
        jobject instance,
        jlong handle,
        jshortArray samples,
        jfloat strength) {
    (void) instance;
    DenoiseState *state = (DenoiseState *) (intptr_t) handle;
    if (state == NULL || (*env)->GetArrayLength(env, samples) != RNNOISE_FRAME_SIZE) return;

    jshort input[RNNOISE_FRAME_SIZE];
    float source[RNNOISE_FRAME_SIZE];
    float filtered[RNNOISE_FRAME_SIZE];
    (*env)->GetShortArrayRegion(env, samples, 0, RNNOISE_FRAME_SIZE, input);

    for (int i = 0; i < RNNOISE_FRAME_SIZE; ++i) source[i] = (float) input[i];
    rnnoise_process_frame(state, filtered, source);

    const float mix = fmaxf(0.0f, fminf(1.0f, strength));
    for (int i = 0; i < RNNOISE_FRAME_SIZE; ++i) {
        float value = source[i] + (filtered[i] - source[i]) * mix;
        value = fmaxf(-32768.0f, fminf(32767.0f, value));
        input[i] = (jshort) lrintf(value);
    }
    (*env)->SetShortArrayRegion(env, samples, 0, RNNOISE_FRAME_SIZE, input);
}

JNIEXPORT void JNICALL
Java_com_loomora_core_audio_enhance_RnnoiseProcessor_nativeDestroy(
        JNIEnv *env,
        jobject instance,
        jlong handle) {
    (void) env;
    (void) instance;
    DenoiseState *state = (DenoiseState *) (intptr_t) handle;
    if (state != NULL) rnnoise_destroy(state);
}
