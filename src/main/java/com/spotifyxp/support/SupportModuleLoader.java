/*
 * Copyright [2024-2025] [Gianluca Beil]
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
package com.spotifyxp.support;

import com.spotifyxp.Flags;

import java.util.ArrayList;

public class SupportModuleLoader {
    private static final ArrayList<SupportModule> supportModules = new ArrayList<>();

    public SupportModuleLoader() {
        if(Flags.linuxSupport) supportModules.add(new LinuxSupportModule());
        if(Flags.macosSupport) supportModules.add(new MacOSXSupportModule());
    }

    public void loadModules() {
        for (SupportModule module : supportModules) {
            if (module.getOSName().equals(System.getProperty("os.name"))) {
                module.run();
            }
        }
    }
}
