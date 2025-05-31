/*
 * Copyright [2024-2025] [Gianluca Beil]
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
package com.spotifyxp.support;

import com.spotifyxp.Initiator;
import com.spotifyxp.PublicValues;
import com.spotifyxp.utils.ApplicationUtils;

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
            PublicValues.fileslocation = System.getProperty("user.home") + "/Library/Application Support/" + ApplicationUtils.getName();
            PublicValues.appLocation = PublicValues.fileslocation;
            PublicValues.configfilepath = PublicValues.fileslocation + "/config.json";
            PublicValues.tempPath = System.getProperty("java.io.tmpdir");
        }
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", ApplicationUtils.getName());
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
