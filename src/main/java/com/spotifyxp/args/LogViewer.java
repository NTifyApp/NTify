package com.spotifyxp.args;

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
                PublicValues.language = new libLanguage();
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
