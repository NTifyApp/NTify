/*
 * Copyright 2021 devgianlu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.spotifyxp.deps.xyz.gianlu.librespot.player.metrics;

import com.spotifyxp.deps.xyz.gianlu.librespot.audio.DecodedAudioStream;
import com.spotifyxp.deps.xyz.gianlu.librespot.audio.PlayableContentFeeder;
import com.spotifyxp.deps.xyz.gianlu.librespot.decoders.Decoder;
import com.spotifyxp.deps.xyz.gianlu.librespot.player.crossfade.CrossfadeController;
import com.spotifyxp.deps.xyz.gianlu.librespot.player.mixing.output.OutputAudioFormat;
import org.jetbrains.annotations.Nullable;

/**
 * @author devgianlu
 */
public final class PlayerMetrics {
    public final PlayableContentFeeder.Metrics contentMetrics;
    public int decodedLength = 0;
    public int size = 0;
    public int bitrate = 0;
    public float sampleRate = 0;
    public int duration = 0;
    public String encoding = null;
    public int fadeOverlap = 0;
    public String transition = "none";
    public int decryptTime = 0;

    public PlayerMetrics(@Nullable PlayableContentFeeder.Metrics contentMetrics, @Nullable CrossfadeController crossfade,
                         @Nullable DecodedAudioStream stream, @Nullable Decoder decoder) {
        this.contentMetrics = contentMetrics;

        if (decoder != null) {
            size = decoder.size();
            duration = decoder.duration();

            OutputAudioFormat format = decoder.getAudioFormat();
            bitrate = (int) (format.getFrameRate() * format.getFrameSize());
            sampleRate = format.getSampleRate();
        }

        if (stream != null) {
            decryptTime = stream.decryptTimeMs();
            decodedLength = stream.stream().decodedLength();

            switch (stream.codec()) {
                case MP3:
                    encoding = "mp3";
                    break;
                case VORBIS:
                    encoding = "vorbis";
                    break;
                case AAC:
                    encoding = "aac";
                    break;
            }
        }

        if (crossfade != null) {
            transition = "crossfade";
            fadeOverlap = crossfade.fadeOverlap();
        }
    }
}
