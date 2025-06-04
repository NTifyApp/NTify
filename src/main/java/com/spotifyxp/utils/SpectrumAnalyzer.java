/*
 * Copyright [2023-2024] [Gianluca Beil]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.spotifyxp.utils;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

public class SpectrumAnalyzer {
    private final FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

    public double[][] analyzeStereoAudio(byte[] audioData, boolean mixToMono) {
        int sampleCount = audioData.length / 4;
        double[] left = new double[sampleCount];
        double[] right = new double[sampleCount];

        for (int i = 0; i < sampleCount; i++) {
            int leftSample = (audioData[i * 4] & 0xFF) | (audioData[i * 4 + 1] << 8);
            int rightSample = (audioData[i * 4 + 2] & 0xFF) | (audioData[i * 4 + 3] << 8);
            left[i] = leftSample / 32768.0;
            right[i] = rightSample / 32768.0;
        }

        if (mixToMono) {
            double[] mixed = new double[sampleCount];
            for (int i = 0; i < sampleCount; i++) {
                mixed[i] = (left[i] + right[i]) / 2.0;
            }
            return new double[][] { analyze(mixed) };
        } else {
            return new double[][] {
                    analyze(left),
                    analyze(right)
            };
        }
    }

    private double[] analyze(double[] samples) {
        for (int i = 0; i < samples.length; i++) {
            samples[i] *= 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (samples.length - 1));
        }

        Complex[] spectrum = fft.transform(samples, TransformType.FORWARD);
        double[] result = new double[spectrum.length / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = Math.sqrt(Math.pow(spectrum[i].getReal(), 2) + Math.pow(spectrum[i].getImaginary(), 2));
        }
        return result;
    }
}
