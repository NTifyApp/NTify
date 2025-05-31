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
package com.spotifyxp.args;

import com.spotifyxp.PublicValues;

public class CustomSaveDir implements Argument {
    @Override
    public Runnable runArgument(String parameter1) {
        return () -> {
            PublicValues.fileslocation = parameter1;
            PublicValues.configfilepath = PublicValues.fileslocation + "/config.json";
            PublicValues.customSaveDir = true;
            PublicValues.appLocation = PublicValues.fileslocation;
        };
    }

    @Override
    public String getName() {
        return "custom-savedir";
    }

    @Override
    public String getDescription() {
        return "Sets save directory (normally AppData) (Paramter z.b 'C://bla' or '/etc/bla')";
    }

    @Override
    public boolean hasParameter() {
        return true;
    }
}