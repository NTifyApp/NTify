/*
 * Copyright [2026] [Gianluca Beil]
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
import com.spotifyxp.theming.ThemeLoader;
import com.spotifyxp.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigValues implements IConfig {
    @Config.CustomComponent(id = "settings.mypal.path", translationKey = "dialogs.settings.browser.mypal_path", category = "dialogs.settings.browser.name", component = MypalComponentProvider.class)
    public String mypalPath = "";

    @Config.Dropdown(id = "settings.ui.theme", translationKey = "dialogs.settings.ui.theme", category = "dialogs.settings.ui.name", values = ThemeListProvider.class)
    public String theme = "DarkGreen";

    @Config.Dropdown(id = "user.settings.language", translationKey = "dialogs.settings.ui.language", category = "dialogs.settings.ui.name", values = LanguageListProvider.class)
    public String language = "English";

    @Config.Dropdown(id = "user.settings.browse_view_style", translationKey = "dialogs.settings.ui.browse_view_style", category = "dialogs.settings.ui.name", values = BrowseViewStyleProvider.class, mapping = BrowseViewStyleMappingProvider.class)
    public int browseViewStyle = 0;

    @Config.CheckBox(id = "general.exception.visibility", translationKey = "dialogs.settings.ui.exception_visibility", category = "dialogs.settings.ui.name")
    public boolean hideExceptions = false;

    @Config.CheckBox(id = "user.settings.cache.disabled", translationKey = "dialogs.settings.playback.cache", category = "dialogs.settings.playback.name")
    public boolean cacheDisabled = false;

    @Config.Dropdown(id = "settings.playback.quality", translationKey = "dialogs.settings.playback.quality", category = "dialogs.settings.playback.name", values = AudioQualityProvider.class, mapping = AudioQualityMappingProvider.class)
    public String audioQuality = "NORMAL";

    @Config.CheckBox(id = "user.settings.autoqueue.disabled", translationKey = "dialogs.settings.playback.autoqueue", category = "dialogs.settings.playback.name")
    public boolean disableAutoQueue = false;

    @Config.CheckBox(id = "proxy.enable", translationKey = "dialogs.settings.proxy.enable", category = "dialogs.settings.proxy.name")
    public boolean enableProxy = false;

    @Config.Dropdown(id = "proxy.type", translationKey = "dialogs.settings.proxy.type", category = "dialogs.settings.proxy.name", values = ProxyTypeProvider.class)
    public String proxyType = Proxy.Type.HTTP.name();

    @Config.Text(id = "proxy.address", translationKey = "dialogs.settings.proxy.address", category = "dialogs.settings.proxy.name")
    public String proxyAddress = "";

    @Config.Text(id = "proxy.username", translationKey = "dialogs.settings.proxy.username", category = "dialogs.settings.proxy.name")
    public String proxyUsername = "";

    @Config.Text(id = "proxy.password", translationKey = "dialogs.settings.proxy.password", category = "dialogs.settings.proxy.name")
    public String proxyPassword = "";

    @Config.CheckBox(id = "proxy.trustall", translationKey = "dialogs.settings.proxy.trust_all_certificates", category = "dialogs.settings.proxy.name")
    public boolean proxyTrustAll = false;

    @Config.CheckBox(id = "ui.settings.logging.enablelogfile", translationKey = "dialogs.settings.logging.enable_file_logging", category = "dialogs.settings.logging.name")
    public boolean enableLogging = true;

    @Config.Numbers(id = "ui.settings.logging.maxkeeplogs", translationKey = "dialogs.settings.logging.max_kept_logs", category = "dialogs.settings.logging.name")
    public int maxKeptLogFiles = 10;

    @Config.CheckBox(id = "user.settings.other.autoplayenabled", translationKey = "dialogs.settings.other.autoplay_enabled", category = "dialogs.settings.other.name")
    public boolean autoplayEnabled = true;

    @Config.Numbers(id = "user.settings.other.crossfadeduration", translationKey = "dialogs.settings.other.crossfade_duration", category = "dialogs.settings.other.name")
    public int crossfadeDuration = 0;

    @Config.CheckBox(id = "user.settings.other.enablenormalization", translationKey = "dialogs.settings.other.enable_normalization", category = "dialogs.settings.other.name")
    public boolean enableNormalization = true;

    @Config.Numbers(id = "user.settings.other.normalizationpregain", translationKey = "dialogs.settings.other.normalization_pregain", category = "dialogs.settings.other.name")
    public int normalizationPregain = 3;

    @Config.Text(id = "user.settings.other.mixersearchkeywords", translationKey = "dialogs.settings.other.mixer_search_keywords", category = "dialogs.settings.other.name")
    public String mixerSearchKeywords = "";

    @Config.CheckBox(id = "user.settings.other.preloadenabled", translationKey = "dialogs.settings.other.preload_enabled", category = "dialogs.settings.other.name")
    public boolean preloadEnabled = true;

    @Config.Numbers(id = "user.settings.other.releaselinedelay", translationKey = "dialogs.settings.other.release_line_delay", category = "dialogs.settings.other.name")
    public int releaseLineDelay = 20;

    @Config.CheckBox(id = "user.settings.other.bypasssinkvolume", translationKey = "dialogs.settings.other.bypass_sink_volume", category = "dialogs.settings.other.name")
    public boolean bypassSinkVolume = false;

    @Config.Text(id = "user.settings.other.preferredlocale", translationKey = "dialogs.settings.other.preferred_locale", category = "dialogs.settings.other.name")
    public String preferredLocale = "en";

    public static class ThemeListProvider implements Config.ConfigValueProvider<String> {
        @Override
        public List<String> values() {
            if (PublicValues.themeLoader == null)
                PublicValues.themeLoader = new ThemeLoader();
            return PublicValues.themeLoader.getThemes();
        }
    }

    public static class LanguageListProvider implements Config.ConfigValueProvider<String> {
        @Override
        public List<String> values() {
            return PublicValues.language.getAvailableLanguages();
        }
    }

    public static class BrowseViewStyleProvider implements Config.ConfigValueProvider<String> {
        @Override
        public List<String> values() {
            return new ArrayList<String>() {{
                add("Metro");
                add("Table");
            }};
        }
    }

    public static class BrowseViewStyleMappingProvider implements Config.ConfigValueProvider<Integer> {
        @Override
        public List<Integer> values() {
            return new ArrayList<Integer>() {{
                add(0);
                add(1);
            }};
        }
    }

    public static class AudioQualityProvider implements Config.ConfigValueProvider<String> {
        @Override
        public List<String> values() {
            return Lists.newArrayList("Normal", "High", "Very High");
        }
    }

    public static class AudioQualityMappingProvider implements Config.ConfigValueProvider<String> {
        @Override
        public List<String> values() {
            return Utils.enumToObjectArray(Quality.values());
        }
    }

    public static class ProxyTypeProvider implements Config.ConfigValueProvider<String> {
        @Override
        public List<String> values() {
            return Arrays.stream(Proxy.Type.values()).map(Enum::name).collect(Collectors.toCollection(ArrayList::new));
        }
    }

    public static class MypalComponentProvider implements Config.CustomComponentCallback {
        @Override
        public JComponent component() {
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            JTextField path = new JTextField();
            panel.add(path, BorderLayout.CENTER);
            JButton choosePath = new JButton(UIManager.getIcon("FileView.directoryIcon"));
            panel.add(choosePath, BorderLayout.EAST);
            return panel;
        }

        @Override
        public void onSave(JComponent component) throws NoSuchFieldException {
            JPanel panel = (JPanel) component;
            JTextField path = (JTextField) panel.getComponent(0);
            PublicValues.config.write("mypalPath", path.getText());
        }
    }

    @Override
    public String translate(String id) {
        return PublicValues.language.translate(id);
    }
}
