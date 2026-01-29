/*
 * Copyright [2025-2026] [Gianluca Beil]
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
import com.spotifyxp.theming.themes.DarkGreen;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
                viewer.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                viewer.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        System.exit(0);
                    }
                });
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
