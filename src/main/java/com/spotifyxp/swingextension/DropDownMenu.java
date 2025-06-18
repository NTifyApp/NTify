/*
 * Copyright [2023-2024] [Gianluca Beil]
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
package com.spotifyxp.swingextension;

import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.utils.AsyncActionListener;
import com.spotifyxp.utils.AsyncMouseListener;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@Deprecated
public class DropDownMenu {
    final JPopupMenu popupMenu = new JPopupMenu();

    int xcache = 0;
    int ycache = 0;

    public DropDownMenu(JImagePanel panel, boolean animate) {
        popupMenu.setLightWeightPopupEnabled(true);
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                panel.setRotation(0);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {

            }
        });
        panel.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseClicked(e);
                if (animate) {
                    if (!popupMenu.isVisible()) {
                        ycache = e.getY();
                        xcache = e.getX();
                        panel.setRotation(10);
                        popupMenu.show(ContentPanel.frame, panel.getX(), panel.getY() + panel.getHeight() * 3 - 5);
                    }
                } else {
                    if (!popupMenu.isVisible()) {
                        ycache = e.getY();
                        xcache = e.getX();
                        popupMenu.show(ContentPanel.frame, panel.getX(), panel.getY() + panel.getHeight() * 3 - 5);
                    }
                }
            }
        }));
    }

    public DropDownMenu(JSVGPanel panel, boolean animate) {
        popupMenu.setLightWeightPopupEnabled(true);
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                panel.setRotation(0);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {

            }
        });
        panel.getJComponent().addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseClicked(e);
                if (animate) {
                    if (!popupMenu.isVisible()) {
                        ycache = e.getY();
                        xcache = e.getX();
                        panel.setRotation(10);
                        popupMenu.show(ContentPanel.frame, panel.getJComponent().getX(), panel.getJComponent().getY() + panel.getJComponent().getHeight() * 3 - 5);
                    }
                } else {
                    if (!popupMenu.isVisible()) {
                        ycache = e.getY();
                        xcache = e.getX();
                        popupMenu.show(ContentPanel.frame, panel.getJComponent().getX(), panel.getJComponent().getY() + panel.getJComponent().getHeight() * 3 - 5);
                    }
                }
            }
        }));
    }

    public void addItem(String text, Runnable onClick) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(new AsyncActionListener(e -> onClick.run()));
        popupMenu.add(item);
    }
}
