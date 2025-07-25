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
package com.spotifyxp.injector;

import java.util.ArrayList;

public interface InjectorInterface {
    /**
     * Will be removed in favor of the plugin.json
     */
    @Deprecated
    default String getIdentifier() {return "";}

    /**
     * Will be removed in favor of the plugin.json
     */
    @Deprecated
    default String getVersion() {return "";}

    /**
     * Will be removed in favor of the plugin.json
     */
    @Deprecated
    default String getAuthor() {return "";}

    /**
     * Will be removed in favor of the plugin.json
     */
    @Deprecated
    default ArrayList<Injector.Dependency> getDependencies() {
        return new ArrayList<>();
    }

    void init();
}
