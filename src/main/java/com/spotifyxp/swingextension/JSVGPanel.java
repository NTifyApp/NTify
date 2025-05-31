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

import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.utils.GraphicalMessage;
import com.spotifyxp.utils.Resources;
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
    static String rad = "";
    public boolean isFilled = false;

    static BufferedImage image;

    public interface DrawMethods {
        void draw();

        void setImage(InputStream stream);

        JComponent getJComponent();
    }

    public enum DrawTypes {
        IMAGE,
        SVG
    }

    static DrawTypes type = DrawTypes.SVG; //Default is SVG
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
    }

    static class RealDrawImage extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D graphics2D = (Graphics2D) g;
            if (!(rad.isEmpty())) {
                graphics2D.rotate(Double.parseDouble(rad), (float) this.getWidth() / 2, (float) this.getHeight() / 2);
            }
            g.drawImage(image.getScaledInstance(this.getWidth(), this.getHeight(), Image.SCALE_SMOOTH), this.getWidth() / 8, this.getHeight() / 8, null);
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
                image = ImageIO.read(stream);
                realDrawImage.paintImmediately(realDrawImage.getX(), realDrawImage.getY(), realDrawImage.getWidth(), realDrawImage.getHeight());
                realDrawImage.repaint();
            } catch (IOException ignored) {
            }
        }

        @Override
        public JComponent getJComponent() {
            return realDrawImage;
        }
    }

    public void setImage(InputStream stream) {
        method.setImage(stream);
    }

    public void setImage(String resourcePath) {
        method.setImage(new Resources().readToInputStream(resourcePath));
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
        rad = String.valueOf(((float) 360 / 100 * percent) * 0.01745329252);
        method.getJComponent().repaint();
    }
}
