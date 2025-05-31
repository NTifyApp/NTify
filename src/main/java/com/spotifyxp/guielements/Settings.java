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
package com.spotifyxp.guielements;

import com.spotifyxp.PublicValues;
import com.spotifyxp.configuration.ConfigValueTypes;
import com.spotifyxp.configuration.ConfigValues;
import com.spotifyxp.configuration.CustomConfigValue;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.swingextension.JFrame;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;

public class Settings extends JFrame {
    JTabbedPane tabbedPane;
    HashMap<String, JPanel> tabs;
    ArrayList<String> categories;
    boolean settingsChanged = false;
    HashMap<String, String[]> warnings;
    HashMap<String, ArrayList<Object>> custom_settings;
    Settings itself;
    ArrayList<OnWrite> injected_settings_onwrite;

    @FunctionalInterface
    public interface OnWrite {
        void run(Object data);
    }

    public Settings() {
        tabbedPane = new JTabbedPane();
        tabs = new HashMap<>();
        warnings = new HashMap<>();
        categories = new ArrayList<>();
        custom_settings = new HashMap<>();
        itself = this;
        injected_settings_onwrite = new ArrayList<>();

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if(warnings.containsKey(categories.get(tabbedPane.getSelectedIndex()))) {
                    String[] message = warnings.get(categories.get(tabbedPane.getSelectedIndex()));
                    JOptionPane.showMessageDialog(itself, message[0], message[1], JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        tabbedPane.setForeground(PublicValues.globalFontColor);
        setContentPane(tabbedPane);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                e.getWindow().dispose();
                onClose();
            }

            @Override
            public void windowOpened(WindowEvent e) {
                setMinimumSize(getSize());
            }
        });
        setTitle(PublicValues.language.translate("ui.settings.title"));
    }

    public void addSetting(
            String category,
            String name,
            ConfigValueTypes type,
            Object defaultValue,
            Object currentValue,
            @NotNull OnWrite onWrite
    ) {
        if(
                !(defaultValue instanceof CustomConfigValue)
                && !(defaultValue instanceof String)
                && !(defaultValue instanceof Integer)
                && !(defaultValue instanceof Boolean)
        ) throw new IllegalArgumentException("Default value must be either a CustomConfigValue, String, Integer, or Boolean");
        injected_settings_onwrite.add(onWrite);
        if(!tabs.containsKey(category)) {
            JPanel panel = new JPanel();
            panel.setName("unofficial");
            panel.setLayout(new GridBagLayout());
            tabs.put(category, panel);
            categories.add(category);
            custom_settings.put(category, new ArrayList<>());
            tabbedPane.addTab(category, new JScrollPane(panel));
        }
        JPanel panel = tabs.get(category);
        JLabel label = new JLabel(name, SwingConstants.RIGHT);
        label.setForeground(PublicValues.globalFontColor);
        panel.add(label, createGbc(0, panel.getComponentCount() + 1, -1));
        switch(type) {
            case BOOLEAN:
                JCheckBox checkBox = new JCheckBox();
                checkBox.setSelected((boolean) currentValue);
                checkBox.addChangeListener(e -> {
                    if(checkBox.isSelected() != (boolean) defaultValue) settingsChanged = true;
                });
                panel.add(checkBox, createGbc(1, panel.getComponentCount(), -1));
                custom_settings.get(category).add(0);
                break;
            case STRING:
                JTextField textField = new JTextField();
                textField.setForeground(PublicValues.globalFontColor);
                textField.setText((String) currentValue);
                textField.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyTyped(KeyEvent e) {
                        if (textField.getText() != defaultValue) settingsChanged = true;
                    }
                });
                panel.add(textField, createGbc(1, panel.getComponentCount(), -1));
                custom_settings.get(category).add(0);
                break;
            case INT:
                JSpinner spinner = new JSpinner();
                spinner.setForeground(PublicValues.globalFontColor);
                spinner.setValue(currentValue);
                spinner.addChangeListener(e -> {
                    if(spinner.getValue() != defaultValue) settingsChanged = true;
                });
                panel.add(spinner, createGbc(1, panel.getComponentCount(), -1));
                custom_settings.get(category).add(0);
                break;
            case CUSTOM:
                if(!(defaultValue instanceof CustomConfigValue)) throw new IllegalArgumentException("Default value must be either CustomConfigValue when the value type is CUSTOM");
                panel.add(((CustomConfigValue<?>) defaultValue).getComponent(), createGbc(1, panel.getComponentCount(), -1));
                ((CustomConfigValue<?>) defaultValue).getComponent().setForeground(PublicValues.globalFontColor);
                ((CustomConfigValue<?>) defaultValue).setOnClickListener(e -> {
                    if(((CustomConfigValue<?>) defaultValue).getDefaultValue() != ((CustomConfigValue<?>) defaultValue).getValue()) settingsChanged = true;
                });
                custom_settings.get(category).add(defaultValue);
        }
    }

    protected void addSetting(ConfigValues value, int i) {
        if(!tabs.containsKey(value.category)) {
            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            tabs.put(value.category, panel);
            categories.add(value.category);
            tabbedPane.addTab(PublicValues.language.translate(value.category), new JScrollPane(panel));
            custom_settings.put(value.category, new ArrayList<>());
        }
        JPanel panel = tabs.get(value.category);
        JLabel label = new JLabel(PublicValues.language.translate(value.name), SwingConstants.RIGHT);
        label.setForeground(PublicValues.globalFontColor);
        panel.add(label, createGbc(0, i, -1));
        switch(value.type) {
            case BOOLEAN:
                JCheckBox checkBox = new JCheckBox();
                checkBox.setName(value.name);
                checkBox.setSelected(PublicValues.config.getBoolean(value.name));
                checkBox.addChangeListener(e -> {
                    if(checkBox.isSelected() != (boolean) value.defaultValue) settingsChanged = true;
                });
                panel.add(checkBox, createGbc(1, i, -1));
                custom_settings.get(value.category).add(0);
                break;
            case STRING:
                JTextField textField = new JTextField();
                textField.setName(value.name);
                textField.setForeground(PublicValues.globalFontColor);
                textField.setText(PublicValues.config.getString(value.name));
                textField.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyTyped(KeyEvent e) {
                        if (textField.getText() != value.defaultValue) settingsChanged = true;
                    }
                });
                panel.add(textField, createGbc(1, i, -1));
                custom_settings.get(value.category).add(0);
                break;
            case INT:
                JSpinner spinner = new JSpinner();
                spinner.setName(value.name);
                spinner.setForeground(PublicValues.globalFontColor);
                spinner.setValue(PublicValues.config.getInt(value.name));
                spinner.addChangeListener(e -> {
                    if(spinner.getValue() != value.defaultValue) settingsChanged = true;
                });
                panel.add(spinner, createGbc(1, i, -1));
                custom_settings.get(value.category).add(0);
                break;
            case CUSTOM:
                panel.add(((CustomConfigValue<?>) value.defaultValue).getComponent(), createGbc(1, i, -1));
                ((CustomConfigValue<?>) value.defaultValue).getComponent().setName(value.name);
                ((CustomConfigValue<?>) value.defaultValue).getComponent().setForeground(PublicValues.globalFontColor);
                ((CustomConfigValue<?>) value.defaultValue).setOnClickListener(e -> {
                    if(((CustomConfigValue<?>) value.defaultValue).getDefaultValue() != ((CustomConfigValue<?>) value.defaultValue).getValue()) settingsChanged = true;
                });
                custom_settings.get(value.category).add(value.defaultValue);
        }
    }

    @Override
    public void open() {
        for(int i = 0; i < ConfigValues.values().length; i++) {
            ConfigValues value = ConfigValues.values()[i];
            addSetting(value, i);
        }

        for(JPanel panel : tabs.values()) {
            panel.add(new JLabel(), createGbc(0, panel.getComponentCount() + 1, 1));
        }
        super.open();
    }

    private GridBagConstraints createGbc(int x, int y, int weight) {
        GridBagConstraints gbc = new GridBagConstraints();
        if(weight != -1) gbc.weighty = weight;
        if(x == 1) gbc.weightx = 1.0;
        gbc.gridx = x;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.gridy = y;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 0, 5);
        return gbc;
    }

    public void addWarningToCategory(String category, String title, String message) {
        warnings.put(category, new String[] {message, title});
    }

    void onClose() {
        if(settingsChanged) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                JPanel panel = (JPanel) ((JScrollPane)tabbedPane.getComponentAt(i)).getViewport().getView();
                if(panel.getName() != null && panel.getName().equals("unofficial")) {
                    //Injected settings tab
                    int counter = 0;
                    for (int j = 0; j < panel.getComponents().length; j++) {
                        Component component = panel.getComponents()[j];
                        if (component instanceof JLabel) {
                            continue;
                        }
                        if (component instanceof JCheckBox) {
                            //Boolean
                            injected_settings_onwrite.get(counter).run(((JCheckBox) component).isSelected());
                        }
                        if (component instanceof JTextField) {
                            //String
                            injected_settings_onwrite.get(counter).run(((JTextField) component).getText());
                        }
                        if (component instanceof JSpinner) {
                            //Integer
                            injected_settings_onwrite.get(counter).run(((JSpinner) component).getValue());
                        }
                        if (component instanceof JComboBox) {
                            //Custom
                            injected_settings_onwrite.get(counter).run(((CustomConfigValue<?>)custom_settings.get(categories.get(i)).get(counter)).getValue());
                        }
                        counter++;
                    }
                }else {
                    int counter = 0;
                    for (Component component : panel.getComponents()) {
                        if (component instanceof JLabel) {
                            continue;
                        }
                        if (component instanceof JCheckBox) {
                            //Boolean
                            PublicValues.config.write(component.getName(), ((JCheckBox) component).isSelected());
                        }
                        if (component instanceof JTextField) {
                            //String
                            PublicValues.config.write(component.getName(), ((JTextField) component).getText());
                        }
                        if (component instanceof JSpinner) {
                            //Integer
                            PublicValues.config.write(component.getName(), ((JSpinner) component).getValue());
                        }
                        if (component instanceof JComboBox) {
                            //Custom
                            PublicValues.config.write(component.getName(), ((CustomConfigValue<?>) custom_settings.get(categories.get(i)).get(counter)).getValue());
                        }
                        counter++;
                    }
                }
            }
            PublicValues.config.save();
            JOptionPane.showConfirmDialog(ContentPanel.frame, PublicValues.language.translate("ui.settings.pleaserestart"), PublicValues.language.translate("joptionpane.info"), JOptionPane.OK_CANCEL_OPTION);
        }
    }
}
