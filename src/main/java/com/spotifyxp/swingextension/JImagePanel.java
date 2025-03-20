package com.spotifyxp.swingextension;

import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.utils.GraphicalMessage;
import com.spotifyxp.utils.Resources;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JImagePanel extends JPanel {
    private BufferedImage image = null;
    private byte[] imagebytes;
    private SVGImageRecalculate recalculate = null;
    public boolean keepAspectRatio = true;
    private String rad = "";

    @FunctionalInterface
    public interface SVGImageRecalculate {
        byte[] svgImageRecalculate();
    }

    public void setKeepAspectRatio(boolean keepAspectRatio) {
        this.keepAspectRatio = keepAspectRatio;
        refresh();
    }

    void refresh() {
        try {
            if(recalculate == null) {
                image = ImageIO.read(new ByteArrayInputStream(imagebytes));
            }
        } catch (IOException ex) {
            ConsoleLogging.Throwable(ex);
        }
        this.repaint();
    }

    public void setImage(String filename) {
        this.recalculate = null;
        try {
            imagebytes = IOUtils.toByteArray(new Resources().readToInputStream(filename));
        } catch (IOException ex) {
            ConsoleLogging.Throwable(ex);
        }
        refresh();
    }

    public void setImage(File file) {
        this.recalculate = null;
        try {
            imagebytes = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        } catch (IOException ex) {
            ConsoleLogging.Throwable(ex);
        }
        refresh();
    }

    public void setImage(byte[] bytes) {
        this.recalculate = null;
        imagebytes = bytes;
        refresh();
    }

    public void setImage(SVGImageRecalculate recalculate) {
        this.recalculate = recalculate;
        refresh();
    }

    public void setRotation(int percent) {
        rad = String.valueOf(((float) 360 / 100 * percent) * 0.01745329252);
        refresh();
    }

    public void setImage(URL url) {
        this.recalculate = null;
        try {
            imagebytes = IOUtils.toByteArray(url);
        } catch (IOException ex) {
            ConsoleLogging.Throwable(ex);
        }
        refresh();
    }

    public void setImage(InputStream inputStream) {
        try {
            imagebytes = IOUtils.toByteArray(inputStream);
        } catch (IOException ex) {
            ConsoleLogging.Throwable(ex);
            GraphicalMessage.openException(ex);
        }
        refresh();
    }

    public void setImage(ImageInputStream imageInputStream) {
        this.recalculate = null;
        try {
            imageInputStream.readFully(imagebytes);
        } catch (IOException e) {
            ConsoleLogging.Throwable(e);
            GraphicalMessage.openException(e);
        }
        refresh();
    }

    public InputStream getImageStream() {
        if (imagebytes == null || imagebytes.length == 0) {
            return null;
        }
        return new ByteArrayInputStream(imagebytes);
    }

    private void drawImage(Graphics graphics2D, BufferedImage image) {
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
        int desiredWidth = this.getWidth();
        int desiredHeight = this.getHeight();
        double originalAspectRatio = (double) originalWidth / originalHeight;
        double desiredAspectRatio = (double) desiredWidth / desiredHeight;
        int newWidth, newHeight;
        int xOffset, yOffset;
        if (originalAspectRatio > desiredAspectRatio) {
            newWidth = desiredWidth;
            newHeight = (int) (desiredWidth / originalAspectRatio);
            xOffset = 0;
            yOffset = (desiredHeight - newHeight) / 2;
        } else {
            newWidth = (int) (desiredHeight * originalAspectRatio);
            newHeight = desiredHeight;
            xOffset = (desiredWidth - newWidth) / 2;
            yOffset = 0;
        }
        graphics2D.drawImage(image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), xOffset, yOffset, null);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image == null && recalculate == null) {
            return;
        }else if(recalculate != null) {
            try {
                byte[] newBytes = recalculate.svgImageRecalculate();
                if(newBytes != null && newBytes.length > 0) {
                    image = ImageIO.read(new ByteArrayInputStream(newBytes));
                }
            } catch (IOException e) {
                ConsoleLogging.Throwable(e);
                return;
            }
        }
        if (!(rad.isEmpty())) {
            Graphics2D graphics2D = (Graphics2D) g;
            if(graphics2D != null) graphics2D.rotate(Double.parseDouble(rad), (float) this.getWidth() / 2, (float) this.getHeight() / 2);
        }
        if(this.keepAspectRatio) {
            drawImage(g, image);
        } else {
            assert g != null;
            g.drawImage(image.getScaledInstance(this.getWidth(), this.getHeight(), Image.SCALE_SMOOTH), 0, 0, null);
        }
    }
}
