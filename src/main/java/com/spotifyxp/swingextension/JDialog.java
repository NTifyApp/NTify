/*
 * Copyright [2025] [Gianluca Beil]
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
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.utils.Resources;
import com.spotifyxp.utils.Utils;

import javax.imageio.ImageIO;
import java.io.IOException;

public class JDialog extends javax.swing.JDialog {
    public JDialog() throws IOException {
        super(ContentPanel.frame);
        setIconImage(ImageIO.read(new Resources().readToInputStream("ntify.png")));
    }

    public void pack() {
        if (ContentPanel.frame.isVisible()) {
            Utils.moveToScreen(this, PublicValues.screenNumber);
        }
        super.pack();
    }
}
