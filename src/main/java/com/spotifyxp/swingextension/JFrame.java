/*
 * Copyright [2024-2025] [Gianluca Beil]
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
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.utils.GraphicalMessage;
import com.spotifyxp.utils.Resources;
import com.spotifyxp.utils.Utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;

public class JFrame extends javax.swing.JFrame {
    public JFrame(String title) {
        super.setTitle(title);
        try {
            setIconImage(ImageIO.read(new Resources().readToInputStream("ntify.png")));
        } catch (IOException e) {
            GraphicalMessage.openException(e);
            ConsoleLogging.Throwable(e);
        }
    }

    public JFrame() {
        try {
            setIconImage(ImageIO.read(new Resources().readToInputStream("ntify.png")));
        } catch (IOException e) {
            GraphicalMessage.openException(e);
            ConsoleLogging.Throwable(e);
        }
    }

    public Point getCenter() {
        return new Point(getLocation().x / 2, getLocation().y / 2);
    }

    public void close() {
        this.setVisible(false);
    }

    public void open() {
        this.pack();
        this.setVisible(true);
    }

    @Override
    public void pack() {
        if (ContentPanel.frame.isVisible()) {
            Utils.moveToScreen(this, PublicValues.screenNumber);
        }
        super.pack();
    }
}
