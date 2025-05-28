package com.spotifyxp.panels;

import com.neovisionaries.i18n.CountryCode;
import com.spotifyxp.PublicValues;
import com.spotifyxp.ctxmenu.GlobalContextMenus;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Artist;
import com.spotifyxp.dev.ErrorSimulator;
import com.spotifyxp.dev.LocationFinder;
import com.spotifyxp.dialogs.ErrorDisplay;
import com.spotifyxp.dialogs.HTMLDialog;
import com.spotifyxp.events.Events;
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
import com.spotifyxp.utils.*;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
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
    public static final JFrame frame = new JFrame(ApplicationUtils.getName() + " - " + ApplicationUtils.getVersion() + " " + ApplicationUtils.getReleaseCandidate());
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
        ConsoleLogging.info(PublicValues.language.translate("debug.buildcontentpanelbegin"));
        Events.subscribe(SpotifyXPEvents.trackLoadFinished.getName(), (Object... data) -> PublicValues.blockLoading = false);
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
        SplashPanel.linfo.setText("Creating injectorStore...");
        injectorStore = new InjectorStore();
        SplashPanel.linfo.setText("Adding window mouse listener...");
        SplashPanel.linfo.setText("Deciding population of hotlist...");
        SplashPanel.linfo.setText("Making window interactive...");
        createLegacy();
        try {
            if (!(InstanceManager.getSpotifyApi().getCurrentUsersProfile() == null)) {
                PublicValues.countryCode = InstanceManager.getSpotifyApi().getCurrentUsersProfile().build().execute().getCountry();
            }
        } catch (IOException | NullPointerException e) {
            ConsoleLogging.Throwable(e);
            // Defaulting to United States
            PublicValues.countryCode = CountryCode.US;
        }
        Events.subscribe(SpotifyXPEvents.addtoqueue.getName(), data -> InstanceManager.getPlayer().getPlayer().addToQueue((String)data[0]));
        SplashPanel.linfo.setText("Done building contentPanel");
        ConsoleLogging.info(PublicValues.language.translate("debug.buildcontentpanelend"));
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
        settings = new Settings();
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
            Artist a = InstanceManager.getSpotifyApi().getArtist(fromUri.split(":")[2]).build().execute();
            artistPanel.fillWith(a);
            artistPanel.openPanel();
        } catch (IOException ex) {
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
            String out = new Resources().readToString("about.html");
            StringBuilder cache = new StringBuilder();
            for (String s : out.split("\n")) {
                if (s.contains("(TRANSLATE)")) {
                    s = s.replace(s.split("\\(TRANSLATE\\)")[1].replace("(TRANSLATE)", ""), PublicValues.language.translate(s.split("\\(TRANSLATE\\)")[1].replace("(TRANSLATE)", "")));
                    s = s.replace("(TRANSLATE)", "");
                }
                cache.append(s);
            }
            String openSourceList = new Resources().readToString("setup/thirdparty.html");
            String finalHTML = cache.toString().split("<insertOpenSourceList>")[0] + openSourceList + cache.toString().split("</insertOpenSourceList>")[1];
            dialog.open(PublicValues.language.translate("ui.menu.help.about"), finalHTML.replace("%APPNAME%", ApplicationUtils.getName()));
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
        legacySwitch.addTab(PublicValues.language.translate("ui.navigation.home"), new JPanel());
        legacySwitch.addTab(PublicValues.language.translate("ui.navigation.browse"), new JPanel());
        legacySwitch.addTab(PublicValues.language.translate("ui.navigation.library"), new JPanel());
        legacySwitch.addTab(PublicValues.language.translate("ui.navigation.search"), new JPanel());
        legacySwitch.addTab(PublicValues.language.translate("ui.navigation.hotlist"), new JPanel());
        legacySwitch.addTab(PublicValues.language.translate("ui.navigation.queue"), new JPanel());
        legacySwitch.addTab(PublicValues.language.translate("ui.navigation.feedback"), new JPanel());
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
                    if (Library.libraryTracks.librarySongList.getModel().getRowCount() == 0) {
                        Library.libraryTracks.libraryThread.start();
                    }
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
        JMenu file = new JMenu(PublicValues.language.translate("ui.legacy.file"));
        JMenu edit = new JMenu(PublicValues.language.translate("ui.legacy.edit"));
        JMenu view = new JMenu(PublicValues.language.translate("ui.legacy.view"));
        JMenu account = new JMenu(PublicValues.language.translate("ui.legacy.account"));
        JMenu help = new JMenu(PublicValues.language.translate("ui.legacy.help"));
        JMenuItem exit = new JMenuItem(PublicValues.language.translate("ui.legacy.exit"));
        JMenuItem logout = new JMenuItem(PublicValues.language.translate("ui.legacy.logout"));
        JMenuItem about = new JMenuItem(PublicValues.language.translate("ui.legacy.about"));
        JMenuItem settingsItem = new JMenuItem(PublicValues.language.translate("ui.legacy.settings"));
        JMenuItem extensions = new JMenuItem(PublicValues.language.translate("ui.legacy.extensionstore"));
        JMenuItem audioVisualizer = new JMenuItem(PublicValues.language.translate("ui.legacy.view.audiovisualizer"));
        JMenuItem playUri = new JMenuItem(PublicValues.language.translate("ui.legacy.playuri"));
        JMenuItem checkUpdate = new JMenuItem(PublicValues.language.translate("updater.menubar.title"));
        JMenuItem openlogviewer = new JMenuItem(PublicValues.language.translate("logsviewer.open"));
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
                        JOptionPane.showMessageDialog(ContentPanel.frame, PublicValues.language.translate("updater.noupdatedialog.message"), PublicValues.language.translate("updater.noupdatedialog.title"), JOptionPane.INFORMATION_MESSAGE);
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
        audioVisualizer.addActionListener(e -> PublicValues.visualizer.open());
        extensions.addActionListener(e -> {
            injectorStore.open();
        });
        settingsItem.addActionListener(new AsyncActionListener(e -> settings.open()));
        logout.addActionListener(new AsyncActionListener(e -> {
            JOptionPane.showConfirmDialog(ContentPanel.frame, PublicValues.language.translate("ui.logout.text"), PublicValues.language.translate("ui.logout.title"), JOptionPane.OK_CANCEL_OPTION);
            System.exit(0);
        }));
        about.addActionListener(new AsyncActionListener(e -> openAbout()));
        exit.addActionListener(e -> System.exit(0));
        playUri.addActionListener(new AsyncActionListener(e -> {
            String uri = JOptionPane.showInputDialog(frame, PublicValues.language.translate("ui.playtrackuri.message"), PublicValues.language.translate("ui.playtrackuri.title"), JOptionPane.PLAIN_MESSAGE);
            if(uri == null || uri.isEmpty()) {
                return;
            }else{
                if(!(uri.split(":").length > 2)) return;
            }
            InstanceManager.getSpotifyPlayer().load(uri, true, PublicValues.shuffle);
            Events.triggerEvent(SpotifyXPEvents.queueUpdate.getName());
        }));
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
        Events.triggerEvent(SpotifyXPEvents.onFrameReady.getName());
        JMenu helpMenu = null;
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu menu = bar.getMenu(i);
            if (menu.getText().equals(PublicValues.language.translate("ui.legacy.help"))) {
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
        Events.subscribe(SpotifyXPEvents.recalculateSizes.getName(), (Object... data) -> fixSize());
        Events.triggerEvent(SpotifyXPEvents.recalculateSizes.getName());
        mainframe.requestFocus();
        mainframe.setAlwaysOnTop(false);
        Events.triggerEvent(SpotifyXPEvents.onFrameVisible.getName());
    }
}