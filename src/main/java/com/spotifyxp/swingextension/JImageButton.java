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
import com.spotifyxp.utils.AsyncActionListener;
import com.spotifyxp.utils.Resources;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.svg.SVGDocument;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;

public class JImageButton extends JSVGCanvas {
    //This will create a new type of JButton that is not really a JButton
    //but looks like one and acts like one but with SVG support
    final JSVGCanvas canvas = this;
    boolean highlight = false;
    boolean click = false;
    AsyncActionListener l;

    public JImageButton() {
        JImageButton button = this;
        canvas.setBackground(getBackground());
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                highlight = false;
                refreshPaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                highlight = true;
                refreshPaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                click = true;
                refreshPaint();
                l.actionPerformed(null);
                button.getMouseListeners()[0].mouseClicked(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                click = false;
                refreshPaint();
            }
        });
        setBorderPainted(true);
        setFocusPainted(true);
        setContentAreaFilled(true);
    }

    final boolean first = false;

    public void setImage(InputStream inputStream) {
        try {
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
            SVGDocument document = factory.createSVGDocument("", inputStream);
            canvas.setSVGDocument(document);
            if (first) {
                canvas.setBounds(canvas.getX(), canvas.getY(), canvas.getWidth() / 2, canvas.getHeight() / 2);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setImage(String resourcePath) {
        setImage(new Resources().readToInputStream(resourcePath));
    }

    Color oldColor = ContentPanel.frame.getBackground();

    public void addActionListener(AsyncActionListener listener) {
        l = listener;
    }

    boolean paintBorder = true;
    boolean areaFilled = true;
    boolean focusPainted = true;

    public void setBorderPainted(boolean value) {
        paintBorder = value;
        if (value) {
            repaint();
        } else {
            canvas.setBorder(BorderFactory.createEmptyBorder());
            repaint();
        }
    }

    public void setColor(Color c) {
        oldColor = c;
    }

    public void setContentAreaFilled(boolean value) {
        areaFilled = value;
        if (!value) {
            setBackground(oldColor);
        }
    }

    public void setFocusPainted(boolean value) {
        focusPainted = value;
    }

    public void refreshPaint() {
        if (click) {
            setBackground(getBackground().brighter().brighter());
        } else {
            setBackground(oldColor);
        }
        if (focusPainted) {
            if (highlight) {
                setBackground(getBackground().brighter());
            } else {
                setBackground(oldColor);
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (paintBorder) {
            g.setColor(oldColor.brighter());
            g.drawRoundRect(getX(), getY(), getWidth(), getHeight(), 15, 15);
        }
    }
}
