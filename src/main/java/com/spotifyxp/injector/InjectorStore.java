package com.spotifyxp.injector;

import com.intellij.uiDesigner.core.GridLayoutManager;
import com.spotifyxp.PublicValues;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.swingextension.JFrame;
import com.spotifyxp.utils.SVGUtils;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class InjectorStore extends JFrame {
    public JPanel contentPanel;
    public JTabbedPane tabSwitcher;
    public JPanel availableTab;
    public JPanel installedTab;
    public byte[] refreshImageBytes;
    private MouseListener onRefresh;
    private Rectangle refreshRect;
    private boolean wasInRefresh = false;

    private Map<String, InjectorAPI.JarExtension> installedExtensions;

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


        tabSwitcher.setTitleAt(0, PublicValues.language.translate("extensions.tab1"));
        tabSwitcher.setTitleAt(1, PublicValues.language.translate("extensions.tab2"));

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

        refreshImageBytes = IOUtils.toByteArray(com.spotifyxp.graphics.Graphics.REFRESH.getInputStream());

        installedTab.setLayout(new BoxLayout(installedTab, BoxLayout.Y_AXIS));
        availableTab.setLayout(new BoxLayout(availableTab, BoxLayout.Y_AXIS));

        setTitle(PublicValues.language.translate("extension.title"));
    }

    private void refreshExtensionsAvailable() throws IOException {
        contentPanel.setEnabled(false);
        availableTab.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.NORTH;

        for (InjectorAPI.InjectorRepository repository : InjectorAPI.injectorRepos) {
            for (InjectorAPI.Extension extension : InjectorAPI.getExtensions(repository, InjectorAPI.getRepository(repository))) {
                if (installedExtensions.containsKey(extension.getIdentifier())) {
                    continue;
                }
                availableTab.add(new ExtensionModule(
                        repository,
                        extension,
                        null,
                        null,
                        new ExtensionModule.ModuleAction() {
                            @Override
                            public void run(JPanel panel) {
                                availableTab.remove(panel);
                                availableTab.revalidate();
                                availableTab.repaint();
                                refreshExtensionsInstalled();
                            }
                        },
                        null
                ).contentPanel, gbc);
                gbc.gridy++;
            }
        }
        contentPanel.setEnabled(true);
    }

    private void refreshExtensionsAll() throws IOException {
        contentPanel.setEnabled(false);
        installedTab.removeAll();
        availableTab.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.NORTH;

        for (InjectorAPI.InjectorRepository repository : InjectorAPI.injectorRepos) {
            for (InjectorAPI.Extension extension : InjectorAPI.getExtensions(repository, InjectorAPI.getRepository(repository))) {
                if (installedExtensions.containsKey(extension.getIdentifier())) {
                    ExtensionModule module = new ExtensionModule(
                            repository,
                            extension,
                            new File(
                                    new File(
                                            PublicValues.fileslocation,
                                            "Extensions"
                                    ),
                                    extension.getName()
                                            + "-"
                                            + extension.getAuthor()
                                            + ".jar")
                                    .getAbsoluteFile(),
                            null,
                            null,
                            new ExtensionModule.ModuleAction() {
                                @Override
                                public void run(JPanel panel) {
                                    installedTab.remove(panel);
                                    installedTab.revalidate();
                                    installedTab.repaint();
                                }
                            }
                    );
                    installedTab.add(module.contentPanel, gbc);
                    gbc.gridy++;
                    continue;
                }
                availableTab.add(new ExtensionModule(
                        repository,
                        extension,
                        null,
                        null,
                        new ExtensionModule.ModuleAction() {
                            @Override
                            public void run(JPanel panel) {
                                availableTab.remove(panel);
                                availableTab.revalidate();
                                availableTab.repaint();
                                refreshExtensionsInstalled();
                            }
                        },
                        null
                ).contentPanel, gbc);
                gbc.gridy++;
            }
        }
        contentPanel.setEnabled(true);
    }

    private void refreshExtensionsInstalled() {
        contentPanel.setEnabled(false);
        installedExtensions.clear();
        installedTab.removeAll();
        try {
            for (File ext : new File(PublicValues.fileslocation, "Extensions").listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            })) {
                InjectorAPI.JarExtension jarext = InjectorAPI.getPluginJson(ext);
                installedExtensions.put(jarext.getIdentifier(), jarext);
            }
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.anchor = GridBagConstraints.NORTH;

            for (InjectorAPI.InjectorRepository repository : InjectorAPI.injectorRepos) {
                for (InjectorAPI.Extension extension : InjectorAPI.getExtensions(repository, InjectorAPI.getRepository(repository))) {
                    if (installedExtensions.containsKey(extension.getIdentifier())) {
                        installedTab.add(new ExtensionModule(
                                repository,
                                extension,
                                new File(
                                        new File(
                                                PublicValues.fileslocation,
                                                "Extensions"
                                        ),
                                        extension.getName()
                                                + "-"
                                                + extension.getAuthor()
                                                + ".jar")
                                        .getAbsoluteFile(),
                                null,
                                null,
                                new ExtensionModule.ModuleAction() {
                                    @Override
                                    public void run(JPanel panel) {
                                        installedTab.remove(panel);
                                        installedTab.revalidate();
                                        installedTab.repaint();
                                    }
                                }
                        ).contentPanel, gbc);
                        gbc.gridy++;
                        continue;
                    }
                    gbc.gridy++;
                }
            }
        } catch (IOException e) {
            ConsoleLogging.Throwable(e);
        }
        contentPanel.setEnabled(true);
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
                    g.drawImage(ImageIO.read(SVGUtils.svgToImageInputStreamSameSize(new ByteArrayInputStream(refreshImageBytes), new Dimension(
                            imageSize, imageSize
                    ))).getScaledInstance(imageSize, imageSize, Image.SCALE_SMOOTH), x, y, null);
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
                for (File ext : new File(PublicValues.fileslocation, "Extensions").listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".jar");
                    }
                })) {
                    InjectorAPI.JarExtension jarext = InjectorAPI.getPluginJson(ext);
                    installedExtensions.put(jarext.getIdentifier(), jarext);
                }
                refreshExtensionsAll();
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
