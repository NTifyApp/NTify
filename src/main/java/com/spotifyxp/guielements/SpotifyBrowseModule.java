package com.spotifyxp.guielements;

import com.spotifyxp.panels.BrowsePanel;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SpotifyBrowseModule extends JPanel {
    private final byte[] image;
    private final String title;
    private final int width,height;

    public SpotifyBrowseModule(int x, int y, String title, InputStream stream, int width, int height, String genreId, BrowsePanel.XYRunnable globalRightClickListener, BrowsePanel.IDRunnable idRunnable) throws IOException {
        image = IOUtils.toByteArray(stream);
        this.title = title;
        this.width = width;
        this.height = height;

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isRightMouseButton(e)) {
                    globalRightClickListener.run(e.getX() + x, e.getY() + y);
                    return;
                }
                idRunnable.run(genreId.split(":")[2]);
            }
        });
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int targetHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        double aspectRatio = (double) originalWidth / originalHeight;
        int newWidth = (int) (targetHeight * aspectRatio);
        BufferedImage resizedImage = new BufferedImage(newWidth, targetHeight, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(originalImage, 0, 0, newWidth, targetHeight, null);
        g.dispose();
        return resizedImage;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        try {
            if(image != null) {
                Image img = resizeImage(ImageIO.read(new ByteArrayInputStream(image)), height - 20);
                int x = width - img.getWidth(null) / 2 - 7;
                int y = height - img.getHeight(null) - 10;
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.rotate(Math.toRadians(22), x, y);
                g2d.drawImage(img, x, y, null);
                g2d.dispose();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        g.setFont(new Font("SansSerif", Font.BOLD, g.getFont().getSize()));
        g.drawString(title, 10, height / 2 - getFontMetrics(getFont()).getHeight() / 2);
    }
}
