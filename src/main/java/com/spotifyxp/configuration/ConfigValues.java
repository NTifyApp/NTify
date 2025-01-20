package com.spotifyxp.configuration;

import com.google.common.collect.Lists;
import com.spotifyxp.PublicValues;
import com.spotifyxp.audio.Quality;
import com.spotifyxp.utils.Utils;

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
