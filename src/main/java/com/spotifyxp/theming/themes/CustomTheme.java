/*
 * Copyright [2023-2026] [Gianluca Beil]
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
package com.spotifyxp.theming.themes;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spotifyxp.PublicValues;
import com.spotifyxp.args.CustomSaveDir;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.swingextension.JFrame;
import com.spotifyxp.theming.Theme;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class CustomTheme implements Theme {
    private static final JFrame frame = new JFrame();
    private static JPanel content;
    private static ThemeConfig config;

    private static class ContentPanel extends JPanel {
        public static JLabel bgcolorlabel;
        public static JTextField bgcolorfield;
        public static JButton bgcolorbutton;
        public static JLabel bordercolorlabel;
        public static JTextField bordercolorfield;
        public static JButton bordercolorbutton;
        public static JLabel tabpanelbglabel;
        public static JTextField tabpanelbgfield;
        public static JButton tabpanelbgbutton;
        public static JLabel fontcolorlabel;
        public static JTextField fontcolorfield;
        public static JButton fontcolorbutton;
        public static JLabel themetouselabel;
        public static JComboBox<String> themetouseselect;

        public ContentPanel() {
            setLayout(null);

            bgcolorlabel = new JLabel(PublicValues.language.translate("dialogs.custom_theme.fields.background_color"));
            bgcolorlabel.setBounds(6, 6, 388, 16);
            add(bgcolorlabel);

            bgcolorfield = new JTextField();
            bgcolorfield.setBounds(6, 34, 196, 26);
            add(bgcolorfield);
            bgcolorfield.setColumns(10);

            bgcolorfield.setEditable(false);

            bgcolorbutton = new JButton(PublicValues.language.translate("dialogs.custom_theme.select_color_button"));
            bgcolorbutton.setBounds(214, 34, 180, 29);
            add(bgcolorbutton);

            bgcolorbutton.addActionListener(e -> bgcolorfield.setText(openColorWheel(bgcolorfield.getText())));

            bordercolorlabel = new JLabel(PublicValues.language.translate("dialogs.custom_theme.fields.border_color"));
            bordercolorlabel.setBounds(6, 75, 388, 16);
            add(bordercolorlabel);

            bordercolorfield = new JTextField();
            bordercolorfield.setColumns(10);
            bordercolorfield.setBounds(6, 100, 196, 26);
            add(bordercolorfield);

            bordercolorfield.setEditable(false);

            bordercolorbutton = new JButton(PublicValues.language.translate("dialogs.custom_theme.select_color_button"));
            bordercolorbutton.setBounds(214, 100, 180, 29);
            add(bordercolorbutton);

            bordercolorbutton.addActionListener(e -> bordercolorfield.setText(openColorWheel(bordercolorfield.getText())));

            tabpanelbglabel = new JLabel(PublicValues.language.translate("dialogs.custom_theme.fields.tabpanel_color"));
            tabpanelbglabel.setBounds(6, 138, 388, 16);
            add(tabpanelbglabel);

            tabpanelbgbutton = new JButton(PublicValues.language.translate("dialogs.custom_theme.select_color_button"));
            tabpanelbgbutton.setBounds(214, 166, 180, 29);
            add(tabpanelbgbutton);

            tabpanelbgbutton.addActionListener(e -> tabpanelbgfield.setText(openColorWheel(tabpanelbgfield.getText())));

            tabpanelbgfield = new JTextField();
            tabpanelbgfield.setColumns(10);
            tabpanelbgfield.setBounds(6, 166, 196, 26);
            add(tabpanelbgfield);

            tabpanelbgfield.setEditable(false);

            fontcolorlabel = new JLabel(PublicValues.language.translate("dialogs.custom_theme.fields.font_color"));
            fontcolorlabel.setBounds(6, 207, 388, 16);
            add(fontcolorlabel);

            fontcolorfield = new JTextField();
            fontcolorfield.setColumns(10);
            fontcolorfield.setBounds(6, 235, 196, 26);
            add(fontcolorfield);

            fontcolorfield.setEditable(false);

            fontcolorbutton = new JButton(PublicValues.language.translate("dialogs.custom_theme.select_color_button"));
            fontcolorbutton.setBounds(214, 235, 180, 29);
            add(fontcolorbutton);

            fontcolorbutton.addActionListener(e -> fontcolorfield.setText(openColorWheel(fontcolorfield.getText())));

            themetouselabel = new JLabel(PublicValues.language.translate("dialogs.custom_theme.fields.theme_to_use"));
            themetouselabel.setBounds(6, 276, 196, 16);
            add(themetouselabel);

            themetouseselect = new JComboBox<>();
            themetouseselect.setBounds(214, 272, 180, 27);
            add(themetouseselect);

            initElements();
        }

        void initElements() {
            themetouseselect.addItem("FlatDarkLaf");
            themetouseselect.addItem("FlatLightLaf");
            themetouseselect.addItem("System");
            themetouseselect.addItem("Ugly");
            loadSettings();
        }

        void loadSettings() {
            themetouseselect.setSelectedItem(config.get("themetouse"));
            fontcolorfield.setText(config.get("fontcolor"));
            tabpanelbgfield.setText(config.get("tabpanelcolor"));
            bordercolorfield.setText(config.get("bordercolor"));
            bgcolorfield.setText(config.get("bgcolor"));
        }

        String openColorWheel(String color) {
            try {
                Color defc = Color.getColor("#FFFFFF");
                if (!color.isEmpty()) {
                    defc = Color.getColor(color);
                }
                Color c = JColorChooser.showDialog(this, PublicValues.language.translate("dialogs.custom_theme.dialogs.color_chooser.title"), defc);
                return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
            } catch (NullPointerException e) {
                //User cancelled the color selection
                return color;
            }
        }
    }

    void saveSettings() {
        config.set("themetouse", Objects.requireNonNull(ContentPanel.themetouseselect.getSelectedItem()).toString());
        config.set("fontcolor", ContentPanel.fontcolorfield.getText());
        config.set("tabpanelcolor", ContentPanel.tabpanelbgfield.getText());
        config.set("bordercolor", ContentPanel.bordercolorfield.getText());
        config.set("bgcolor", ContentPanel.bgcolorfield.getText());
    }

    public static void main(String[] args) {
        new CustomSaveDir().runArgument(new File("data").getAbsolutePath()).run();
        new CustomTheme().openCustomzationMenu();
    }

    @Override
    public String getAuthor() {
        return "Werwolf2303";
    }

    @Override
    public boolean isLight() {
        return false;
    }

    void openCustomzationMenu() {
        if (!(content == null)) {
            frame.pack();
            frame.setVisible(true);
            return;
        }
        content = new ContentPanel();
        frame.setPreferredSize(new Dimension(400, 341));
        frame.getContentPane().add(content);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveSettings();
                frame.dispose();
                JOptionPane.showConfirmDialog(com.spotifyxp.panels.ContentPanel.frame, PublicValues.language.translate("dialogs.settings.dialogs.please_restart.title"), PublicValues.language.translate("general.info"), JOptionPane.OK_CANCEL_OPTION);
            }
        });
    }

    @Override
    public void initTheme() {
        config = new ThemeConfig();
        com.spotifyxp.panels.ContentPanel.frame.setBackground(Color.decode(config.get("bgcolor")));
        com.spotifyxp.panels.ContentPanel.legacySwitch.setBackground(Color.decode(config.get("tabpanelcolor")));
        PublicValues.borderColor = Color.decode(config.get("bordercolor"));
        try {
            switch (config.get("themetouse")) {
                case "FlatDarkLaf":
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                    break;
                case "FlatLightLaf":
                    UIManager.setLookAndFeel(new FlatLightLaf());
                    break;
                case "System":
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    break;
                default:
                    ConsoleLogging.error("Invalid theme: " + config.get("themetouse") + "! Using default");
                    break;
            }
        } catch (UnsupportedLookAndFeelException e) {
            ConsoleLogging.Throwable(e);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        PublicValues.globalFontColor = Color.decode(config.get("fontcolor"));
        SpotifyXPEvents.onFrameReady.subscribe((data) -> {
            JMenu menu = new JMenu(PublicValues.language.translate("menubar.theme.name"));
            JMenuItem change = new JMenuItem(PublicValues.language.translate("menubar.theme.change_color"));
            menu.add(change);
            PublicValues.menuBar.add(menu);
            change.addActionListener(e -> openCustomzationMenu());
        });
    }

    private static class ThemeConfig {
        private final File configFile;
        private JsonObject rootCache;

        public ThemeConfig() {
            configFile = new File(PublicValues.fileslocation, "customTheme.json");
            if (!configFile.exists()) {
                try {
                    configFile.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                rootCache = new JsonObject();
                rootCache.addProperty("bgcolor", "#3C3F41");
                rootCache.addProperty("bordercolor", "#000000");
                rootCache.addProperty("tabpanelcolor", "#3F3F3F");
                rootCache.addProperty("fontcolor", "#00ff00");
                rootCache.addProperty("themetouse", "FlatDarkLaf");
                save();
            }
            load();
        }

        void load() {
            try {
                rootCache = JsonParser.parseString(IOUtils.toString(Files.newInputStream(configFile.toPath()), Charset.defaultCharset())).getAsJsonObject();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        void save() {
            try {
                Files.write(Paths.get(configFile.getAbsolutePath()), rootCache.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        String get(String name) {
            JsonElement value = rootCache.get(name);
            if (value == null) return null;
            return value.getAsString();
        }

        void set(String name, String value) {
            rootCache.remove(name);
            rootCache.addProperty(name, value);
            save();
        }
    }
}
