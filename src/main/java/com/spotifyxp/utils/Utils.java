package com.spotifyxp.utils;

import com.spotifyxp.PublicValues;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.swingextension.JFrame;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class Utils {
    @SuppressWarnings("rawtypes")
    public static String getClassName(Class c) {
        return c.getName().split("\\.")[c.getName().split("\\.").length - 1];
    }

    public static int getDisplayNumber(JFrame frame) {
        // Get the graphics configuration of the JFrame
        GraphicsConfiguration config = frame.getGraphicsConfiguration();

        // Get an array of all screen devices
        GraphicsDevice[] allScreenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

        // Iterate through all screen devices to find the display number
        for (int i = 0; i < allScreenDevices.length; i++) {
            if (config.getDevice() == allScreenDevices[i]) {
                // Return the display number if the device matches
                if (PublicValues.debug) ConsoleLogging.debug("Returning screen number: " + (i + 1));
                return i + 1; // Display numbers are usually 1-based
            }
        }

        return -1;
    }

    public static void moveToScreen(Window window, int targetDisplayNumber) {
        // Get an array of all screen devices
        GraphicsDevice[] allScreenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

        if (targetDisplayNumber >= 1 && targetDisplayNumber <= allScreenDevices.length) {
            // Get the GraphicsConfiguration of the target screen
            GraphicsConfiguration targetConfig = allScreenDevices[targetDisplayNumber - 1].getDefaultConfiguration();

            // Calculate the center position on the target screen
            int x = (ContentPanel.frame.getSize().width - window.getWidth()) / 2;
            int y = (ContentPanel.frame.getSize().height - window.getHeight()) / 2;


            // Set the JFrame location to the center of the target screen
            window.setLocation(targetConfig.getBounds().x + x, targetConfig.getBounds().y + y);

            if (PublicValues.debug) ConsoleLogging.debug("Moving JFrame to screen number: " + targetDisplayNumber);
        } else {
            ConsoleLogging.warning("Can't move window to the right screen");
        }
    }

    public static int getDefaultScreenNumber() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        // Get the default screen device
        GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();

        // Get all screen devices
        GraphicsDevice[] screenDevices = ge.getScreenDevices();

        // Find the screen number (index) of the default screen
        int defaultScreenNumber;

        for (int i = 0; i < screenDevices.length; i++) {
            if (screenDevices[i].equals(defaultScreen)) {
                defaultScreenNumber = i;
                return defaultScreenNumber;
            }
        }
        return -1;
    }

    public static ArrayList<Object> enumToObjectArray(Enum<?>[] enums) {
        ArrayList<Object> objects = new ArrayList<>();
        for (Enum<?> e : enums) {
            objects.add(e.name());
        }
        return objects;
    }

    public static boolean checkOrLockFile() throws IOException {
        if(!new File(PublicValues.fileslocation, "lockfile").exists()) {
            new File(PublicValues.fileslocation, "lockfile").createNewFile();
        }
        FileChannel channel = FileChannel.open(
                new File(PublicValues.fileslocation, "lockfile").toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
        );
        FileLock lock = channel.tryLock();
        return lock == null;
    }

    public static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if(dot != -1) { version = version.substring(0, dot); }
        } return Integer.parseInt(version);
    }
}
