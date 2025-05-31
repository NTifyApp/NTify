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
package com.spotifyxp.theming.themes;

import ch.randelshofer.quaqua.QuaquaLookAndFeel;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.theming.Theme;
import com.spotifyxp.utils.Utils;

import javax.swing.*;

public class MacOS implements Theme {
    public MacOS() {
    }

    public String getAuthor() {
        return "Werwolf2303";
    }

    public boolean isLight() {
        return true;
    }

    public void initTheme() {

        if(Utils.getJavaVersion() > 8) {
            ConsoleLogging.error("Quaqua doesn't support Java version 9 or higher");
            return;
        }

        try {
            UIManager.setLookAndFeel(new QuaquaLookAndFeel());
        } catch (UnsupportedLookAndFeelException var2) {
            ConsoleLogging.Throwable(var2);
        }

    }
}
