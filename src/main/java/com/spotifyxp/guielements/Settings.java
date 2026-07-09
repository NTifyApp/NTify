/*
 * Copyright [2025-2026] [Gianluca Beil]
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
import com.spotifyxp.configuration.Config;
import com.spotifyxp.configuration.IConfig;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.swingextension.JFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Settings extends JFrame {
    public JTabbedPane tabbedPane;
    HashMap<String, JPanel> tabs;
    HashMap<String, Config.RuntimeConfig<?>> configInstanceMap;
    ArrayList<String> categories;
    boolean settingsChanged = false;
    HashMap<String, Object> custom_settings;
    Settings itself;

    /**
     * You may not instantiate this class yourself
     */
    public Settings(boolean initializeDefault) {
        tabbedPane = new JTabbedPane();
        configInstanceMap = new HashMap<>();
        categories = new ArrayList<>();
        itself = this;
        custom_settings = new HashMap<>();
        tabs = new HashMap<>();

        tabbedPane.setForeground(PublicValues.globalFontColor);
        setContentPane(tabbedPane);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                e.getWindow().dispose();
                try {
                    onClose();
                } catch (NoSuchFieldException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public void windowOpened(WindowEvent e) {
                setMinimumSize(getSize());
            }
        });
        setTitle(PublicValues.language.translate("dialogs.settings.title"));

        if (!initializeDefault) return;

        try {
            addSettings(PublicValues.config);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    public void addSettings(
            String pluginUUID,
            Config.RuntimeConfig<?> config
    ) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException, InstantiationException {
        if (!configInstanceMap.containsKey(pluginUUID))
            configInstanceMap.put(pluginUUID, config);

        if (pluginUUID == null)
            throw new IllegalArgumentException("Plugin UUID is required");

        IConfig configClassInstance = (IConfig) config.getFields();

        for(Field field : configClassInstance.getClass().getFields()) {
            addSetting(config, field, pluginUUID);
        }
    }

    @SuppressWarnings("unchecked")
    private void addSetting(Config.RuntimeConfig<?> config, Field field, String pluginUUID) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException, InstantiationException {
        if (field.getAnnotations().length == 0) {
            ConsoleLogging.warning("[Settings] Skipping field without annotation");
            return;
        }

        if (field.getAnnotations()[0].annotationType() == Config.HiddenConfigValue.class) {
            ConsoleLogging.debug("[Settings] Skipping field with hidden annotation");
            return;
        }

        IConfig configClassInstance = (IConfig) config.getFields();

        Annotation annotation = field.getAnnotations()[0];
        Class<?> annotationClass = annotation.annotationType();
        String id = (String) annotationClass.getDeclaredMethod("id").invoke(annotation);
        String category = (String) annotationClass.getDeclaredMethod("category").invoke(annotation);
        String translationKey = (String) annotationClass.getDeclaredMethod("translationKey").invoke(annotation);
        Object currentValue = field.get(configClassInstance);
        Object defaultValue = field.get(configClassInstance);
        String translation = configClassInstance.translate(translationKey.isEmpty() ? id : translationKey);

        if(!tabs.containsKey(category)) {
            JPanel panel = new JPanel();
            if (pluginUUID != null)
                panel.setName("unofficial/" + pluginUUID);
            panel.setLayout(new GridBagLayout());
            tabs.put(category, panel);
            categories.add(category);
            tabbedPane.addTab(configClassInstance.translate(category), new JScrollPane(panel));
        }

        JPanel panel = tabs.get(category);
        JLabel label = new JLabel(translation, SwingConstants.RIGHT);
        label.setForeground(PublicValues.globalFontColor);
        panel.add(label, createGbc(0, panel.getComponentCount() + 1, -1));

        if (annotationClass.equals(Config.CheckBox.class)) {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setSelected((boolean) field.get(configClassInstance));
            checkBox.addChangeListener(e -> {
                if(checkBox.isSelected() != (boolean) defaultValue) settingsChanged = true;
            });
            checkBox.setName(field.getName());
            panel.add(checkBox, createGbc(1, panel.getComponentCount(), -1));
        } else if (annotationClass.equals(Config.Text.class)) {
            com.spotifyxp.swingextension.JTextField textField = new com.spotifyxp.swingextension.JTextField(
                    (int) annotationClass.getDeclaredMethod("characterLimit").invoke(annotation)
            );
            List<String> allowedValues = config.getAllowedValuesFor(field.getName());
            boolean emptyAllowed = (boolean) annotationClass.getDeclaredMethod("allowEmpty").invoke(annotation);
            textField.setForeground(PublicValues.globalFontColor);
            textField.setText((String) currentValue);
            textField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    if (textField.getText() != defaultValue) settingsChanged = true;
                    if (!allowedValues.isEmpty())
                        if (allowedValues.contains(textField.getText()))
                            textField.setForeground(Color.RED);
                        else textField.setForeground(PublicValues.globalFontColor);

                    if (textField.getText().isEmpty() && !emptyAllowed)
                        textField.setForeground(Color.RED);
                    else textField.setForeground(PublicValues.globalFontColor);
                }
            });
            textField.setName(field.getName());
            panel.add(textField, createGbc(1, panel.getComponentCount(), -1));
        } else if (annotationClass.equals(Config.Numbers.class)) {
            int minValue = (int) annotationClass.getDeclaredMethod("min").invoke(annotation);
            int maxValue = (int) annotationClass.getDeclaredMethod("max").invoke(annotation);
            SpinnerNumberModel model = new SpinnerNumberModel((int) currentValue, minValue, maxValue, 1);
            JSpinner spinner = new JSpinner(model);
            spinner.setForeground(PublicValues.globalFontColor);
            spinner.addChangeListener(e -> {
                if(spinner.getValue() != defaultValue) settingsChanged = true;
            });
            spinner.setName(field.getName());
            panel.add(spinner, createGbc(1, panel.getComponentCount(), -1));
        } else if (annotationClass.equals(Config.Dropdown.class)) {
            List<String> values = config.getAllowedValuesFor(field.getName());
            List<Object> mappings = (List<Object>) config.getMappingValuesFor(field.getName());
            JComboBox<String> comboBox = new JComboBox<>(values.toArray(new String[0]));
            comboBox.setRenderer(new ColoredComboBoxRenderer());
            comboBox.addItemListener(e -> {
                if (comboBox.getSelectedItem() != currentValue) settingsChanged = true;
            });
            comboBox.setName(field.getName());
            comboBox.setForeground(PublicValues.globalFontColor);
            panel.add(comboBox, createGbc(1, panel.getComponentCount(), -1));
            if (mappings.isEmpty()) {
                mappings = Arrays.asList(values.toArray());
                comboBox.setSelectedItem(currentValue);
            } else comboBox.setSelectedItem(values.get(mappings.indexOf(currentValue)));
            custom_settings.put(field.getName(), mappings);
        } else if (annotationClass.equals(Config.CustomComponent.class)) {
            Method method = annotationClass.getMethod("component");
            method.setAccessible(true);
            Class<?> providerClass = (Class<?>) method.invoke(annotation);
            Constructor<?> constructor = providerClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Config.CustomComponentCallback componentCallback = (Config.CustomComponentCallback) constructor.newInstance();
            JComponent component = componentCallback.component();
            component.setName(field.getName());
            panel.add(component, createGbc(1, panel.getComponentCount(), -1));
            custom_settings.put(field.getName(), componentCallback);
        } else {
            ConsoleLogging.error("[Settings] Unsupported annotation in config class");
        }
    }

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

    protected void addSettings(Config.RuntimeConfig<?> config) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException, InstantiationException {
        IConfig configClassInstance = (IConfig) config.getFields();

        for(Field field : configClassInstance.getClass().getFields()) {
            addSetting(config, field, null);
        }
    }

    @Override
    public void open() {
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

    @SuppressWarnings("unchecked")
    void onClose() throws NoSuchFieldException {
        if(settingsChanged) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                JPanel panel = (JPanel) ((JScrollPane)tabbedPane.getComponentAt(i)).getViewport().getView();
                Config.RuntimeConfig<?> config = PublicValues.config;
                if(panel.getName() != null && panel.getName().equals("unofficial")) {
                    //Injected settings tab
                    config = configInstanceMap.get(panel.getName().split("/")[1]);
                }
                for (Component component : panel.getComponents()) {
                    if (component instanceof JLabel) {
                        continue;
                    }
                    if (component instanceof JCheckBox) {
                        //Boolean
                        config.write(component.getName(), ((JCheckBox) component).isSelected());
                    }
                    if (component instanceof JTextField) {
                        //String
                        config.write(component.getName(), ((JTextField) component).getText());
                    }
                    if (component instanceof JSpinner) {
                        //Integer
                        config.write(component.getName(), ((JSpinner) component).getValue());
                    }
                    if (component instanceof JComboBox) {
                        //Custom
                        config.write(component.getName(), ((List<Object>) custom_settings.get(component.getName())).get(((JComboBox<?>) component).getSelectedIndex()));
                    }
                    if (component instanceof JPanel) {
                        ((Config.CustomComponentCallback) custom_settings.get(component.getName())).onSave(
                                (JComponent) component
                        );
                    }
                }
            }
            PublicValues.config.save();
            JOptionPane.showConfirmDialog(ContentPanel.frame, PublicValues.language.translate("dialogs.settings.dialogs.please_restart.title"), PublicValues.language.translate("general.info"), JOptionPane.OK_CANCEL_OPTION);
        }
    }
}
