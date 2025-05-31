/*
 * Copyright [2024] [Gianluca Beil]
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

import javax.swing.*;

public class SettingsTable extends JPanel {
    int ycache = 10;

    public SettingsTable() {
        setLayout(null);
    }

    public void addSetting(String name, JComponent component) {
        JTextField field = new JTextField(name);
        field.setHorizontalAlignment(SwingConstants.RIGHT);
        field.setEditable(false);
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setForeground(PublicValues.globalFontColor);
        field.setBounds(10, ycache, getWidth() / 2 - 40, 30);
        component.setBounds(10 + field.getWidth(), ycache, getWidth() / 2, 30);
        component.setForeground(PublicValues.globalFontColor);
        add(component);
        add(field);
        ycache += component.getHeight() + 10;
    }
}
