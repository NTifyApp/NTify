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
    @Config.CustomComponent(id = "settings.mypal.path", category = "ui.settings.browser", component = MypalComponentProvider.class)
    public String mypalPath = "";

    @Config.Dropdown(id = "settings.ui.theme", category = "ui.settings.ui.label", values = ThemeListProvider.class)
    public String theme = "DarkGreen";

    @Config.Dropdown(id = "user.settings.language", category = "ui.settings.ui.label", values = LanguageListProvider.class)
    public String language = "English";

    @Config.Dropdown(id = "user.settings.browse_view_style", category = "ui.settings.ui.label", values = BrowseViewStyleProvider.class, mapping = BrowseViewStyleMappingProvider.class)
    public int browseViewStyle = 0;

    @Config.CheckBox(id = "general.exception.visibility", category = "ui.settings.ui.label")
    public boolean hideExceptions = false;

    @Config.CheckBox(id = "user.settings.cache.disabled", category = "ui.settings.playback.label")
    public boolean cacheDisabled = false;

    @Config.Dropdown(id = "settings.playback.quality", category = "ui.settings.playback.label", values = AudioQualityProvider.class, mapping = AudioQualityMappingProvider.class)
    public String audioQuality = "NORMAL";

    @Config.CheckBox(id = "user.settings.autoqueue.disabled", category = "ui.settings.playback.label")
    public boolean disableAutoQueue = false;

    @Config.CheckBox(id = "proxy.enable", category = "ui.settings.proxy")
    public boolean enableProxy = false;

    @Config.Dropdown(id = "proxy.type", category = "ui.settings.proxy", values = ProxyTypeProvider.class)
    public String proxyType = Proxy.Type.HTTP.name();

    @Config.Text(id = "proxy.address", category = "ui.settings.proxy")
    public String proxyAddress = "";

    @Config.Text(id = "proxy.username", category = "ui.settings.proxy")
    public String proxyUsername = "";

    @Config.Text(id = "proxy.password", category = "ui.settings.proxy")
    public String proxyPassword = "";

    @Config.CheckBox(id = "proxy.trustall", category = "ui.settings.proxy")
    public boolean proxyTrustAll = false;

    @Config.CheckBox(id = "ui.settings.logging.enablelogfile", category = "ui.settings.logging.label")
    public boolean enableLogging = true;

    @Config.Numbers(id = "ui.settings.logging.maxkeeplogs", category = "ui.settings.logging.label")
    public int maxKeptLogFiles = 10;

    @Config.CheckBox(id = "user.settings.other.autoplayenabled", category = "ui.settings.other")
    public boolean autoplayEnabled = true;

    @Config.Numbers(id = "user.settings.other.crossfadeduration", category = "ui.settings.other")
    public int crossfadeDuration = 0;

    @Config.CheckBox(id = "user.settings.other.enablenormalization", category = "ui.settings.other")
    public boolean enableNormalization = true;

    @Config.Numbers(id = "user.settings.other.normalizationpregain", category = "ui.settings.other")
    public int normalizationPregain = 3;

    @Config.Text(id = "user.settings.other.mixersearchkeywords", category = "ui.settings.other")
    public String mixerSearchKeywords = "";

    @Config.CheckBox(id = "user.settings.other.preloadenabled", category = "ui.settings.other")
    public boolean preloadEnabled = true;

    @Config.Numbers(id = "user.settings.other.releaselinedelay", category = "ui.settings.other")
    public int releaseLineDelay = 20;

    @Config.CheckBox(id = "user.settings.other.bypasssinkvolume", category = "ui.settings.other")
    public boolean bypassSinkVolume = false;

    @Config.Text(id = "user.settings.other.preferredlocale", category = "ui.settings.other")
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
