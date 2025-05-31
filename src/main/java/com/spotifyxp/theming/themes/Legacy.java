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

import com.spotifyxp.PublicValues;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.theming.Theme;

import javax.swing.*;
import java.awt.*;

public class Legacy implements Theme {
    @Override
    public String getAuthor() {
        return "Werwolf2303";
    }

    @Override
    public boolean isLight() {
        return true;
    }

    @Override
    public void initTheme() {
        PublicValues.borderColor = Color.gray;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (RuntimeException | UnsupportedLookAndFeelException | ClassNotFoundException |
                 InstantiationException | IllegalAccessException e) {
            ConsoleLogging.Throwable(e);
        }
        Events.subscribe(SpotifyXPEvents.onFrameReady.getName(), (Object... data) -> {
            ContentPanel.legacySwitch.setBackgroundAt(0, Color.white);
            ContentPanel.legacySwitch.setBackgroundAt(1, Color.white);
            ContentPanel.legacySwitch.setBackgroundAt(2, Color.white);
            ContentPanel.legacySwitch.setBackgroundAt(3, Color.white);
            ContentPanel.legacySwitch.setBackgroundAt(4, Color.white);
            ContentPanel.legacySwitch.setBackgroundAt(5, Color.white);
            ContentPanel.legacySwitch.setBackgroundAt(6, Color.white);
        });
    }
}
