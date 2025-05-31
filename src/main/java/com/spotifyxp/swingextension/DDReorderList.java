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
package com.spotifyxp.swingextension;

import com.spotifyxp.graphics.Graphics;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.utils.SVGUtils;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * This is a JList that can be reordered per dragging
 * @param <T> The object type of the elements in the list
 */
public class DDReorderList<T> extends JList<T> {
    private OnReordered<T> onReordered = (newIndex, oldIndex, name) -> {};

    @FunctionalInterface
    public interface OnReordered<T> {
        void run(int newIndex, int oldIndex, T name);
    }

    public DDReorderList(DefaultListModel<T> model) throws IOException {
        super(model);
        MouseAdapter mouseAdapter = new ReorderListener(this);
        ReorderVisualizerRenderer reorderVisualizerRenderer = new ReorderVisualizerRenderer(this);
        setCellRenderer(reorderVisualizerRenderer);
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    public void setOnReordered(OnReordered<T> onReordered) {
        this.onReordered = onReordered;
    }

    static class ReorderVisualizerRenderer extends DefaultListCellRenderer {
        private final byte[] unselected;
        private final byte[] selected;
        private boolean mouseIsDown = false;
        private boolean wasDragged = false;

        public ReorderVisualizerRenderer(JComponent component) throws IOException {
            selected = IOUtils.toByteArray(SVGUtils.svgToImageInputStreamSameSize(Graphics.MVERTICALSELECTED.getInputStream(), new Dimension(40, 40)));
            unselected = IOUtils.toByteArray(SVGUtils.svgToImageInputStreamSameSize(Graphics.MVERTICAL.getInputStream(), new Dimension(40, 40)));
            component.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    mouseIsDown = true;
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    mouseIsDown = false;
                    wasDragged = false;
                }
            });
            component.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    wasDragged = true;
                }
            });
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            JPanel elementPanel = new JPanel(new BorderLayout());
            JImagePanel imagePanel = new JImagePanel();
            elementPanel.add(imagePanel, BorderLayout.LINE_START);
            if(isSelected && mouseIsDown && wasDragged) {
                c.setForeground(Color.GRAY);
                c.setFont(c.getFont().deriveFont(Font.BOLD));
                imagePanel.setImage(new ByteArrayInputStream(selected));
            } else {
                imagePanel.setImage(new ByteArrayInputStream(unselected));
            }
            elementPanel.add(c, BorderLayout.CENTER);
            return elementPanel;
        }
    }

    // Taken from: https://forums.oracle.com/ords/apexds/post/jlist-drag-and-drop-to-reorder-items-8826
    // Author: darrylburke
    // Date: Feb 7 2009
    class ReorderListener extends MouseAdapter {
        private final JList<T> list;
        private int pressIndex = 0;
        private int releaseIndex = 0;

        public ReorderListener(JList<T> list) {
            if (!(list.getModel() instanceof DefaultListModel)) {
                throw new IllegalArgumentException("List must have a DefaultListModel");
            }
            this.list = list;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            pressIndex = list.locationToIndex(e.getPoint());
        }

        void mouseReleasedManual(MouseEvent e) {
            releaseIndex = list.locationToIndex(e.getPoint());
            if (releaseIndex != pressIndex && releaseIndex != -1) {
                DefaultListModel<T> model = (DefaultListModel<T>) list.getModel();
                T dragee = model.elementAt(pressIndex);
                onReordered.run(getSelectedIndex(), pressIndex, dragee);
                reorder();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            mouseReleasedManual(e);
            pressIndex = releaseIndex;
        }

        private void reorder() {
            try {
                DefaultListModel<T> model = (DefaultListModel<T>) list.getModel();
                T dragee = model.elementAt(pressIndex);
                model.removeElementAt(pressIndex);
                model.insertElementAt(dragee, releaseIndex);
            }catch (ArrayIndexOutOfBoundsException e) {
                ConsoleLogging.warning("ArrayIndexOutOfBoundsException in DDReorderList");
            }
        }
    }
}
