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

import com.spotifyxp.Initiator;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.utils.GraphicalMessage;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.svg.SVGDocument;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ConcurrentModificationException;

public class JSVGPanel {
    public boolean isFilled = false;

    public interface DrawMethods {
        void draw();

        void setImage(InputStream stream);

        JComponent getJComponent();

        void setRotation(String rad);
    }

    public enum DrawTypes {
        IMAGE,
        SVG
    }

    DrawTypes type = DrawTypes.SVG; //Default is SVG
    DrawMethods method = new DrawSVG(); //Default is SVG

    public static class DrawSVG extends JSVGCanvas implements DrawMethods {
        @Override
        public void draw() {
        }

        @Override
        public JComponent getJComponent() {
            return this;
        }

        @Override
        public void setImage(InputStream stream) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        String parser = XMLResourceDescriptor.getXMLParserClassName();
                        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
                        SVGDocument document = factory.createSVGDocument("", stream);
                        setSVGDocument(document);
                    } catch (IOException e) {
                        GraphicalMessage.openException(e);
                        ConsoleLogging.Throwable(e);
                    } catch (ConcurrentModificationException e) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                setImage(stream);
                            }
                        });
                    }
                }
            });
        }

        @Override
        public void setRotation(String rad) {
            //SVG mode doesn't support rotation.
        }
    }

    static class RealDrawImage extends JPanel {
        BufferedImage image;
        String rad = "";

        //Cache of image scaled to the panel's current size - getScaledInstance(SCALE_SMOOTH) is
        //the slowest software scaling path, so only recompute it when the source image or the
        //panel's size actually changes instead of on every repaint.
        private Image scaledImage;
        private int scaledForWidth = -1;
        private int scaledForHeight = -1;
        private BufferedImage scaledFromImage;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D graphics2D = (Graphics2D) g;
            if (!(rad.isEmpty())) {
                graphics2D.rotate(Double.parseDouble(rad), (float) this.getWidth() / 2, (float) this.getHeight() / 2);
            }
            if (image != null) {
                if (scaledImage == null || scaledFromImage != image || scaledForWidth != getWidth() || scaledForHeight != getHeight()) {
                    scaledImage = image.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH);
                    scaledFromImage = image;
                    scaledForWidth = getWidth();
                    scaledForHeight = getHeight();
                }
                g.drawImage(scaledImage, this.getWidth() / 8, this.getHeight() / 8, null);
            }
        }
    }

    public static class DrawImage implements DrawMethods {
        final RealDrawImage realDrawImage = new RealDrawImage();

        @Override
        public void draw() {
            realDrawImage.repaint();
        }

        @Override
        public void setImage(InputStream stream) {
            try {
                realDrawImage.image = ImageIO.read(stream);
                realDrawImage.paintImmediately(realDrawImage.getX(), realDrawImage.getY(), realDrawImage.getWidth(), realDrawImage.getHeight());
                realDrawImage.repaint();
            } catch (IOException ignored) {
            }
        }

        @Override
        public JComponent getJComponent() {
            return realDrawImage;
        }

        @Override
        public void setRotation(String rad) {
            realDrawImage.rad = rad;
        }
    }

    public void setImage(InputStream stream) {
        method.setImage(stream);
    }

    public void setImage(String resourcePath) {
        method.setImage(Initiator.class.getResourceAsStream(resourcePath));
    }

    public JComponent getJComponent() {
        return method.getJComponent();
    }

    public JSVGPanel() {
        method.getJComponent().setBackground(ContentPanel.frame.getBackground());
    }

    public void setSVG(boolean value) {
        if (value) {
            type = DrawTypes.SVG;
            method = new DrawSVG();
        } else {
            type = DrawTypes.IMAGE;
            method = new DrawImage();
        }
    }

    public void setRotation(int percent) {
        method.setRotation(String.valueOf(((float) 360 / 100 * percent) * 0.01745329252));
        method.getJComponent().repaint();
    }
}
