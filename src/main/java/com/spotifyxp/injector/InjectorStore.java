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
package com.spotifyxp.injector;

import com.intellij.uiDesigner.core.GridLayoutManager;
import com.spotifyxp.PublicValues;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.swingextension.JFrame;
import com.spotifyxp.utils.SVGUtils;
import com.spotifyxp.utils.Utils;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.io.*;
import java.util.Map;
import java.util.TreeMap;

public class InjectorStore extends JFrame {
    public JPanel contentPanel;
    public JTabbedPane tabSwitcher;
    public JPanel availableTab;
    public JPanel installedTab;
    public byte[] refreshImageBytes;
    private final MouseListener onRefresh;
    private Rectangle refreshRect;
    private boolean wasInRefresh = false;

    private final Map<String, InjectorAPI.JarExtension> installedExtensions;

    private final String cacheID = "8a17048c";
    private int cacheState = 0;

    public InjectorStore() throws IOException {
        $$$setupUI$$$();
        setContentPane(contentPanel);

        new InjectorAPI();

        installedExtensions = new TreeMap<>();

        onRefresh = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new Thread(() -> {
                    try {
                        refreshExtensionsAvailable();
                    } catch (IOException ex) {
                        ConsoleLogging.Throwable(ex);
                    }
                }, "Refresh extensions").start();
            }
        };


        tabSwitcher.setTitleAt(0, PublicValues.language.translate("dialogs.extension_store.tabs.installed"));
        tabSwitcher.setTitleAt(1, PublicValues.language.translate("dialogs.extension_store.tabs.store"));

        tabSwitcher.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (refreshRect.contains(e.getPoint())) {
                    wasInRefresh = true;
                    setCursor(Cursor.HAND_CURSOR);
                } else if (wasInRefresh) {
                    wasInRefresh = false;
                    setCursor(Cursor.DEFAULT_CURSOR);
                }
            }
        });

        tabSwitcher.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (refreshRect.contains(e.getPoint())) {
                    onRefresh.mouseClicked(e);
                }
            }
        });

        if (!PublicValues.cache.namespace("InjectorStore").has(cacheID)) {
            refreshImageBytes = IOUtils.toByteArray(com.spotifyxp.graphics.Graphics.REFRESH.getInputStream());
        } else {
            refreshImageBytes = PublicValues.cache.namespace("InjectorStore").get(cacheID);
            cacheState = 1;
        }

        installedTab.setLayout(new BoxLayout(installedTab, BoxLayout.Y_AXIS));
        availableTab.setLayout(new BoxLayout(availableTab, BoxLayout.Y_AXIS));

        setTitle(PublicValues.language.translate("dialogs.extension_store.title"));
    }

    private static final FilenameFilter JAR_FILTER = (dir, name) -> name.endsWith(".jar");

    private File installedJarPath(InjectorAPI.Extension extension) {
        return new File(
                new File(PublicValues.fileslocation, "Extensions"),
                extension.getName() + "-" + extension.getAuthor() + ".jar"
        ).getAbsoluteFile();
    }

    private void scanInstalledExtensions() throws IOException {
        installedExtensions.clear();
        File[] jars = new File(PublicValues.fileslocation, "Extensions").listFiles(JAR_FILTER);
        if (jars == null) return;

        for (File ext : jars) {
            InjectorAPI.JarExtension jarext = InjectorAPI.getPluginJson(ext);
            if (jarext.getIdentifier() == null) {
                // Extension uses an outdated plugin.json format
                ConsoleLogging.warning("Extension " + ext.getName() + " uses an outdated plugin.json format");
                continue;
            }
            installedExtensions.put(jarext.getIdentifier(), jarext);
        }
    }

    private ExtensionModule buildInstalledModule(InjectorAPI.InjectorRepository repository, InjectorAPI.Extension extension) throws IOException {
        return new ExtensionModule(
                repository,
                extension,
                installedJarPath(extension),
                null,
                null,
                panel -> {
                    installedTab.remove(panel);
                    installedTab.revalidate();
                    installedTab.repaint();
                }
        );
    }

    private ExtensionModule buildAvailableModule(InjectorAPI.InjectorRepository repository, InjectorAPI.Extension extension) throws IOException {
        return new ExtensionModule(
                repository,
                extension,
                null,
                null,
                panel -> {
                    availableTab.remove(panel);
                    availableTab.revalidate();
                    availableTab.repaint();
                    refreshExtensionsInstalled();
                },
                null
        );
    }

    private static GridBagConstraints freshGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        return gbc;
    }

    /**
     * Rebuilds the installed/available tabs from the current installedExtensions map.
     * @param rebuildInstalled whether to also clear+repopulate installedTab, or leave it as-is
     */
    private void refreshExtensionTabs(boolean rebuildInstalled) throws IOException {
        contentPanel.setEnabled(false);
        availableTab.removeAll();
        if (rebuildInstalled) installedTab.removeAll();

        GridBagConstraints availableGbc = freshGbc();
        GridBagConstraints installedGbc = freshGbc();

        for (InjectorAPI.InjectorRepository repository : InjectorAPI.injectorRepos) {
            for (InjectorAPI.Extension extension : InjectorAPI.getExtensions(repository, InjectorAPI.getRepository(repository))) {
                if (installedExtensions.containsKey(extension.getIdentifier())) {
                    if (rebuildInstalled) {
                        installedTab.add(buildInstalledModule(repository, extension).contentPanel, installedGbc);
                        installedGbc.gridy++;
                    }
                    continue;
                }
                availableTab.add(buildAvailableModule(repository, extension).contentPanel, availableGbc);
                availableGbc.gridy++;
            }
        }
        contentPanel.setEnabled(true);
    }

    private void refreshExtensionsAvailable() throws IOException {
        refreshExtensionTabs(false);
    }

    private void refreshExtensionsInstalled() {
        try {
            scanInstalledExtensions();
            refreshExtensionTabs(true);
        } catch (IOException e) {
            ConsoleLogging.Throwable(e);
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        contentPanel.setLayout(new BorderLayout(0, 0));
        tabSwitcher = new JTabbedPane();
        contentPanel.add(tabSwitcher, BorderLayout.CENTER);
        installedTab = new JPanel();
        installedTab.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabSwitcher.addTab("Installed", installedTab);
        availableTab = new JPanel();
        availableTab.setLayout(new GridBagLayout());
        tabSwitcher.addTab("Store", availableTab);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPanel;
    }

    private void createUIComponents() {
        contentPanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                Rectangle tabBounds = tabSwitcher.getBoundsAt(0);
                int imageSpacing = 5;
                int imageSize = (int) (tabBounds.getHeight() - imageSpacing * 2);
                int x = getWidth() - imageSpacing - imageSize;
                int y = imageSpacing;
                refreshRect = new Rectangle(
                        x,
                        y,
                        imageSize,
                        imageSize
                );
                try {
                    if (cacheState == 1) {
                        g.drawImage(ImageIO.read(new ByteArrayInputStream(refreshImageBytes)), x, y, null);
                    } else {
                        Image image = ImageIO.read(SVGUtils.svgToImageInputStreamSameSize(new ByteArrayInputStream(refreshImageBytes), new Dimension(
                                imageSize, imageSize
                        ))).getScaledInstance(imageSize, imageSize, Image.SCALE_SMOOTH);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ImageIO.write(Utils.imageToBufferedImage(image), "png", bos);
                        refreshImageBytes = bos.toByteArray();
                        PublicValues.cache.namespace("InjectorStore").put(cacheID, bos.toByteArray());
                        cacheState = 1;
                        g.drawImage(image, x, y, null);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    public void open() {
        new Thread(() -> {
            try {
                scanInstalledExtensions();
                refreshExtensionTabs(true);
            } catch (IOException e) {
                ConsoleLogging.Throwable(e);
            }
        }, "Load extensions").start();
        contentPanel.setEnabled(false);
        setPreferredSize(new Dimension(377, 526));
        setMinimumSize(getPreferredSize());
        setResizable(false);
        super.open();
    }
}
