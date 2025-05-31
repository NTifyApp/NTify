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
package com.spotifyxp.theming;

import com.spotifyxp.PublicValues;
import com.spotifyxp.lib.libDetect;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.theming.themes.*;
import com.spotifyxp.utils.Utils;

import java.util.ArrayList;

public class ThemeLoader {
    public static Theme loadedTheme = null;
    static String loadedThemePath = "";
    private static final ArrayList<Theme> availableThemes = new ArrayList<>();

    public ThemeLoader() {
        availableThemes.add(new DarkGreen());
        availableThemes.add(new Legacy());
        if (!(PublicValues.osType == libDetect.OSType.MacOS)) {
            //Unsupported byte count: 16
            availableThemes.add(new MacOS());
        }
        availableThemes.add(new Ugly());
        availableThemes.add(new CustomTheme());
    }

    public static boolean hasTheme(String name) {
        for (Theme t : availableThemes) {
            if (Utils.getClassName(t.getClass()).equals(name)) {
                return true;
            }
        }
        return false;
    }

    public void tryLoadTheme(String name) throws UnknownThemeException {
        boolean found = false;
        for (Theme theme : availableThemes) {
            if (Utils.getClassName(theme.getClass()).toLowerCase().contains(name.toLowerCase())) {
                executeTheme(theme);
                PublicValues.theme = theme;
                found = true;
            }
        }
        if (!found) {
            throw new UnknownThemeException(name);
        }
    }

    public void loadTheme(String name) throws UnknownThemeException {
        boolean found = false;
        for (Theme theme : availableThemes) {
            if (Utils.getClassName(theme.getClass()).equals(name)) {
                executeTheme(theme);
                PublicValues.theme = theme;
                found = true;
            }
        }
        if (!found) {
            throw new UnknownThemeException(name);
        }
    }

    public static ArrayList<Theme> getAvailableThemes() {
        return availableThemes;
    }

    public static void addTheme(Theme theme) {
        availableThemes.add(theme);
    }

    void executeTheme(Theme theme) {
        theme.initTheme();
        ConsoleLogging.info("Loaded Theme => " + Utils.getClassName(theme.getClass()) + " from => " + theme.getAuthor());
    }

    public ArrayList<String> getThemes() {
        ArrayList<String> themes = new ArrayList<>();
        for (Theme theme : availableThemes) {
            themes.add(Utils.getClassName(theme.getClass()));
        }
        return themes;
    }

    public ArrayList<String> getThemesAsSetting() {
        ArrayList<String> themes = new ArrayList<>();
        for (Theme theme : availableThemes) {
            themes.add(Utils.getClassName(theme.getClass()) + " from " + theme.getAuthor());
        }
        return themes;
    }

    public static class UnknownThemeException extends Exception {
        public UnknownThemeException(String themeName) {
            super(themeName);
        }
    }
}
