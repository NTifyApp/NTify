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
package com.spotifyxp.panels;

import com.neovisionaries.i18n.CountryCode;
import com.spotifyxp.Initiator;
import com.spotifyxp.PublicValues;
import com.spotifyxp.ctxmenu.GlobalContextMenus;
import com.spotifyxp.dev.ErrorSimulator;
import com.spotifyxp.dev.LocationFinder;
import com.spotifyxp.dialogs.ErrorDisplay;
import com.spotifyxp.dialogs.HTMLDialog;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.Settings;
import com.spotifyxp.injector.InjectorStore;
import com.spotifyxp.lib.libDetect;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.logging.LogsViewer;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.swingextension.JFrame;
import com.spotifyxp.updater.Updater;
import com.spotifyxp.updater.UpdaterUI;
import com.spotifyxp.utils.ApplicationUtils;
import com.spotifyxp.utils.AsyncUtils;
import com.spotifyxp.utils.GraphicalMessage;
import com.spotifyxp.utils.Utils;
import org.apache.commons.io.IOUtils;
import xyz.gianlu.librespot.core.TokenProvider;
import xyz.gianlu.librespot.mercury.MercuryClient;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ContentPanel extends JPanel {
    public static PlayerArea playerArea;
    public static Search searchPanel;
    public static Library libraryPanel;
    public static BrowsePanel browsePanel;
    public static HomePanel homePanel;
    public static HotList hotListPanel;
    public static Queue queuePanel;
    public static Feedback feedbackPanel;
    public static ArtistPanel artistPanel;
    public static JPanel tabPanel;
    public static final JTabbedPane legacySwitch = new JTabbedPane();
    public static final JMenuBar bar = new JMenuBar();
    public static final JFrame frame;

    static {
        try {
            frame = new JFrame("NTify - " + ApplicationUtils.getVersion() + " " + ApplicationUtils.getReleaseCandidate());
        } catch (IOException e) {
            GraphicalMessage.sorryErrorExit("Unable to start the application: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static Views currentView = Views.HOME; //The view on start is home
    public static Views lastView = Views.HOME;
    public static View currentViewPanel;
    public static View lastViewPanel;
    public static Settings settings;
    public static TrackPanel trackPanel;
    public static SpotifySectionPanel sectionPanel;
    public static ErrorDisplay errorDisplay;
    public static InjectorStore injectorStore;

    public ContentPanel() throws IOException {
        PublicValues.contentPanel = this;
        ConsoleLogging.info("Building ContentPanel");
        SpotifyXPEvents.trackLoadFinished.subscribe((data) -> PublicValues.blockLoading = false);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                PublicValues.userFocusedInputField = evt.getNewValue() instanceof JTextField;
            }
        });
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if(event.getID() == KeyEvent.KEY_PRESSED) {
                    if(((KeyEvent) event).getKeyCode() == KeyEvent.VK_SPACE
                            && !PublicValues.userFocusedInputField
                            && !(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof AbstractButton)) {
                        InstanceManager.getSpotifyPlayer().playPause();
                    }
                }
            }
        }, AWTEvent.KEY_EVENT_MASK);
        SplashPanel.linfo.setText("Creating context menu items...");
        createContextMenuItems();
        SplashPanel.linfo.setText("Creating menu bar...");
        createMenuBar();
        SplashPanel.linfo.setText("Setting window size...");
        setPreferredSize(PublicValues.getApplicationDimensions());
        setLayout(null);
        SplashPanel.linfo.setText("Creating errorDisplay...");
        createErrorDisplay();
        SplashPanel.linfo.setText("Creating tabpanel...");
        createTabPanel();
        SplashPanel.linfo.setText("Creating playerarea...");
        createPlayerArea();
        SplashPanel.linfo.setText("Creating feedback...");
        createFeedback();
        SplashPanel.linfo.setText("Creating library...");
        createLibrary();
        SplashPanel.linfo.setText("Creating hotlist...");
        createHotList();
        SplashPanel.linfo.setText("Creating queue...");
        createQueue();
        SplashPanel.linfo.setText("Creating searchPanel...");
        createSearchPanel();
        SplashPanel.linfo.setText("Creating artistPanel...");
        createArtistPanel();
        SplashPanel.linfo.setText("Creating browse...");
        createBrowse();
        SplashPanel.linfo.setText("Creating browse section...");
        createSectionPanel();
        SplashPanel.linfo.setText("Creating home...");
        createHome();
        SplashPanel.linfo.setText("Creating track panel...");
        createTrackPanel();
        SplashPanel.linfo.setText("Creating settingsPanel...");
        createSettings();
        SplashPanel.linfo.setText("Making window interactive...");
        createLegacy();
        try {
            PublicValues.countryCode = CountryCode.getByCode(PublicValues.session.countryCode());
        } catch (NullPointerException e) {
            ConsoleLogging.Throwable(e);
            // Defaulting to United States
            PublicValues.countryCode = CountryCode.US;
        }
        SpotifyXPEvents.addToQueue.subscribe((data) -> InstanceManager.getSpotifyPlayer().addToQueue(data));
        SplashPanel.linfo.setText("Done building contentPanel");
        ConsoleLogging.info("Done building ContentPanel");
    }

    void createContextMenuItems() {
        for(GlobalContextMenus menu : GlobalContextMenus.values()) {
            PublicValues.globalContextMenuItems.add(menu.getGlobalContextMenuItem());
        }
    }

    private void createTrackPanel() {
        trackPanel = new TrackPanel();
        tabPanel.add(trackPanel);
    }

    void createTabPanel() {
        tabPanel = new JPanel();
        tabPanel.setLayout(new BoxLayout(tabPanel, BoxLayout.Y_AXIS));
    }

    void createSettings() {
        settings = new Settings(true);
    }

    @Override
    public void paint(java.awt.Graphics g) {
        super.paint(g);
        if (getPaintOverwrite() != null) {
            getPaintOverwrite().run(g);
        }
    }

    public static void blockTabSwitch() {
        legacySwitch.setEnabled(false);
    }

    public static void enableTabSwitch() {
        legacySwitch.setEnabled(true);
    }

    public static void showArtistPanel(String fromUri) {
        currentViewPanel.makeInvisible();
        switchView(Views.ARTIST);
        try {
            artistPanel.fillWith(fromUri);
            artistPanel.openPanel();
        } catch (IOException | MercuryClient.MercuryException | TokenProvider.TokenException ex) {
            ConsoleLogging.Throwable(ex);
        }
    }

    static void preventBuglegacySwitch() {
        for (int i = 0; i < legacySwitch.getTabCount(); i++) {
            legacySwitch.setComponentAt(i, new JPanel());
        }
    }

    public static void openAbout() {
        HTMLDialog dialog = new HTMLDialog();
        dialog.getDialog().setPreferredSize(new Dimension(400, 500));
        try {
            String out = IOUtils.toString(Initiator.class.getResourceAsStream("about.html"), StandardCharsets.UTF_8);
            String translated = PublicValues.language.translateHTML(out);
            String openSourceList = IOUtils.toString(Initiator.class.getResourceAsStream("setup/thirdparty.html"), StandardCharsets.UTF_8);
            String finalHTML = translated.split("<insertOpenSourceList>")[0] + openSourceList + translated.split("</insertOpenSourceList>")[1];
            dialog.open(PublicValues.language.translate("menubar.help.about"), finalHTML);
        } catch (Exception ex) {
            GraphicalMessage.openException(ex);
            ConsoleLogging.Throwable(ex);
        }
        dialog.getDialog().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dialog.getDialog().dispose();
            }
        });
    }

    void createHome() {
        homePanel = new HomePanel();
        tabPanel.add(homePanel);
    }

    void createBrowse() {
        browsePanel = new BrowsePanel();
        tabPanel.add(browsePanel);
    }

    void createPlayerArea() {
        playerArea = new PlayerArea(frame);
        add(playerArea);
    }

    void createLibrary() {
        libraryPanel = new Library();
        tabPanel.add(libraryPanel);
    }

    void createSectionPanel() {
        sectionPanel = new SpotifySectionPanel();
        tabPanel.add(sectionPanel);
    }

    void createArtistPanel() {
        artistPanel = new ArtistPanel();
        tabPanel.add(artistPanel);
    }

    void createSearchPanel() {
        searchPanel = new Search();
        tabPanel.add(searchPanel);
    }

    void createErrorDisplay() {
        errorDisplay = new ErrorDisplay();
        add(errorDisplay.getDisplayPanel());
    }

    void createHotList() {
        hotListPanel = new HotList();
        tabPanel.add(hotListPanel);
    }

    void createQueue() throws IOException {
        queuePanel = new Queue();
        tabPanel.add(queuePanel);
    }

    void createFeedback() {
        feedbackPanel = new Feedback();
        tabPanel.add(feedbackPanel);
    }

    @SuppressWarnings("all")
    void createLegacy() {
        legacySwitch.setForeground(PublicValues.globalFontColor);
        legacySwitch.setBounds(0, 111, PublicValues.applicationWidth, PublicValues.contentContainerHeight());
        legacySwitch.addTab(PublicValues.language.translate("tabs.home"), new JPanel());
        legacySwitch.addTab(PublicValues.language.translate("tabs.browse"), new JPanel());
        legacySwitch.addTab(PublicValues.language.translate("tabs.library"), new JPanel());
        legacySwitch.addTab(PublicValues.language.translate("tabs.search"), new JPanel());
        legacySwitch.addTab(PublicValues.language.translate("tabs.hotlist"), new JPanel());
        legacySwitch.addTab(PublicValues.language.translate("tabs.queue"), new JPanel());
        legacySwitch.addTab(PublicValues.language.translate("tabs.feedback"), new JPanel());
        legacySwitch.setUI(new BasicTabbedPaneUI() {
            @Override
            protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
                return 800 / legacySwitch.getTabCount();
            }
        });
        add(legacySwitch);
        legacySwitch.setSelectedIndex(0);
        preventBuglegacySwitch();
        legacySwitch.setComponentAt(0, tabPanel);
        switchView(Views.HOME);
        legacySwitch.addChangeListener(e -> {
            switch (legacySwitch.getSelectedIndex()) {
                case 0:
                    currentView = Views.HOME;
                    preventBuglegacySwitch();
                    legacySwitch.setComponentAt(legacySwitch.getSelectedIndex(), tabPanel);
                    switchView(Views.HOME);
                    break;
                case 1:
                    currentView = Views.BROWSE;
                    preventBuglegacySwitch();
                    legacySwitch.setComponentAt(legacySwitch.getSelectedIndex(), tabPanel);
                    switchView(Views.BROWSE);
                    break;
                case 2:
                    currentView = Views.LIBRARY;
                    preventBuglegacySwitch();
                    legacySwitch.setComponentAt(legacySwitch.getSelectedIndex(), tabPanel);
                    switchView(Views.LIBRARY);
                    break;
                case 3:
                    currentView = Views.SEARCH;
                    preventBuglegacySwitch();
                    legacySwitch.setComponentAt(legacySwitch.getSelectedIndex(), tabPanel);
                    switchView(Views.SEARCH);
                    break;
                case 4:
                    currentView = Views.HOTLIST;
                    preventBuglegacySwitch();
                    legacySwitch.setComponentAt(legacySwitch.getSelectedIndex(), tabPanel);
                    switchView(Views.HOTLIST);
                    break;
                case 5:
                    currentView = Views.QUEUE;
                    preventBuglegacySwitch();
                    legacySwitch.setComponentAt(legacySwitch.getSelectedIndex(), tabPanel);
                    switchView(Views.QUEUE);
                    break;
                case 6:
                    currentView = Views.FEEDBACK;
                    preventBuglegacySwitch();
                    legacySwitch.setComponentAt(legacySwitch.getSelectedIndex(), tabPanel);
                    switchView(Views.FEEDBACK);
                    break;
                default:
                    GraphicalMessage.bug("JTabbedPane: Clicked outsite of allowed range");
            }
        });
    }

    void createMenuBar() {
        PublicValues.menuBar = bar;
        JMenu file = new JMenu(PublicValues.language.translate("menubar.file.name"));
        JMenu edit = new JMenu(PublicValues.language.translate("menubar.edit.name"));
        JMenu view = new JMenu(PublicValues.language.translate("menubar.view.name"));
        JMenu account = new JMenu(PublicValues.language.translate("menubar.account.name"));
        JMenu help = new JMenu(PublicValues.language.translate("menubar.help.name"));
        JMenuItem exit = new JMenuItem(PublicValues.language.translate("menubar.file.exit"));
        JMenuItem logout = new JMenuItem(PublicValues.language.translate("menubar.account.logout"));
        JMenuItem about = new JMenuItem(PublicValues.language.translate("menubar.help.about"));
        JMenuItem settingsItem = new JMenuItem(PublicValues.language.translate("menubar.edit.settings"));
        JMenuItem extensions = new JMenuItem(PublicValues.language.translate("menubar.help.extension_store"));
        JMenuItem audioVisualizer = new JMenuItem(PublicValues.language.translate("menubar.view.audio_visualizer"));
        JMenuItem playUri = new JMenuItem(PublicValues.language.translate("menubar.file.play_uri"));
        JMenuItem checkUpdate = new JMenuItem(PublicValues.language.translate("menubar.help.check_update"));
        JMenuItem openlogviewer = new JMenuItem(PublicValues.language.translate("menubar.help.open_log_viewer"));
        bar.add(file);
        bar.add(edit);
        bar.add(view);
        bar.add(account);
        bar.add(help);
        if (PublicValues.devMode) {
            JMenu developer = new JMenu("Developer");
            JMenuItem locationFinder = new JMenuItem("Location Finder");
            JMenuItem errorSimulator = new JMenuItem("Error Generator");
            bar.add(developer);
            developer.add(errorSimulator);
            developer.add(locationFinder);
            errorSimulator.addActionListener(e -> new ErrorSimulator().open());
            locationFinder.addActionListener(e -> new LocationFinder());
        }
        file.add(playUri);
        file.add(exit);
        edit.add(settingsItem);
        view.add(audioVisualizer);
        account.add(logout);
        help.add(extensions);
        help.add(openlogviewer);
        if(!PublicValues.updaterDisabled) help.add(checkUpdate);
        help.add(about);
        checkUpdate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Optional<Updater.UpdateInfo> updateInfo = Updater.updateAvailable();
                    if(updateInfo.isPresent()) {
                        new UpdaterUI().openWithoutUpdateFunctionality(updateInfo.get());
                    }else{
                        JOptionPane.showMessageDialog(ContentPanel.frame, PublicValues.language.translate("dialogs.updater.dialogs.no_update_available.message"), PublicValues.language.translate("dialogs.updater.title"), JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        openlogviewer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new LogsViewer().open();
            }
        });
        audioVisualizer.addActionListener(e -> {
            try {
                PublicValues.visualizer.open();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        extensions.addActionListener(e -> AsyncUtils.run(() -> {
            if(injectorStore == null) {
                try {
                    injectorStore = new InjectorStore();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            injectorStore.open();
        }));
        settingsItem.addActionListener(e -> settings.open());
        logout.addActionListener(e -> {
            JOptionPane.showConfirmDialog(ContentPanel.frame, PublicValues.language.translate("dialogs.logout.message"), PublicValues.language.translate("general.info"), JOptionPane.OK_CANCEL_OPTION);
            AsyncUtils.run(() -> {
                new File(PublicValues.fileslocation, "credentials.json").delete();
                System.exit(0);
            });
        });
        about.addActionListener(e -> AsyncUtils.run(ContentPanel::openAbout));
        exit.addActionListener(e -> System.exit(0));
        playUri.addActionListener(e -> {
            String uri = JOptionPane.showInputDialog(frame, PublicValues.language.translate("dialogs.play_track_uri.message"), PublicValues.language.translate("dialogs.play_track_uri.title"), JOptionPane.PLAIN_MESSAGE);
            if(uri == null || uri.isEmpty()) {
                return;
            }else{
                if(!(uri.split(":").length > 2)) return;
            }
            AsyncUtils.run(() -> {
                InstanceManager.getSpotifyPlayer().load(uri, true, PublicValues.shuffle);
                SpotifyXPEvents.queueUpdate.trigger();
            });
        });
    }

    @FunctionalInterface
    public interface PaintOverwrite {
        void run(java.awt.Graphics g);
    }

    private static PaintOverwrite overwrite;

    public PaintOverwrite getPaintOverwrite() {
        return overwrite;
    }

    public void removePaintOverwrite() {
        overwrite = null;
    }

    public static void addPaintOverwrite(PaintOverwrite over) {
        overwrite = over;
        PublicValues.contentPanel.repaint();
    }

    public static void switchView(Views view) {
        if(currentViewPanel != null) {
            lastView = currentView;
            lastViewPanel = currentViewPanel;
        }
        if(lastViewPanel != null) {
            lastViewPanel.makeInvisible();
        }
        currentView = view;
        switch (view) {
            case HOME:
                currentViewPanel = homePanel;
                break;
            case BROWSE:
                currentViewPanel = browsePanel;
                break;
            case TRACKPANEL:
                currentViewPanel = trackPanel;
                break;
            case ARTIST:
                currentViewPanel = artistPanel;
                break;
            case SEARCH:
                currentViewPanel = searchPanel;
                break;
            case LIBRARY:
                currentViewPanel = libraryPanel;
                break;
            case QUEUE:
                currentViewPanel = queuePanel;
                break;
            case HOTLIST:
                currentViewPanel = hotListPanel;
                break;
            case FEEDBACK:
                currentViewPanel = feedbackPanel;
                break;
            case BROWSESECTION:
                currentViewPanel = sectionPanel;
                break;
        }
        currentViewPanel.makeVisible();
    }

    void fixSize() {
        legacySwitch.setSize(new Dimension(legacySwitch.getWidth(), getHeight() - 111));
    }

    public void open() {
        JFrame mainframe = frame;
        mainframe.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                PublicValues.screenNumber = Utils.getDisplayNumber(mainframe);
                super.componentMoved(e);
            }
        });
        mainframe.setContentPane(this);
        mainframe.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        mainframe.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if(PublicValues.osType == libDetect.OSType.Linux) {
                    //No support for ICCCM XEmbed protocol on newer Desktop Environments
                    System.exit(0);
                }
                mainframe.dispose();
            }
        });
        mainframe.setForeground(Color.blue);
        SpotifyXPEvents.onFrameReady.trigger();
        JMenu helpMenu = null;
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu menu = bar.getMenu(i);
            if (menu.getText().equals(PublicValues.language.translate("menubar.help.name"))) {
                helpMenu = menu;
                break;
            }
        }
        if (helpMenu != null) {
            bar.remove(helpMenu);
            bar.add(helpMenu);
        }
        PublicValues.menuBar.setFont(getFont());
        PublicValues.menuBar.setBorder(null);
        PublicValues.menuBar.setForeground(PublicValues.globalFontColor);
        PublicValues.menuBar.setBackground(getBackground());
        mainframe.setJMenuBar(PublicValues.menuBar);
        mainframe.open();
        mainframe.setResizable(false);
        mainframe.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width / 2 - PublicValues.applicationWidth / 2,
                Toolkit.getDefaultToolkit().getScreenSize().height / 2 - PublicValues.applicationHeight / 2)
        ;
        SpotifyXPEvents.recalcSizes.subscribe((data) -> fixSize());
        SpotifyXPEvents.recalcSizes.trigger();
        mainframe.requestFocus();
        mainframe.setAlwaysOnTop(false);
        SpotifyXPEvents.onFrameVisible.trigger();
    }
}