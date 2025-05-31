/*
 * Copyright [2023-2025] [Gianluca Beil]
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
package com.spotifyxp.lib;

import java.util.Locale;

public class libDetect {
    public enum OSType {
        Windows, MacOS, Linux, Other
    }

    protected static OSType detectedOS;

    static OSType getOperatingSystemType() {
        String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if ((OS.contains("mac")) || (OS.contains("darwin"))) {
            detectedOS = OSType.MacOS;
        } else if (OS.contains("win")) {
            detectedOS = OSType.Windows;
        } else if (OS.contains("nux")) {
            detectedOS = OSType.Linux;
        } else {
            detectedOS = OSType.Other;
        }
        return detectedOS;
    }

    public static OSType getDetectedOS() {
        detectedOS = getOperatingSystemType();
        return detectedOS;
    }

    /**
     * Returns true if the operating system is Windows
     */
    public static boolean isWindows() {
        detectedOS = getOperatingSystemType();
        return detectedOS == OSType.Windows;
    }

    /**
     * Returns true if the operating system is Linux
     */
    public static boolean isLinux() {
        detectedOS = getOperatingSystemType();
        return detectedOS == OSType.Linux;
    }

    /**
     * Returns true if the operating system is MacOS
     */
    public static boolean isMacOS() {
        detectedOS = getOperatingSystemType();
        return detectedOS == OSType.MacOS;
    }

    /**
     * Returns true if the operating system is Other (unspecified)
     */
    public static boolean isOther() {
        detectedOS = getOperatingSystemType();
        return detectedOS == OSType.Other;
    }
}
