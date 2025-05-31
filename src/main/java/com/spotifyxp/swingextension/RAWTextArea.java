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

import com.spotifyxp.PublicValues;
import com.spotifyxp.utils.ClipboardUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;

public class RAWTextArea extends JPanel {
    public static class ColoredLine {
        private Color textColor;
        private final String textContent;

        public ColoredLine(Color textColor, String textContent) {
            this.textColor = textColor;
            this.textContent = textContent;
        }

        public Color getTextColor() {
            return this.textColor;
        }

        public String getTextContent() {
            return this.textContent;
        }
    }

    public enum ColoredModifyLocations {
        HEAD,
        TAIL
    }

    private String content = "";
    private boolean colorMode = false;
    private final ArrayList<ColoredLine> coloredLines = new ArrayList<>();

    public void copyText() {
        ClipboardUtil.set(content);
    }

    public void append(String text) {
        content = content + text;
        repaint();
    }

    public void append(String text, Color color) {
        coloredLines.add(new ColoredLine(color, text));
        repaint();
    }

    public void setColorModeActive() {
        colorMode = true;
    }

    public void setColorModeDisabled() {
        colorMode = false;
    }

    public void modifyColorAt(ColoredModifyLocations location, Color changeTo) {
        switch (location) {
            case HEAD:
                ColoredLine head = coloredLines.get(0);
                coloredLines.remove(0);
                head.textColor = changeTo;
                coloredLines.add(0, head);
                repaint();
                break;
            case TAIL:
                ColoredLine tail = coloredLines.get(coloredLines.size() - 1);
                coloredLines.remove(coloredLines.size() - 1);
                tail.textColor = changeTo;
                coloredLines.add(tail);
                repaint();
                break;
        }
    }

    public void remove(ColoredModifyLocations location) {
        switch (location) {
            case HEAD:
                coloredLines.remove(0);
                repaint();
                break;
            case TAIL:
                coloredLines.remove(coloredLines.size() - 1);
                repaint();
                break;
        }
    }

    public void setText(String text) {
        content = text;
        repaint();
    }

    public String getText() {
        return content;
    }

    boolean skipPaint = false;

    public void dontPaint() {
        skipPaint = true;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (skipPaint) return;
        if (!colorMode) {
            g.setColor(PublicValues.globalFontColor);
            Rectangle2D r = g.getFontMetrics().getStringBounds(content, g);
            int ycache = getFontMetrics(getFont()).getHeight() + 5;
            ArrayList<String> toDraw = new ArrayList<>(Arrays.asList(content.split("\n")));
            for (String s : toDraw) {
                g.drawString(s, 5, ycache);
                ycache += getFontMetrics(getFont()).getHeight() + 5;
            }
            setPreferredSize(new Dimension(getWidth(), ycache + 10));
            return;
        }
        int ycache = getFontMetrics(getFont()).getHeight() + 5;
        for (ColoredLine line : coloredLines) {
            g.setColor(PublicValues.globalFontColor);
            Rectangle2D r = g.getFontMetrics().getStringBounds(line.textContent, g);
            g.drawString(line.textContent, 5, ycache);
            ycache += getFontMetrics(getFont()).getHeight() + 5;
        }
        setPreferredSize(new Dimension(getWidth(), ycache + 10));
    }
}
