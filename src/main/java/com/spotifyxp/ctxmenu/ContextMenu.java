/*
 * Copyright [2025] [Gianluca Beil]
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
package com.spotifyxp.ctxmenu;

import com.spotifyxp.PublicValues;
import com.spotifyxp.utils.AsyncActionListener;
import com.spotifyxp.utils.AsyncMouseListener;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

@SuppressWarnings("unused")
public class ContextMenu {
    public interface GlobalContextMenuItem {
        Runnable toRun(JComponent component, @Nullable ArrayList<String> uris);
        String name();
        boolean shouldBeAdded(JComponent component, Class<?> containingClass);
        boolean showItem(JComponent component, ArrayList<String> uris);
    }

    final ContextualPopupMenu holder;

    public static class ContextualPopupMenu extends JPopupMenu {
        private ArrayList<GlobalContextMenuItem> globalContextMenuItems = new ArrayList<>();
        private ArrayList<JMenuItem> normalItems = new ArrayList<>();

        public ContextualPopupMenu() {
        }

        @Override
        public JMenuItem add(JMenuItem menuItem) {
            normalItems.add(menuItem);
            return menuItem;
        }

        public void addGlobalContextMenuItem(GlobalContextMenuItem item) {
            globalContextMenuItems.add(item);
        }

        public void removeGlobalContextMenuItem(GlobalContextMenuItem item) {
            globalContextMenuItems.remove(item);
        }

        public void show(JComponent invoker, int x, int y, ArrayList<String> uris) {
            removeAll();
            for(JMenuItem item : normalItems) {
                super.add(item);
            }
            for(int i = 0; i < globalContextMenuItems.size(); i++) {
                GlobalContextMenuItem globalContextMenuItem = globalContextMenuItems.get(i);
                JMenuItem item = new JMenuItem(globalContextMenuItem.name());
                item.addActionListener(new AsyncActionListener(e -> globalContextMenuItem.toRun(invoker, uris).run()));
                if(globalContextMenuItem.showItem(invoker, uris)) super.add(item);
            }
            super.show(invoker, x, y);
        }

        @Override
        public void show(Component invoker, int x, int y) {
            removeAll();
            for(JMenuItem item : normalItems) {
                super.add(item);
            }
            super.show(invoker, x, y);
        }
    }

    public ContextMenu() {
        holder = new ContextualPopupMenu();
    }

    public ContextMenu(JComponent component, @Nullable ArrayList<String> uris, Class<?> containingClass) {
        component.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (SwingUtilities.isRightMouseButton(e)) {
                    if(uris == null) {} else if(uris.isEmpty()) return;
                    holder.show(component, e.getX(), e.getY(), uris);
                }
            }
        }));
        holder = new ContextualPopupMenu();
        for (GlobalContextMenuItem item : PublicValues.globalContextMenuItems) {
            if(item.shouldBeAdded(component, containingClass)) holder.addGlobalContextMenuItem(item);
        }
        PublicValues.contextMenus.add(this);
    }

    public void addItem(String text, Runnable onClick) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(new AsyncActionListener(e -> onClick.run()));
        holder.add(item);
    }

    public void addSeparator() {
        holder.add(new JSeparator());
    }

    /**
     * Show the context menu at a specific location
     * @param invoker - The component to which the location is relative
     * @param x - X location where the context menu should appear
     * @param y - Y location where the context menu should appear
     */
    public void showAt(@Nullable JComponent invoker, int x, int y) {
        if(invoker == null) {
            holder.setLocation(x, y);
            holder.setVisible(true);
        }else{
            holder.show(invoker, x, y);
        }
    }

    /**
     * Show the context menu at a specific location with a specific size
     * @apiNote You have to reset the preferred size manually after invoking this
     * @param invoker - The component to which the location is relative
     * @param x - X location where the context menu should appear
     * @param y - Y location where the context menu should appear
     * @param w - Width of the context menu
     * @param h - Height of the context menu
     */
    public void showAt(@Nullable JComponent invoker, int x, int y, @Nullable Integer w, @Nullable Integer h) {
        Dimension preferredSize = holder.getPreferredSize();
        if(w != null) preferredSize.width = w;
        if(h != null) preferredSize.height = h;
        holder.setPreferredSize(preferredSize);
        showAt(invoker, x, y);
    }
}
