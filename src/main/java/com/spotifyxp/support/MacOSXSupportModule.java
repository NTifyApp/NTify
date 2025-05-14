package com.spotifyxp.support;

import com.spotifyxp.Initiator;
import com.spotifyxp.PublicValues;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

public class MacOSXSupportModule implements SupportModule {
    @Override
    public String getOSName() {
        return "Mac OS X";
    }

    @Override
    public void run() {
        if (!PublicValues.customSaveDir) {
            PublicValues.fileslocation = System.getProperty("user.home") + "/Library/Application Support/SpotifyXP";
            PublicValues.appLocation = PublicValues.fileslocation;
            PublicValues.configfilepath = PublicValues.fileslocation + "/config.json";
            PublicValues.tempPath = System.getProperty("java.io.tmpdir");
        }
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "SpotifyXP");
        try {
            Class<?> util = Class.forName("com.apple.eawt.Application");
            Method getApplication = util.getMethod("getApplication", new Class[0]);
            Object application = getApplication.invoke(util);
            Class[] params = new Class[1];
            params[0] = Image.class;
            Method setDockIconImage = util.getMethod("setDockIconImage", params);
            URL url = Initiator.class.getClassLoader().getResource("ntify.png");
            Image image = Toolkit.getDefaultToolkit().getImage(url);
            setDockIconImage.invoke(application, image);
        } catch (Exception ignored) {
            try {
                Class<?> util = Class.forName("java.awt.Taskbar");
                Method getApplication = util.getMethod("getTaskbar", new Class[0]);
                Object application = getApplication.invoke(util);
                Class[] params = new Class[1];
                params[0] = Image.class;
                Method setDockIconImage = util.getMethod("setIconImage", params);
                URL url = Initiator.class.getClassLoader().getResource("ntify.png");
                Image image = Toolkit.getDefaultToolkit().getImage(url);
                setDockIconImage.invoke(application, image);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                     InvocationTargetException e) {
            }
        }
    }
}
