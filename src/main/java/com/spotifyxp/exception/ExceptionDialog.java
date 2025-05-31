/*
 * Copyright [2023-2025] [Gianluca Beil]
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
package com.spotifyxp.exception;

import com.spotifyxp.PublicValues;
import com.spotifyxp.configuration.ConfigValues;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.panels.SplashPanel;
import com.spotifyxp.swingextension.JFrame;
import com.spotifyxp.utils.ClipboardUtil;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionDialog {
    private final Throwable e;
    private final JTextPane exceptionText;

    public ExceptionDialog(Throwable ex) {
        this.e = ex;
        this.exceptionText = new JTextPane();
        this.exceptionText.setText("[" + this.e.toString() + "]" + " ");
        this.exceptionText.setEditable(false);
    }

    /**
     * Gets a preview string of the exception (used inside the exception counter dialog)
     *
     * @return string preview for the excpetion
     */
    public String getPreview() {
        return exceptionText.getText();
    }

    public static String extractStackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    /**
     * Opens a real exception window
     */
    public void openReal() {
        if (PublicValues.config.getBoolean(ConfigValues.hideExceptions.name)) {
            return;
        }
        if (SplashPanel.frame.isVisible()) {
            SplashPanel.frame.setAlwaysOnTop(false);
        }

        JFrame frame = new JFrame(PublicValues.language.translate("exception.dialog.title"));

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        
        if (!exceptionText.getText().contains(extractStackTrace(e))) {
            exceptionText.setText(extractStackTrace(e));
        }
        
        JLabel exceptionLabel = new JLabel(PublicValues.language.translate("exception.dialog.label"));
        exceptionLabel.setFont(new Font("Tahoma", Font.PLAIN, 17));
        exceptionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        contentPane.add(exceptionLabel, BorderLayout.NORTH);

        JScrollPane exceptionScrollPane = new JScrollPane();
        exceptionScrollPane.setViewportView(exceptionText);
        contentPane.add(exceptionScrollPane, BorderLayout.CENTER);

        ContextMenu menu = new ContextMenu(exceptionText, null, getClass());
        menu.addItem(PublicValues.language.translate("ui.general.copy"), () -> ClipboardUtil.set(exceptionText.getText()));

        JButton exceptionOkButton = new JButton(PublicValues.language.translate("exception.dialog.button.text"));
        contentPane.add(exceptionOkButton, BorderLayout.SOUTH);
        exceptionOkButton.addActionListener(e -> frame.dispose());

        frame.getContentPane().add(contentPane);
        frame.setPreferredSize(new Dimension(600, 439));
        frame.pack();
        frame.setVisible(true);
    }

    public String getAsFormattedText() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(e.toString()).append("] ");
        for (StackTraceElement element : e.getStackTrace()) {
            builder.append(element.toString()).append("\n");
        }
        return builder.toString();
    }
}
