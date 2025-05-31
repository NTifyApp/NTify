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
package com.spotifyxp.configuration;

import com.google.common.collect.Lists;
import com.spotifyxp.PublicValues;
import com.spotifyxp.audio.Quality;
import com.spotifyxp.utils.Utils;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Holds all registered config values with their type and default value
 *
 * name - Acts as config key and language key for the config value
 * type - Type of the config value (STRING, BOOLEAN, INT, CUSTOM(Must implement ConfigValue))
 * category (String) - Category of the config value also is as language key
 * defaultValue - Default value for the config value
 */
public enum ConfigValues {
    mypalpath("settings.mypal.path", ConfigValueTypes.STRING, "ui.settings.browser",""),
    theme("settings.ui.theme", ConfigValueTypes.CUSTOM, "ui.settings.ui.label", new CustomConfigValue<>("settings.ui.theme", "DarkGreen", PublicValues.themeLoader.getThemesAsSetting(), PublicValues.themeLoader.getThemes(), ConfigValueTypes.STRING)),
    language("user.settings.language", ConfigValueTypes.CUSTOM, "ui.settings.ui.label", new CustomConfigValue<>("user.settings.language", "English", PublicValues.language.getAvailableLanguages(), null, ConfigValueTypes.STRING)),
    load_all_tracks("user.settings.load_all_tracks", ConfigValueTypes.BOOLEAN, "ui.settings.ui.label", true),
    browse_view_style("user.settings.browse_view_style", ConfigValueTypes.CUSTOM, "ui.settings.ui.label", new CustomConfigValue<>("user.settings.browse_view_style", 0, Lists.newArrayList("Metro", "Table"), Lists.newArrayList(0, 1), ConfigValueTypes.INT)),
    hideExceptions("general.exception.visibility", ConfigValueTypes.BOOLEAN, "ui.settings.ui.label", false),
    spconnect("user.settings.spconnect", ConfigValueTypes.BOOLEAN, "ui.settings.playback.label", false),
    cache_disabled("user.settings.cache.disabled", ConfigValueTypes.BOOLEAN, "ui.settings.playback.label", false),
    disable_autoqueue("user.settings.autoqueue.disabled", ConfigValueTypes.BOOLEAN, "ui.settings.playback.label", false),
    audioquality("settings.playback.quality", ConfigValueTypes.CUSTOM, "ui.settings.playback.label", new CustomConfigValue<>("settings.playback.quality", Quality.NORMAL.configValue(), Lists.newArrayList("Normal", "High", "Very high"), Utils.enumToObjectArray(Quality.values()), ConfigValueTypes.STRING)),
    proxy_enable("proxy.enable", ConfigValueTypes.BOOLEAN, "ui.settings.proxy", false),
    proxy_type("proxy.type", ConfigValueTypes.CUSTOM, "ui.settings.proxy", new CustomConfigValue<>("proxy.type", Proxy.Type.HTTP.name(), Arrays.stream(Proxy.Type.values()).map(Enum::name).collect(Collectors.toCollection(ArrayList::new)), Arrays.stream(Proxy.Type.values()).map(Enum::name).collect(Collectors.toCollection(ArrayList::new)), ConfigValueTypes.STRING)),
    proxy_address("proxy.address", ConfigValueTypes.STRING, "ui.settings.proxy", ""),
    proxy_username("proxy.username", ConfigValueTypes.STRING, "ui.settings.proxy", ""),
    proxy_password("proxy.password", ConfigValueTypes.STRING, "ui.settings.proxy", ""),
    proxy_trustall("proxy.trustall", ConfigValueTypes.BOOLEAN, "ui.settings.proxy", false),
    logging_enable("ui.settings.logging.enablelogfile", ConfigValueTypes.BOOLEAN, "ui.settings.logging.label", true),
    logging_maxkept("ui.settings.logging.maxkeeplogs", ConfigValueTypes.INT, "ui.settings.logging.label", 10),
    other_autoplayenabled("user.settings.other.autoplayenabled", ConfigValueTypes.BOOLEAN, "ui.settings.other", true),
    other_crossfadeduration("user.settings.other.crossfadeduration", ConfigValueTypes.INT, "ui.settings.other", 0),
    other_enablenormalization("user.settings.other.enablenormalization", ConfigValueTypes.BOOLEAN, "ui.settings.other", true),
    other_normalizationpregain("user.settings.other.normalizationpregain", ConfigValueTypes.INT, "ui.settings.other", 3),
    other_mixersearchkeywords("user.settings.other.mixersearchkeywords", ConfigValueTypes.STRING, "ui.settings.other" ,""),
    other_preloadenabled("user.settings.other.preloadenabled", ConfigValueTypes.BOOLEAN, "ui.settings.other" ,true),
    other_releaselinedelay("user.settings.other.releaselinedelay", ConfigValueTypes.INT, "ui.settings.other" ,20),
    other_bypasssinkvolume("user.settings.other.bypasssinkvolume", ConfigValueTypes.BOOLEAN, "ui.settings.other" ,false),
    other_preferredlocale("user.settings.other.preferredlocale", ConfigValueTypes.STRING, "ui.settings.other" ,"en");


    public final String name;
    public final ConfigValueTypes type;
    public final String category;
    public final Object defaultValue;

    ConfigValues(String name, ConfigValueTypes type, String category, Object defaultValue) {
        this.name = name;
        this.type = type;
        this.category = category;
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the ConfigValues instance for the config value
     *
     * @param name name of the config value e.g. user.settings.spconnect
     * @return ConfigValues
     */
    public static ConfigValues get(String name) {
        for (ConfigValues value : ConfigValues.values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        return null;
    }
}
