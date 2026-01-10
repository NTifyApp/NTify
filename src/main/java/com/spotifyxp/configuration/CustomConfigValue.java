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
package com.spotifyxp.configuration;

import com.google.gson.JsonPrimitive;
import com.spotifyxp.PublicValues;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemListener;
import java.util.ArrayList;

public class CustomConfigValue<T> implements ICustomConfigValue<T>{
    private final String name;
    private final Object defaultValue;
    private final ArrayList<String> displayValues;
    private final ArrayList<T> possibleValues;
    private JComboBox<String> component;
    private final ConfigValueTypes internalTyp;
    private final boolean fromSpotifyXP;

    @SuppressWarnings("unchecked")
    CustomConfigValue(String name, Object defaultValue, ArrayList<String> displayValues, ArrayList<T> possibleValues, ConfigValueTypes internalType) {
        this.name = name;
        this.displayValues = displayValues;
        this.fromSpotifyXP = true;
        this.defaultValue = defaultValue;
        if(possibleValues == null) {
            this.possibleValues = (ArrayList<T>) displayValues;
        }else {
            this.possibleValues = possibleValues;
        }
        this.internalTyp = internalType;
    }

    // ToDo: Find a way to get the set value of the injected setting
    /*@SuppressWarnings("unchecked")
    public CustomConfigValue(String name, Object defaultValue, ArrayList<String> displayValues, ArrayList<T> possibleValues, ConfigValueTypes internalType, boolean p) {
        this.name = name;
        this.fromSpotifyXP = false;
        this.displayValues = displayValues;
        this.defaultValue = defaultValue;
        if(possibleValues == null) {
            this.possibleValues = (ArrayList<T>) displayValues;
        }else {
            this.possibleValues = possibleValues;
        }
        this.internalTyp = internalType;
    }*/

    private static class ColoredComboBoxRenderer implements ListCellRenderer<String> {
        private final DefaultListCellRenderer defaultRenderer;

        public ColoredComboBoxRenderer() {
            defaultRenderer = new DefaultListCellRenderer();
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
            Component defaultComponent = defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            defaultComponent.setForeground(PublicValues.globalFontColor);
            return defaultComponent;
        }
    }

    @Override
    public ConfigValueTypes internalType() {
        return internalTyp;
    }

    @Override
    public boolean check() {
        if(!fromSpotifyXP) throw new UnsupportedOperationException("check() is only available for SpotifyXP settings");
        for (T val : possibleValues) {
            if (new JsonPrimitive(val.toString()).getAsString().equals(PublicValues.config.getElement(name).getAsJsonPrimitive().getAsString())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public JComponent getComponent() {
        if(component == null) {
            if(fromSpotifyXP) {
                this.component = new JComboBox<>(displayValues.toArray(new String[0]));
                this.component.setRenderer(new ColoredComboBoxRenderer());
                if(possibleValues != null) {
                    for (T val : possibleValues) {
                        if (new JsonPrimitive(val.toString()).getAsString().equals(PublicValues.config.getElement(name).getAsJsonPrimitive().getAsString())) {
                            this.component.setSelectedItem(displayValues.get(possibleValues.indexOf(val)));
                            break;
                        }
                    }
                }else {
                    for (String val : displayValues) {
                        if (val.equals(PublicValues.config.getElement(name).getAsJsonPrimitive().getAsString())) {
                            this.component.setSelectedItem(val);
                            break;
                        }
                    }
                    this.component.setSelectedItem(PublicValues.config.getElement(name));
                }
            }else {
                this.component = new JComboBox<>(displayValues.toArray(new String[0]));
                this.component.setRenderer(new ColoredComboBoxRenderer());
                this.component.setSelectedItem(name);
            }
        }
        return component;
    }

    @Override
    public void setOnClickListener(ItemListener l) {
        component.addItemListener(l);
    }

    @Override
    public void writeDefault() {
        if(!fromSpotifyXP) throw new UnsupportedOperationException("writeDefault() is only available for SpotifyXP settings");
        if (defaultValue instanceof String) {
            PublicValues.config.getProperties().addProperty(name, (String) defaultValue);
        } if (defaultValue instanceof Integer) {
            PublicValues.config.getProperties().addProperty(name, (Integer) defaultValue);
        } else if (defaultValue instanceof Boolean) {
            PublicValues.config.getProperties().addProperty(name, (Boolean) defaultValue);
        } else if (defaultValue instanceof Double) {
            PublicValues.config.getProperties().addProperty(name, (Double) defaultValue);
        }
    }

    @Override
    public Object getValue() {
        return possibleValues.get(component.getSelectedIndex());
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public ArrayList<T> getPossibleValues() {
        return possibleValues;
    }
}
