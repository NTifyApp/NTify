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
package com.spotifyxp.audio;

/**
 * Holds all available audio qualities<br><br>
 * - Normal<br>
 * - High<br>
 * - Very high
 */
public enum Quality {
    NORMAL("Normal", "NORMAL"),
    HIGH("High", "HIGH"),
    VERY_HIGH("VeryHigh", "VERYHIGH");
    private final String toselect;
    private final String configValue;

    Quality(String toselect, String configValue) {
        this.toselect = toselect;
        this.configValue = configValue;
    }

    /**
     * Returns the audio quality as a readable string
     *
     * @return String
     */
    public String getAsString() {
        return toselect;
    }

    /**
     * Returns the audio quality as a config value
     *
     * @return String
     */
    public String configValue() {
        return configValue;
    }
}
