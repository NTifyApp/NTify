package com.spotifyxp.injector;

import com.spotifyxp.PublicValues;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.swingextension.JFrame;
import com.spotifyxp.utils.ApplicationUtils;
import com.spotifyxp.utils.AsyncActionListener;
import com.spotifyxp.utils.GraphicalMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class InjectorStore {
    public static JFrame main;
    // (Extension Store)
    //Features:
    // Download and Install Extensions
    // Remove Extensions
    public static InjectorAPI api;
    static ArrayList<InjectorAPI.ExtensionSimplified> installedExtensions;
    static int exnum = 0;
    static boolean update_available = false;
    static ArrayList<InjectorAPI.ExtensionSimplified> updatesExtensions;
    static JTabbedPane switcher;


    public InjectorStore() throws IOException {
        api = new InjectorAPI();
        installedExtensions = new ArrayList<>();
        updatesExtensions = new ArrayList<>();
        api.getInjectorFile();
        installedExtensions.addAll(api.getInjectorFile().getExtensions());
        checkUpdate();
    }

    /**
     * Opens the extension store
     */
    public void open() throws IOException {
        main = new JFrame(PublicValues.language.translate("extension.title"));
        switcher = new JTabbedPane();
        switcher.setTabPlacement(SwingConstants.TOP);
        switcher.addTab(PublicValues.language.translate("extension.store.tab"), new ContentPanel());
        switcher.addTab(PublicValues.language.translate("extension.com.spotifyxp.updater.tab"), new UpdatePanel());
        main.getContentPane().add(switcher);
        main.setPreferredSize(new Dimension(377, 526));
        main.setResizable(false);
        main.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                JOptionPane.showConfirmDialog(com.spotifyxp.panels.ContentPanel.frame, PublicValues.language.translate("ui.settings.pleaserestart"), PublicValues.language.translate("joptionpane.info"), JOptionPane.OK_CANCEL_OPTION);
            }
        });
        main.pack();
        main.setVisible(true);
    }

    class UpdatePanel extends JPanel {
        final JScrollPane scrollPane;
        final JPanel modulesholder;
        int ycache = 0;

        public UpdatePanel() throws IOException {
            setLayout(null);
            scrollPane = new JScrollPane();
            scrollPane.setBounds(0, 0, 361, 487);
            add(scrollPane);
            modulesholder = new JPanel();
            modulesholder.setSize(361, 487 * updatesExtensions.size());
            scrollPane.setViewportView(modulesholder);
            modulesholder.setLayout(null);
            for (InjectorAPI.ExtensionSimplified updateAvailable : updatesExtensions) {
                addModule(api.getExtensionRepository(updateAvailable.getName(), updateAvailable.getAuthor()), updateAvailable);
            }
        }

        public void addModule(InjectorAPI.APIInjectorRepository repository, InjectorAPI.ExtensionSimplified ex) {
            JButton extensioninstallremovebutton = new JButton(PublicValues.language.translate("extension.updateAvailable.update"));
            extensioninstallremovebutton.setBounds(260, ycache + 41, 105, 23);
            modulesholder.add(extensioninstallremovebutton);
            extensioninstallremovebutton.setForeground(PublicValues.globalFontColor);
            InjectorAPI.ExtensionSimplified installedExtension = null;
            for (InjectorAPI.ExtensionSimplified installed : api.getInjectorFile().getExtensions()) {
                if (installed.getAuthor().equals(ex.getAuthor()) && installed.getName().equals(ex.getName())) {
                    installedExtension = installed;
                    break;
                }
            }
            JLabel extensiontitle;
            if (installedExtension == null) {
                extensiontitle = new JLabel(ex.getName() + " from " + ex.getAuthor() + " - " + ex.getVersion());
            } else {
                extensiontitle = new JLabel(ex.getName() + " from " + ex.getAuthor() + " - " + installedExtension.getVersion() + " > " + ex.getVersion());
            }
            extensiontitle.setBounds(10, ycache + 11, 339, 14);
            modulesholder.add(extensiontitle);
            extensiontitle.setForeground(PublicValues.globalFontColor);
            JTextArea extensiondescription = new JTextArea(ex.getDescription());
            extensiondescription.setBackground(main.getBackground());
            extensiondescription.setBounds(10, ycache + 36, 240, 64);
            modulesholder.add(extensiondescription);
            extensiondescription.setEditable(false);
            extensiondescription.setLineWrap(true);
            extensiondescription.setWrapStyleWord(true);
            extensiondescription.setForeground(PublicValues.globalFontColor);
            ycache += 112;
            JSeparator separator = new JSeparator();
            try {
                if (PublicValues.theme.isLight()) {
                    separator.setBackground(Color.black);
                } else {
                    separator.setBackground(Color.white);
                }
            } catch (Exception e) {
                separator.setBackground(Color.black);
            }
            separator.setSize(361, 2);
            separator.setLocation(0, ycache);
            modulesholder.add(separator);
            extensioninstallremovebutton.addActionListener(new AsyncActionListener(e -> {
                try {
                    InjectorAPI.Extension extension = api.getExtensionFromSimplified(ex, repository);
                    long size = api.getSizeOfExtension(repository, extension);
                    for (InjectorAPI.ExtensionSimplified dependency : extension.getDependencies()) {
                        long depSize = api.getSizeOfExtension(repository, dependency);
                        InjectorAPI.Extension depExtension = api.getExtensionFromSimplified(dependency, repository);
                        api.downloadExtension(depExtension, repository, filesizeDownloaded -> {
                            if (filesizeDownloaded == depSize) {
                                api.getInjectorFile().write(ex);
                            }
                        });
                    }
                    api.downloadExtension(extension, repository, filesizeDownloaded -> {
                        if (filesizeDownloaded == size) {
                            api.getInjectorFile().write(ex);
                            extensioninstallremovebutton.setEnabled(false);
                        }
                    });
                } catch (IOException exc) {
                    throw new RuntimeException(exc);
                }
            }));
        }
    }

    static class ContentPanel extends JPanel {
        final JScrollPane scrollPane;
        final JPanel modulesholder;
        int ycache = 0;

        public ContentPanel() throws IOException {
            setLayout(null);
            scrollPane = new JScrollPane();
            scrollPane.setBounds(0, 0, 361, 487);
            add(scrollPane);
            modulesholder = new JPanel();
            modulesholder.setSize(361, 487 * exnum);
            scrollPane.setViewportView(modulesholder);
            modulesholder.setLayout(null);
            boolean found = false;
            for (InjectorAPI.APIInjectorRepository repository : api.getRepositories()) {
                for (InjectorAPI.ExtensionSimplified extensionSimplified : installedExtensions) {
                    exnum++;
                    addModule(repository, extensionSimplified);
                }
                for (InjectorAPI.ExtensionSimplified e : api.getExtensions(repository)) {
                    for (InjectorAPI.ExtensionSimplified ex : installedExtensions) {
                        if (e.getName().equals(ex.getName()) && e.getAuthor().equals(ex.getAuthor())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        if (!Objects.equals(e.getMinVersion(), InjectorAPI.compatibleMinVersion)) {
                            exnum++;
                            addModuleNotCompatible(e);
                            continue;
                        }
                        exnum++;
                        addModule(repository, e);
                    }
                    found = false;
                }
            }
        }

        public void addModuleNotCompatible(InjectorAPI.ExtensionSimplified ex) {
            JButton extensioninstallremovebutton = new JButton(PublicValues.language.translate("extension.install.incompatible"));
            extensioninstallremovebutton.setBounds(260, ycache + 41, 105, 23);
            modulesholder.add(extensioninstallremovebutton);
            extensioninstallremovebutton.setForeground(PublicValues.globalFontColor);
            for (InjectorAPI.ExtensionSimplified extension : installedExtensions) {
                if (extension.getIdentifier().equals(ex.getIdentifier()) && extension.getAuthor().equals(ex.getAuthor())) {
                    extensioninstallremovebutton.setText(PublicValues.language.translate("extension.remove"));
                }
            }
            extensioninstallremovebutton.addActionListener(new AsyncActionListener(e -> {
                if (!extensioninstallremovebutton.getText().equals(PublicValues.language.translate("extension.remove"))) {
                    return;
                }
                //Remove extension
                extensioninstallremovebutton.setEnabled(false);
                for (Injector.InjectionEntry entry : PublicValues.injector.injectedJars) {
                    if (entry.filename.equals((ex.getName() + "-" + ex.getAuthor() + ".jar").replace(" ", ""))) {
                        try {
                            entry.loader.close();
                        } catch (IOException exc) {
                            ConsoleLogging.Throwable(exc);
                            GraphicalMessage.openException(exc);
                        }
                    }
                }
                if (new File(PublicValues.appLocation + File.separator + "Extensions", ex.getName() + "-" + ex.getAuthor() + ".jar").delete()) {
                    api.getInjectorFile().remove(ex);
                    installedExtensions.remove(ex);
                }
            }));
            JLabel extensiontitle = new JLabel(ex.getName() + " from " + ex.getAuthor() + " - " + ex.getVersion());
            extensiontitle.setBounds(10, ycache + 11, 339, 14);
            modulesholder.add(extensiontitle);
            extensiontitle.setForeground(PublicValues.globalFontColor);
            JTextArea extensiondescription = new JTextArea(ex.getDescription());
            extensiondescription.setBackground(main.getBackground());
            extensiondescription.setBounds(10, ycache + 36, 240, 64);
            modulesholder.add(extensiondescription);
            extensiondescription.setEditable(false);
            extensiondescription.setLineWrap(true);
            extensiondescription.setWrapStyleWord(true);
            extensiondescription.setForeground(PublicValues.globalFontColor);
            ycache += 112;
            JSeparator separator = new JSeparator();
            try {
                if (PublicValues.theme.isLight()) {
                    separator.setBackground(Color.black);
                } else {
                    separator.setBackground(Color.white);
                }
            } catch (Exception e) {
                separator.setBackground(Color.black);
            }
            separator.setSize(361, 2);
            separator.setLocation(0, ycache);
            modulesholder.add(separator);
        }

        public void addModule(InjectorAPI.APIInjectorRepository repository, InjectorAPI.ExtensionSimplified ex) {
            JButton extensioninstallremovebutton = new JButton(PublicValues.language.translate("extension.install"));
            extensioninstallremovebutton.setBounds(260, ycache + 41, 105, 23);
            modulesholder.add(extensioninstallremovebutton);
            extensioninstallremovebutton.setForeground(PublicValues.globalFontColor);
            for (InjectorAPI.ExtensionSimplified extension : installedExtensions) {
                if (extension.getIdentifier().equals(ex.getIdentifier())) {
                    extensioninstallremovebutton.setText(PublicValues.language.translate("extension.remove"));
                }
            }
            extensioninstallremovebutton.addActionListener(new AsyncActionListener(e -> {
                if (extensioninstallremovebutton.getText().equals(PublicValues.language.translate("extension.remove"))) {
                    //Remove extension
                    extensioninstallremovebutton.setEnabled(false);
                    for (Injector.InjectionEntry entry : PublicValues.injector.injectedJars) {
                        if (entry.filename.equals((ex.getName() + "-" + ex.getAuthor() + "-" + ".jar").replace(" ", ""))) {
                            try {
                                entry.loader.close();
                            } catch (IOException exc) {
                                ConsoleLogging.Throwable(exc);
                                GraphicalMessage.openException(exc);
                            }
                        }
                    }
                    if (new File(PublicValues.appLocation + "/Extensions", (ex.getName() + "-" + ex.getAuthor() + ".jar").replace(" ", "")).delete()) {
                        api.getInjectorFile().remove(ex);
                        extensioninstallremovebutton.setEnabled(true);
                        extensioninstallremovebutton.setText(PublicValues.language.translate("extension.install"));
                    }
                } else {
                    //Install extension
                    try {
                        InjectorAPI.Extension extension = api.getExtensionFromSimplified(ex, repository);
                        long size = api.getSizeOfExtension(repository, extension);
                        for (InjectorAPI.ExtensionSimplified dependency : extension.getDependencies()) {
                            long depSize = api.getSizeOfExtension(repository, dependency);
                            InjectorAPI.Extension depExtension = api.getExtensionFromSimplified(dependency, repository);
                            api.downloadExtension(depExtension, repository, filesizeDownloaded -> {
                                if (filesizeDownloaded == depSize) {
                                    api.getInjectorFile().write(ex);
                                    extensioninstallremovebutton.setEnabled(true);
                                    extensioninstallremovebutton.setText(PublicValues.language.translate("extension.remove"));
                                } else {
                                    extensioninstallremovebutton.setEnabled(false);
                                }
                            });
                        }
                        api.downloadExtension(extension, repository, filesizeDownloaded -> {
                            if (filesizeDownloaded == size) {
                                api.getInjectorFile().write(ex);
                                extensioninstallremovebutton.setEnabled(true);
                                extensioninstallremovebutton.setText(PublicValues.language.translate("extension.remove"));
                            } else {
                                extensioninstallremovebutton.setEnabled(false);
                            }
                        });
                    } catch (IOException exc) {
                        throw new RuntimeException(exc);
                    }
                    GraphicalMessage.pleaseRestart();
                }
            }));
            JLabel extensiontitle = new JLabel(ex.getName() + " from " + ex.getAuthor() + " - " + ex.getVersion());
            extensiontitle.setBounds(10, ycache + 11, 339, 14);
            modulesholder.add(extensiontitle);
            extensiontitle.setForeground(PublicValues.globalFontColor);
            JTextArea extensiondescription = new JTextArea(ex.getDescription());
            extensiondescription.setBackground(main.getBackground());
            extensiondescription.setBounds(10, ycache + 36, 240, 64);
            modulesholder.add(extensiondescription);
            extensiondescription.setEditable(false);
            extensiondescription.setLineWrap(true);
            extensiondescription.setWrapStyleWord(true);
            extensiondescription.setForeground(PublicValues.globalFontColor);
            ycache += 112;
            JSeparator separator = new JSeparator();
            try {
                if (PublicValues.theme.isLight()) {
                    separator.setBackground(Color.black);
                } else {
                    separator.setBackground(Color.white);
                }
            } catch (Exception e) {
                separator.setBackground(Color.black);
            }
            separator.setSize(361, 2);
            separator.setLocation(0, ycache);
            modulesholder.add(separator);
        }
    }

    public void checkUpdate() throws IOException {
        boolean didInform = false;
        for (InjectorAPI.ExtensionSimplified extension : installedExtensions) {
            InjectorAPI.ExtensionSimplified webExtension = api.getExtensionWithNameAndAuthor(extension.getName(), extension.getAuthor());
            if (!Objects.equals(webExtension.getVersion(), extension.getVersion())) {
                //New version available!
                //Notify user
                if (!Objects.equals(webExtension.getMinVersion(), ApplicationUtils.getVersion())) {
                    continue;
                }
                update_available = true;
                updatesExtensions.add(webExtension);
                if (!didInform) {
                    didInform = true;
                    GraphicalMessage.showMessageDialog("extension.updateAvailable.title", "extension.updateAvailable.message", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }

    public boolean updateAvailable() {
        return update_available;
    }

    public ArrayList<InjectorAPI.ExtensionSimplified> getExtensionsWithUpdate() {
        return updatesExtensions;
    }
}
