/*
 * Copyright [2023] [Gianluca Beil]
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

public class Development implements Argument {
    @Override
    public Runnable runArgument(String parameter1) {
        return new Runnable() {
            @Override
            public void run() {
                PublicValues.devMode = true;
            }
        };
    }

    @Override
    public String getName() {
        return "devMode";
    }

    @Override
    public String getDescription() {
        return "Enable devMode";
    }

    @Override
    public boolean hasParameter() {
        return false;
    }
}
