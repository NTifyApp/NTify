/*
 * Copyright [2023-2024] [Gianluca Beil]
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

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class EasyJLabelUI extends JFrame {
    private static final ArrayList<JLabel> labels = new ArrayList<>();
    private static JPanel panel;

    public EasyJLabelUI() {
        panel = new JPanel();
        JScrollPane pane = new JScrollPane(panel);
        panel.setLayout(null);
        add(pane, BorderLayout.CENTER);
    }

    public void addJLabel(JLabel label) {
        if (isVisible()) return;
        labels.add(label);
    }

    @Override
    public void open() {
        int ycache = 8;
        for (JLabel label : labels) {
            label.setBounds(5, ycache, getPreferredSize().width, 15);
            panel.add(label);
            ycache += 20 + 3;
            panel.setPreferredSize(new Dimension(getPreferredSize().width, ycache + 10));
        }
        super.open();
    }
}
