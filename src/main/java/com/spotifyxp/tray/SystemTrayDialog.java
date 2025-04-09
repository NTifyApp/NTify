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
