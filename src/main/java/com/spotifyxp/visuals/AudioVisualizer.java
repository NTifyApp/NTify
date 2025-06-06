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
package com.spotifyxp.visuals;

import com.spotifyxp.utils.ApplicationUtils;
import com.spotifyxp.utils.SpectrumAnalyzer;
import javax.swing.*;
import com.spotifyxp.swingextension.JFrame;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class AudioVisualizer extends JPanel {
    private byte[] currentBuffer;
    private final SpectrumAnalyzer analyzer = new SpectrumAnalyzer();
    private final List<Color> palette = new ArrayList<>();
    private JFrame frame;
    private BufferedImage bufferImage;
    private final Timer renderTimer;

    public AudioVisualizer() {
        initColorPalette();
        renderTimer = new Timer(1000 / 75, e -> {
            resizeBufferImageIfNeeded();
            renderToBuffer();
            repaint();
        });
    }

    private void initColorPalette() {
        palette.clear();
        for (int i = 0; i < 64; i++) {
            float hue = i / 64f;
            int rgb = Color.HSBtoRGB(hue, 1f, 1f);
            String hex = String.format("#%06X", (0xFFFFFF & rgb));
            palette.add(Color.decode(hex));
        }
    }

    public void setBuffer(byte[] audioData) {
        this.currentBuffer = audioData;
    }

    public void open() {
        if (frame == null) {
            frame = new JFrame(ApplicationUtils.getName() + " Audio Visualizer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.add(this, BorderLayout.CENTER);
            frame.setSize(300, 300);
            frame.setLocationRelativeTo(null);
        }
        frame.setVisible(true);
        renderTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bufferImage != null) {
            g.drawImage(bufferImage, 0, 0, null);
        }
    }

    private void resizeBufferImageIfNeeded() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;
        if (bufferImage == null || bufferImage.getWidth() != width || bufferImage.getHeight() != height) {
            bufferImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
    }

    private void renderToBuffer() {
        if (currentBuffer == null || bufferImage == null) return;
        Graphics2D g = bufferImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = bufferImage.getWidth();
        int height = bufferImage.getHeight();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        double[][] spectrum = analyzer.analyzeStereoAudio(currentBuffer, false);
        drawCircularSpectrum(g, spectrum, width, height);
        g.dispose();
    }

    private void drawCircularSpectrum(Graphics2D g, double[][] stereoData, int width, int height) {
        int cx = width / 2;
        int cy = height / 2;
        int innerRadius = Math.min(width, height) / 4;
        int maxBarLength = Math.min(width, height) / 4 - 10;
        int totalBars = 64;
        int halfBars = totalBars / 2;
        g.setStroke(new BasicStroke(2f));
        for (int i = 0; i < halfBars; i++) {
            double theta = Math.PI + (Math.PI / halfBars) * i;
            int dataIndex = (int) ((double) i / halfBars * stereoData[0].length);
            double amp = Math.min(1.0, stereoData[0][dataIndex]);
            int length = (int) (amp * maxBarLength);
            int x1 = (int) (cx + Math.cos(theta) * innerRadius);
            int y1 = (int) (cy + Math.sin(theta) * innerRadius);
            int x2 = (int) (cx + Math.cos(theta) * (innerRadius + length));
            int y2 = (int) (cy + Math.sin(theta) * (innerRadius + length));
            g.setColor(palette.get(i % palette.size()));
            g.drawLine(x1, y1, x2, y2);
        }
        for (int i = 0; i < halfBars; i++) {
            double theta = (Math.PI / halfBars) * i;
            int dataIndex = (int) ((double) i / halfBars * stereoData[1].length);
            double amp = Math.min(1.0, stereoData[1][dataIndex]);
            int length = (int) (amp * maxBarLength);
            int x1 = (int) (cx + Math.cos(theta) * innerRadius);
            int y1 = (int) (cy + Math.sin(theta) * innerRadius);
            int x2 = (int) (cx + Math.cos(theta) * (innerRadius + length));
            int y2 = (int) (cy + Math.sin(theta) * (innerRadius + length));
            g.setColor(palette.get((i + halfBars) % palette.size()));
            g.drawLine(x1, y1, x2, y2);
        }
    }
}
