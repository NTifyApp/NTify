/*
 * Copyright [2025] [Gianluca Beil]
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

import com.spotifyxp.Initiator;
import com.spotifyxp.PublicValues;
import com.spotifyxp.lib.libLanguage;
import com.spotifyxp.logging.LogsViewer;
import com.spotifyxp.panels.SplashPanel;
import com.spotifyxp.theming.themes.DarkGreen;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class LogViewer implements Argument {
    @Override
    public Runnable runArgument(String commands) {
        return new Runnable() {
            @Override
            public void run() {
                new DarkGreen().initTheme();
                PublicValues.language = new libLanguage(Initiator.class);
                PublicValues.language.setLanguageFolder("lang");
                PublicValues.language.setNoAutoFindLanguage("en");
                LogsViewer viewer = new LogsViewer();
                viewer.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                viewer.open();
                while(true) {}
            }
        };
    }

    @Override
    public String getName() {
        return "open-logviewer";
    }

    @Override
    public String getDescription() {
        return "Opens the log viewer (With ansi support)";
    }

    @Override
    public boolean hasParameter() {
        return false;
    }
}
