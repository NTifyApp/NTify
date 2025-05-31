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
package com.spotifyxp.swingextension;

import com.spotifyxp.panels.ContentPanel;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("BusyWait")
public class JScrollText extends JLabel implements Runnable {
    private Thread t;
    private volatile boolean animate = true;
    private volatile String originalText;

    public JScrollText(String text) {
        super(text);
    }

    @Override
    public void run() {
        long frameTime = 1000 / 2;
        long lastFrameTime = System.currentTimeMillis();
        while (animate) {
            Insets insets = getInsets();
            int labelWidth = getWidth() - insets.left - insets.right;

            FontMetrics fm = getFontMetrics(getFont());
            int stringWidth = fm.stringWidth(originalText);

            if (stringWidth <= labelWidth) {
                animate = false;
                break;
            }

            String oldText = getText();
            String newText = oldText.substring(1) + oldText.charAt(0);
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastFrameTime;
            if (elapsedTime < frameTime) {
                try {
                    Thread.sleep(frameTime - elapsedTime);
                } catch (InterruptedException ignored) {
                }
            }
            lastFrameTime = currentTime;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    realSetText(newText);
                }
            });
        }
    }

    void realSetText(String text) {
        super.setText(text);
    }

    @Override
    public void setText(String text) {
        if (t != null) {
            animate = false;
            try {
                t.join();
            } catch (InterruptedException ignored) {
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                realSetText(text + "  ");
            }
        });
        originalText = text;
        animate = true;
        t = new Thread(this);
        if(ContentPanel.frame.isVisible()) t.start();
    }
}