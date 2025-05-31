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
package com.spotifyxp.background;


import com.spotifyxp.PublicValues;
import com.spotifyxp.lib.libDetect;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.tray.SystemTrayDialog;
import com.spotifyxp.utils.ApplicationUtils;
import com.spotifyxp.utils.Resources;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class BackgroundService {
    /**
     * Holds the trayDialog after start() was called
     */
    public static SystemTrayDialog trayDialog;

    /**
     * Creates the tray icon
     */
    public void start() {
        if(PublicValues.osType == libDetect.OSType.Linux) {
            //No support for ICCCM XEmbed protocol on newer Desktop Environments
            return;
        }
        try {
            trayDialog = new SystemTrayDialog();
            trayDialog.add(new ImageIcon(ImageIO.read(new Resources().readToInputStream("ntify.png"))), ApplicationUtils.getName());
            trayDialog.open(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    ContentPanel.frame.setVisible(true);
                    ContentPanel.frame.requestFocus();
                }
            });
        } catch (Exception e) {
            ConsoleLogging.Throwable(e);
        }
    }
}
