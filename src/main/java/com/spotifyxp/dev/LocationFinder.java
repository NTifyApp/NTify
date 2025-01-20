package com.spotifyxp.dev;

import com.spotifyxp.PublicValues;
import com.spotifyxp.panels.ContentPanel;

import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LocationFinder {
    static Runnable helloRunnable;
    static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);


    public LocationFinder() {
        PublicValues.locationFinderActive = true;
        ContentPanel.addPaintOverwrite(new ContentPanel.PaintOverwrite() {
            @Override
            public void run(Graphics g) {
                try {
                    g.setColor(Color.CYAN);
                    g.drawString("X:" + PublicValues.contentPanel.getMousePosition().x, PublicValues.contentPanel.getMousePosition().x, PublicValues.contentPanel.getMousePosition().y);
                    g.drawString("Y:" + PublicValues.contentPanel.getMousePosition().y, PublicValues.contentPanel.getMousePosition().x, PublicValues.contentPanel.getMousePosition().y + 10);
                } catch (Exception ignored) {
                }
            }
        });

        helloRunnable = new Runnable() {
            public void run() {
                Rectangle rectangle = ContentPanel.frame.getBounds();
                Point screenlocation = MouseInfo.getPointerInfo().getLocation();
                if (rectangle.contains(screenlocation)) {
                    PublicValues.contentPanel.repaint();
                }
            }
        };
        executor.scheduleAtFixedRate(helloRunnable, 0, 10, TimeUnit.MILLISECONDS);
    }

    public void open() {
        if (PublicValues.locationFinderActive) {
            PublicValues.contentPanel.removePaintOverwrite();
            executor.shutdown();
            helloRunnable = null;
            PublicValues.locationFinderActive = false;
            PublicValues.contentPanel.repaint();
        } else {
            new LocationFinder();
        }
    }
}
