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
package com.spotifyxp.tray;

import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.utils.AsyncActionListener;
import com.spotifyxp.utils.AsyncMouseListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


@SuppressWarnings("CanBeFinal")
public class SystemTrayDialog {
    String toolt = null;
    SystemTray systemTray = null;
    Image image = null;
    TrayIcon trayIcon = null;
    boolean calledadd = false;
    boolean calledopen = false;
    PopupMenu menu;

    public void add(String trayicon, String tooltip) {
        calledadd = true;
        systemTray = SystemTray.getSystemTray();
        image = Toolkit.getDefaultToolkit().getImage(trayicon);
        menu = new PopupMenu("TrayDialog menu");
        toolt = tooltip;
    }

    public void add(ImageIcon trayicon, String tooltip) {
        calledadd = true;
        systemTray = SystemTray.getSystemTray();
        image = trayicon.getImage();
        menu = new PopupMenu("TrayDialog menu");
        toolt = tooltip;
    }

    public void addEntry(String name, ActionListener onclick) {
        if (calledadd) {
            MenuItem action = new MenuItem(name);
            action.addActionListener(new AsyncActionListener(onclick));
            menu.add(action);
        }
    }

    public void open(ActionListener ondoubleclick) {
        if (!calledopen && calledadd) {
            calledopen = true;
            trayIcon = new TrayIcon(image, toolt);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(new AsyncActionListener(ondoubleclick));
            try {
                systemTray.add(trayIcon);
            } catch (AWTException awtException) {
                ConsoleLogging.Throwable(awtException);
            }
        }
    }

    public void open(MouseAdapter adapter) {
        if (!calledopen) {
            calledopen = true;
            trayIcon = new TrayIcon(image, toolt);
            trayIcon.setPopupMenu(menu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addMouseListener(new AsyncMouseListener(adapter));
            try {
                systemTray.add(trayIcon);
            } catch (AWTException awtException) {
                ConsoleLogging.Throwable(awtException);
            }
        }
    }
}
