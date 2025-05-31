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
package com.spotifyxp.utils;


import com.spotifyxp.PublicValues;
import org.jetbrains.annotations.NotNull;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

public class StringUtils {
    public static String replaceLast(String original, String target, String replacement) {
        int lastIndex = original.lastIndexOf(target);

        if (lastIndex == -1) {
            // Target substring not found
            return original;
        }

        String beforeLast = original.substring(0, lastIndex);
        String afterLast = original.substring(lastIndex + target.length());

        return beforeLast + replacement + afterLast;
    }

    public static double calculateStringSizeInKilobytes(@NotNull String string) {
        double size = (8+4+12+(string.length()* 2))/1024.0;
        if(PublicValues.architecture != ArchitectureDetection.Architecture.x86) {
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            List<String> jvmArgs = runtimeMXBean.getInputArguments();
            boolean isCompressedOopsEnabled = false;
            for (String arg : jvmArgs) {
                if (arg.contains("UseCompressedOops")) {
                    isCompressedOopsEnabled = true;
                    break;
                }
            }
            size = isCompressedOopsEnabled ?
                    (12+4+12+(string.length()* 2))/1024.0
                    :  (16+8+16+(string.length()* 2))/1024.0;
        }
        return size;
    }
}
