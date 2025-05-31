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
package com.spotifyxp.injector;

import com.google.gson.Gson;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.spotifyxp.PublicValues;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.panels.ContentPanel;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Functionality notes:
 * <br>
 * <br> When updating it places a file named EXTENSIONNAME.jar.updated
 * <br> When an extension should be removed, it deletes the file via deleteOnExit()
 */
public class ExtensionModule {
    public JPanel contentPanel;
    public JButton installButton;
    public JLabel extensionNameAndAuthor;
    public JProgressBar installProgress;
    public JButton removeButton;
    public JButton updateButton;

    private InjectorAPI.InjectorRepository repository;
    private InjectorAPI.Extension extension;
    private InjectorAPI.JarExtension pluginJSON;
    private File installedPath;

    public ExtensionModule(
            InjectorAPI.InjectorRepository repository,
            InjectorAPI.Extension extension,
            @Nullable File installedPath,
            ModuleAction onUpdateFinished,
            ModuleAction onInstallFinished,
            ModuleAction onRemoveFinished
    ) throws IOException {
        this.repository = repository;
        this.extension = extension;
        this.installedPath = installedPath;

        if (installedPath != null) {
            pluginJSON = new Gson().fromJson(
                    IOUtils.toString(
                            new URLClassLoader(new URL[]{installedPath.toPath().toUri().toURL()}).getResourceAsStream("plugin.json"),
                            StandardCharsets.UTF_8
                    ),
                    InjectorAPI.JarExtension.class
            );
        }

        if (installedPath != null) {
            installButton.setVisible(false);
            checkUpdate();
        }

        extensionNameAndAuthor.setText(extension.getName() + " - " + extension.getAuthor());

        installProgress.setVisible(false);

        if (installedPath == null) {
            updateButton.setVisible(false);
        } else {
            updateButton.setVisible(!pluginJSON.getVersion().equals(extension.getVersion()));
        }
        updateButton.setText(PublicValues.language.translate("extensions.module.updateButton"));
        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(() -> {
                    try {
                        update(onUpdateFinished);
                    } catch (IOException ex) {
                        ConsoleLogging.Throwable(ex);
                    }
                }, "Update extension").start();
            }
        });

        if (installedPath == null) {
            removeButton.setVisible(false);
        }
        removeButton.setText(PublicValues.language.translate("extensions.module.removeButton"));
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                uninstall(onRemoveFinished);
                JOptionPane.showMessageDialog(ContentPanel.frame, PublicValues.language.translate("extensions.module.removal.dialog.description"), PublicValues.language.translate("extensions.module.removal.dialog.title"), JOptionPane.INFORMATION_MESSAGE);
            }
        });

        installButton.setText(PublicValues.language.translate("extensions.module.installButton"));
        installButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(() -> {
                    try {
                        install(onInstallFinished);
                        JOptionPane.showMessageDialog(ContentPanel.frame, PublicValues.language.translate("extensions.module.install.dialog.description"), PublicValues.language.translate("extensions.module.install.dialog.title"), JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException ex) {
                        ConsoleLogging.Throwable(ex);
                    }
                }, "Install extension").start();
            }
        });
    }

    private void uninstall(ModuleAction onRemoveDone) {
        installedPath.deleteOnExit();
        if (onRemoveDone != null) onRemoveDone.run(contentPanel);
    }

    private void install(ModuleAction onInstallDone) throws IOException {
        installProgress.setVisible(true);
        installButton.setVisible(false);
        installProgress.setVisible(true);
        updateButton.setVisible(false);
        InjectorAPI.downloadExtension(
                extension,
                repository,
                new InjectorAPI.ProgressRunnable() {
                    @Override
                    public void run(long completeSize, long downloaded) {
                        installProgress.setMaximum((int) completeSize);
                        installProgress.setValue((int) downloaded);
                        if (downloaded == completeSize) {
                            if (onInstallDone != null) onInstallDone.run(contentPanel);
                            installProgress.setVisible(false);
                            removeButton.setVisible(true);
                        }
                    }
                }
        );
    }

    private void update(ModuleAction onUpdateDone) throws IOException {
        installProgress.setVisible(true);
        updateButton.setVisible(false);
        InjectorAPI.downloadExtension(
                extension,
                repository,
                new InjectorAPI.ProgressRunnable() {
                    @Override
                    public void run(long completeSize, long downloaded) {
                        installProgress.setMaximum((int) completeSize);
                        installProgress.setValue((int) downloaded);
                        if (downloaded == completeSize) {
                            if (onUpdateDone != null) onUpdateDone.run(contentPanel);
                            installProgress.setVisible(false);
                            JOptionPane.showMessageDialog(ContentPanel.frame, PublicValues.language.translate("extensions.module.update.dialog.description"), PublicValues.language.translate("extensions.module.update.dialog.title"), JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                },
                new File(PublicValues.fileslocation,
                        "Extensions"
                                + File.separator
                                + extension.getName()
                                + "-"
                                + extension.getAuthor()
                                + ".jar.updated"
                ).getAbsolutePath()
        );
    }

    private void checkUpdate() {
        if (installedPath == null) {
            throw new IllegalStateException("Tried to check for an update on an extension that isn't installed");
        }
        if (extension.getVersion() != pluginJSON.getVersion()) {
            updateButton.setVisible(true);
        }
    }

    @FunctionalInterface
    public interface ModuleAction {
        void run(JPanel panel);
    }

    public InjectorAPI.InjectorRepository getRepository() {
        return repository;
    }

    public InjectorAPI.Extension getExtension() {
        return extension;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayoutManager(2, 4, new Insets(10, 10, 10, 10), -1, -1));
        contentPanel.setMaximumSize(new Dimension(2147483647, 100));
        contentPanel.setMinimumSize(new Dimension(502, 100));
        contentPanel.setPreferredSize(new Dimension(502, 100));
        contentPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        extensionNameAndAuthor = new JLabel();
        Font extensionNameAndAuthorFont = this.$$$getFont$$$(null, Font.BOLD, -1, extensionNameAndAuthor.getFont());
        if (extensionNameAndAuthorFont != null) extensionNameAndAuthor.setFont(extensionNameAndAuthorFont);
        extensionNameAndAuthor.setText("Label");
        contentPanel.add(extensionNameAndAuthor, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        installProgress = new JProgressBar();
        contentPanel.add(installProgress, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        removeButton = new JButton();
        removeButton.setText("Button");
        contentPanel.add(removeButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        installButton = new JButton();
        installButton.setText("Button");
        contentPanel.add(installButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        updateButton = new JButton();
        updateButton.setText("Button");
        contentPanel.add(updateButton, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPanel;
    }

}
