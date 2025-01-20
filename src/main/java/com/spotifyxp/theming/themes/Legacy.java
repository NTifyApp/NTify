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
