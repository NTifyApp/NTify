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
import com.spotifyxp.utils.AsyncUtils;
import com.spotifyxp.utils.GraphicalMessage;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

public class JImagePanel extends JPanel {
    private BufferedImage image;
    private byte[] imageBytes;
    private SVGImageRecalculate recalculate;
    private double rotationRadians = 0;
    private boolean keepAspectRatio = true;

    @FunctionalInterface
    public interface SVGImageRecalculate {
        byte[] svgImageRecalculate();
    }

    public void setKeepAspectRatio(boolean keepAspectRatio) {
        this.keepAspectRatio = keepAspectRatio;
        repaint();
    }

    private void refresh() {
        try {
            if (recalculate != null) {
                byte[] newBytes = recalculate.svgImageRecalculate();
                if (newBytes != null && newBytes.length > 0) {
                    image = ImageIO.read(new ByteArrayInputStream(newBytes));
                }
            } else if (imageBytes != null) {
                image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            }
        } catch (IOException ex) {
            ConsoleLogging.Throwable(ex);
            GraphicalMessage.openException(ex);
        }
        repaint();
    }

    // ---------- Image setters ----------
    public void setImage(String resourcePath) {
        recalculate = null;
        try (InputStream in = Initiator.class.getResourceAsStream(resourcePath)) {
            imageBytes = IOUtils.toByteArray(in);
        } catch (IOException ex) {
            ConsoleLogging.Throwable(ex);
        }
        refresh();
    }

    public void setImage(BufferedImage image) {
        recalculate = null;
        this.image = image;
        repaint();
    }

    public void setImage(File file) {
        recalculate = null;
        try {
            imageBytes = Files.readAllBytes(file.toPath());
        } catch (IOException ex) {
            ConsoleLogging.Throwable(ex);
        }
        refresh();
    }

    public void setImage(byte[] bytes) {
        recalculate = null;
        imageBytes = bytes;
        refresh();
    }

    public void setImage(SVGImageRecalculate recalculate) {
        this.recalculate = recalculate;
        refresh();
    }

    /**
     * Fetches and decodes the image on a background thread so a careless caller on the EDT
     * can't block it on network I/O - the actual field assignment/repaint is marshalled back
     * onto the EDT once decoding finishes.
     */
    public void setImage(URL url) {
        recalculate = null;
        AsyncUtils.run(() -> {
            byte[] bytes;
            try (InputStream in = url.openStream()) {
                bytes = IOUtils.toByteArray(in);
            } catch (IOException ex) {
                ConsoleLogging.Throwable(ex);
                return;
            }
            BufferedImage decoded;
            try {
                decoded = ImageIO.read(new ByteArrayInputStream(bytes));
            } catch (IOException ex) {
                ConsoleLogging.Throwable(ex);
                return;
            }
            SwingUtilities.invokeLater(() -> {
                imageBytes = bytes;
                image = decoded;
                repaint();
            });
        });
    }

    public void setImage(InputStream inputStream) {
        recalculate = null;
        try {
            imageBytes = IOUtils.toByteArray(inputStream);
        } catch (IOException ex) {
            ConsoleLogging.Throwable(ex);
        }
        refresh();
    }

    public void setRotation(int percent) {
        rotationRadians = Math.toRadians((360.0 / 100) * percent);
        repaint();
    }

    public InputStream getImageStream() {
        return (imageBytes == null || imageBytes.length == 0)
                ? null
                : new ByteArrayInputStream(imageBytes);
    }

    private void drawImage(Graphics2D g2d) {
        if (image == null) return;

        int panelWidth = getWidth();
        int panelHeight = getHeight();

        if (keepAspectRatio) {
            int imgWidth = image.getWidth();
            int imgHeight = image.getHeight();
            double imgAspect = (double) imgWidth / imgHeight;
            double panelAspect = (double) panelWidth / panelHeight;

            int drawWidth, drawHeight, xOffset, yOffset;
            if (imgAspect > panelAspect) {
                drawWidth = panelWidth;
                drawHeight = (int) (panelWidth / imgAspect);
                xOffset = 0;
                yOffset = (panelHeight - drawHeight) / 2;
            } else {
                drawHeight = panelHeight;
                drawWidth = (int) (panelHeight * imgAspect);
                xOffset = (panelWidth - drawWidth) / 2;
                yOffset = 0;
            }

            g2d.drawImage(image, xOffset, yOffset, drawWidth, drawHeight, this);
        } else {
            g2d.drawImage(image, 0, 0, panelWidth, panelHeight, this);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image == null && recalculate == null) return;

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (rotationRadians != 0) {
            g2d.rotate(rotationRadians, getWidth() / 2.0, getHeight() / 2.0);
        }

        drawImage(g2d);
        g2d.dispose();
    }
}
