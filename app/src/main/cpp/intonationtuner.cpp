// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("intonationtuner");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("intonationtuner")
//      }
//    }

#include <jni.h>
#include <math.h>
#include <cstdint>

/*int flwt(uint8_t audioData[]) {
    return 0;
}*/

extern "C"
JNIEXPORT jfloat JNICALL
Java_ece_wisc_intonationtuner_MainActivity_flwt(JNIEnv *env, jclass clazz, jfloatArray audio_data) {
    // Declare variables
    static float last_freq = 0;
    const float fs = 45000.0;
    const int lev = 6;
    const float global_max_thresh = 0.75;
    const float max_freq = 3000.0;
    const int diff_levs = 3;

    float* a[lev];
    //uint_fast8_t* d[lev];

    float old_mode, new_mode;
    float mode[lev];
    uint_fast16_t length, new_width;
    float freq;
    uint_fast16_t max_cnt[lev];
    uint_fast16_t min_cnt[lev];
    float avg, max, min;
    float max_thresh, min_thresh;
    uint_fast16_t min_dist;
    int_fast16_t climber;
    float test;
    bool can_ext;
    int_fast16_t too_close;
    uint_fast16_t d_cnt;
    uint_fast16_t num, num_j;
    int_fast16_t differs[2 * diff_levs];
    int_fast16_t running_cnt;

    // Set up environment to run pitch detection in C/C++

    jboolean isCopy = JNI_FALSE;
    jfloat* b = env->GetFloatArrayElements(audio_data, &isCopy);
    length = env->GetArrayLength(audio_data);

    for (int i = 0; i < lev; i++) {
        a[i] = (float*)calloc(length, sizeof(float));
    }

    avg = 0;
    max = 0;
    min = -1; // I just picked some large value
    for (int i = 0; i < length; i++) {
        a[0][i] = b[i];
        avg += b[i];

        if (b[i] > max)
            max = b[i];
        if (b[i] < min)
            min = b[i];
    }
    avg /= length; // TODO: Check if dividing float by an int works out

    max_thresh = global_max_thresh * (max - avg) + avg;
    min_thresh = global_max_thresh * (min - avg) + avg;

    if (last_freq) {
        old_mode = fs / last_freq;
    }

    freq = 0;
    max_cnt[0] = 0;
    min_cnt[0] = 0;

    // Time for pitch detection
    for (int i = 1; i < lev; i++) {
        printf("Level 1");

        // Compute FLWT
        new_width = length / pow(2, i);
        for (int j = 0; j < new_width; j++) {
            a[i][j] = a[i - 1][2 * j - 1] + (a[i - 1][2 * j] - a[i - 1 ][2 * j - 1]) / 2;
        }

        // Find maxes of current approximation
        min_dist = floor(1.0 * fs / max_freq / pow(2, (i - 1)));
        if (min_dist < 1)
            min_dist = 1;
        max_cnt[i] = 0;
        min_cnt[i] = 0;
        climber =  0;

        if (a[i][2] - a[i][1] > 0) {
            climber = 1;
        } else {
            climber = -1;
        }
        can_ext = true;
        too_close = 0;

        for (int j = 1; j < new_width - 1; j++) {
            test = a[i][j] - a[i][j - 1];
            if (climber && (test < 0)) {
                if (a[i][j - 1] >= max_thresh && can_ext && !too_close) {
                    max_cnt[i] += 1;

                    //max_indices[i][max_cnt[i]] = j - 1;
                    can_ext = 0;
                    too_close = min_dist;

                }
                climber = -1;
            } else if(climber <= 0 && test > 0) {
                if (a[i][j - 1] <= min_thresh && can_ext && !too_close) {
                    min_cnt[i] += 1;
                    //min_indices[i][min_cnt[i]] = j - 1;
                    can_ext = 0;
                    too_close = min_dist;
                }
                    climber = 1;
            }


            if (((a[i][j] <= avg) && (a[i][j - 1] > avg)) || ((a[i][j] >= avg) && (a[i][j - 1] < avg)))
                can_ext = 1;

            if (too_close) {
                too_close = too_close - 1;
            }
        }

        // Calculate the mode distance between peaks at each level
        if ((max_cnt[i] >= 2) && (min_cnt[i] >= 2)) {
            //differs = [];
            for (int j = 0; j < diff_levs; j++) {
                for (int k = 0; k < max_cnt[i]; k++) {
                    //differs = [differs abs(max_indices[i][k+j] - max_indices]i][k])];
                }
                for (int k = 0; k < min_cnt[i]; k++) {
                    //differs = [differs abs(min_indices[i][k+j] - min_indices]i][k])];
                }
            }
            //d_cnt = length(differs);
            num = 1;
            mode[i] = 0;


            for (int j = 0; j < d_cnt; j++) {
                num_j = 0;
                for (int k = 0; k < d_cnt; k++) {
                    if (abs(differs[k] - differs[j]) <= min_dist)
                        num_j++;
                }
                if ((num_j >= num) && (num_j > (floor(new_width / differs[j]) / 4))) {
                    if (num_j == num) {
                        if (old_mode && abs(differs[j] - old_mode / (pow(2, (i - 1)))) < min_dist) {
                            mode[i] = differs[j];
                        } else if (!old_mode && (differs[j] > 1.95 * mode[i] && differs[j] < 2.05 * mode[i])) {
                            mode[i] = differs[j];
                        }
                    } else {
                        num = num_j;
                        mode[i] = differs[j];
                    }
                } else if ((num_j == num - 1) && old_mode && (abs(differs[j] - (old_mode / pow(2, (i-1)))) < min_dist)) {
                    mode[i] = differs[j];
                }
            }

            // Set mode by averaging
            if (mode[i]) {
                for (int j = 0; j < d_cnt; j++) {
                    if (abs(mode[i] - differs[j]) <= min_dist) {
                        running_cnt++;
                        new_mode += differs[j];
                    }
                }
                new_mode /= running_cnt;
            }

            // Determine if the modes are shared

            if (mode[i - 1] && max_cnt[i - 1] >= 2 && min_cnt[i - 1] >= 2) {
                if (abs(mode[i - 1] - (2 * mode[i])) <= min_dist) {
                    for (int j = 0; j < lev; j++) {
                        free(a[j]);
                    }
                    return fs / mode[i - 1] / pow(2, (i-2));
                }
            }
        }
    }

    for (int i = 0; i < lev; i++) {
        //free(a[i]);
    }
    //return a[0][0];
    return -1;
}