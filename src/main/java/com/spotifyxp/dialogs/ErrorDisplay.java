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
package com.spotifyxp.dialogs;

import com.spotifyxp.PublicValues;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.exception.ExceptionDialog;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.swingextension.JFrame;
import com.spotifyxp.utils.AsyncActionListener;
import com.spotifyxp.utils.AsyncMouseListener;
import com.spotifyxp.utils.ClipboardUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class ErrorDisplay extends JFrame {
    public static ArrayList<ExceptionDialog> errorQueue;
    public static ErrorDisplayPanel errorDisplayPanel;
    public static JScrollPane errorDisplayScrollPane;
    public static DefTable errorDisplayTable;
    public static ContextMenu errorDisplayContextMenu;
    public static JButton removeButton;

    public ErrorDisplay() {
        errorDisplayPanel = new ErrorDisplayPanel();

        errorDisplayTable = new DefTable();
        errorDisplayTable.setModel(new DefaultTableModel(new Object[][]{}, new String[]{""}));
        errorDisplayTable.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (SwingUtilities.isRightMouseButton(e)) return;
                if (e.getClickCount() == 2) {
                    if(errorDisplayTable.getSelectedRow() == -1) return;
                    errorQueue.get(errorDisplayTable.getSelectedRow()).openReal();
                }
            }
        }));

        errorDisplayScrollPane = new JScrollPane();
        errorDisplayScrollPane.setViewportView(errorDisplayTable);

        errorDisplayContextMenu = new ContextMenu(errorDisplayTable, null, getClass());
        errorDisplayContextMenu.addItem(PublicValues.language.translate("ui.general.copy"), () -> ClipboardUtil.set(errorQueue.get(errorDisplayTable.getSelectedRow()).getAsFormattedText()));
        errorDisplayContextMenu.addItem(PublicValues.language.translate("ui.general.remove"), () -> {
            errorQueue.remove(errorDisplayTable.getSelectedRow());
            errorDisplayPanel.setText(String.valueOf(errorQueue.size()));
            ((DefaultTableModel) errorDisplayTable.getModel()).removeRow(errorDisplayTable.getSelectedRow());
            if (errorDisplayTable.getModel().getRowCount() == 0) {
                errorDisplayPanel.setVisible(false);
            }
        });

        removeButton = new JButton(PublicValues.language.translate("ui.errorqueue.clear"));
        removeButton.addActionListener(new AsyncActionListener(e1 -> {
            errorQueue.clear();
            ((DefaultTableModel) errorDisplayTable.getModel()).setRowCount(0);
            errorDisplayPanel.setVisible(false);
        }));
    }

    public void open() {
        ((DefaultTableModel) errorDisplayTable.getModel()).setRowCount(0);
        for (ExceptionDialog exd : errorQueue) {
            ((DefaultTableModel) errorDisplayTable.getModel()).addRow(new Object[]{exd.getPreview()});
        }
        setTitle(PublicValues.language.translate("ui.errorqueue.title"));
        add(removeButton, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(ContentPanel.frame.getWidth() / 2, ContentPanel.frame.getHeight() / 2));
        add(errorDisplayTable, BorderLayout.CENTER);
        pack();
        setVisible(true);
    }

    public static class ErrorDisplayPanel extends JButton {
        public ErrorDisplayPanel() {
            setText("Default");
            errorQueue = new ArrayList<ExceptionDialog>() {
                @Override
                public boolean add(ExceptionDialog exceptionDialog) {
                    super.add(exceptionDialog);
                    setText(String.valueOf(errorQueue.size()));
                    return true;
                }
            };
            addActionListener(new AsyncActionListener(e -> ContentPanel.errorDisplay.open()));
            setVisible(false);
            setBackground(Color.decode("#BB0000"));
            setBounds(5, 5, 100, 40);
        }

        @Override
        public void setText(String text) {
            if (text.equals("Default")) {
                text = PublicValues.language.translate("ui.errorqueue.button");
                super.setText(text);
                return;
            }
            if (!text.contains(PublicValues.language.translate("ui.errorqueue.button"))) {
                text = PublicValues.language.translate("ui.errorqueue.button") + " " + text;
            }
            setVisible(true);
            if (text.equals(String.valueOf(0))) {
                try {
                    setVisible(false);
                } catch (NullPointerException e) {
                    throw new RuntimeException(e);
                }
            }
            super.setText(text);
            setBounds(10, 10, getWidth(), getHeight());
        }
    }

    public JButton getDisplayPanel() {
        return errorDisplayPanel;
    }
}
