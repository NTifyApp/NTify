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

import com.formdev.flatlaf.FlatDarkLaf;
import com.spotifyxp.PublicValues;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.theming.Theme;

import javax.swing.*;
import java.awt.*;

public class DarkGreen implements Theme {
    @Override
    public String getAuthor() {
        return "Werwolf2303";
    }

    @Override
    public boolean isLight() {
        return false;
    }

    @Override
    public void initTheme() {
        ContentPanel.frame.setBackground(Color.getColor("#3c3f41"));
        ContentPanel.legacySwitch.setBackground(new Color(63, 63, 63));
        PublicValues.borderColor = Color.black;
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException e) {
            ConsoleLogging.Throwable(e);
        }
        PublicValues.globalFontColor = Color.green;
    }
}
