package com.spotifyxp;


import com.neovisionaries.i18n.CountryCode;
import com.spotifyxp.args.ArgParser;
import com.spotifyxp.audio.Quality;
import com.spotifyxp.configuration.Config;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.deps.xyz.gianlu.librespot.core.Session;
import com.spotifyxp.dialogs.LyricsDialog;
import com.spotifyxp.history.PlaybackHistory;
import com.spotifyxp.injector.Injector;
import com.spotifyxp.lib.libDetect;
import com.spotifyxp.lib.libLanguage;
import com.spotifyxp.logging.LogPrintStream;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.theming.Theme;
import com.spotifyxp.theming.ThemeLoader;
import com.spotifyxp.utils.ApplicationUtils;
import com.spotifyxp.utils.ArchitectureDetection;
import com.spotifyxp.utils.Utils;
import com.spotifyxp.video.DummyVLCPlayer;
import com.spotifyxp.video.VLCPlayer;
import com.spotifyxp.visuals.AudioVisualizer;
import okhttp3.OkHttpClient;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

@SuppressWarnings("CanBeFinal")
public class PublicValues {
    public static int applicationHeight = 600;
    public static int applicationWidth = 800;

    public static int contentContainerHeight() {
        return applicationHeight - 111;
    }

    public static Dimension getApplicationDimensions() {
        return new Dimension(applicationWidth, (int) (applicationHeight + (PublicValues.osType == libDetect.OSType.MacOS ? 0 : menuBar.getPreferredSize().getHeight())));
    }

    public static ArchitectureDetection.Architecture architecture = ArchitectureDetection.Architecture.x86;
    public static libLanguage language = null;
    public static String fileslocation = System.getenv("appdata") + File.separator + ApplicationUtils.getName();
    public static String configfilepath = fileslocation + File.separator + "config.json";
    public static Config config = null;
    public static Session session;
    public static boolean debug = false;
    public static Quality quality = null;
    public static String[] args = null;
    @Deprecated
    public static String appLocation = fileslocation;
    public static String deviceName = ApplicationUtils.getName();
    public static Theme theme = null;
    public static libDetect.OSType osType;
    public static boolean foundSetupArgument = false;
    public static Color globalFontColor = Color.black;
    public static LyricsDialog lyricsDialog = null;
    public static Injector injector = new Injector();
    public static ContentPanel contentPanel;
    public static ArgParser argParser = new ArgParser();
    public static boolean customSaveDir = false;
    public static AudioVisualizer visualizer = new AudioVisualizer();
    public static ArrayList<ContextMenu.GlobalContextMenuItem> globalContextMenuItems = new ArrayList<>();
    public static String tempPath = System.getenv("temp");
    public static ArrayList<ContextMenu> contextMenus = new ArrayList<>();
    public static Color borderColor = Color.black;
    public static PlaybackHistory history;
    public static boolean blockLoading = false;
    public static boolean devMode = false;
    public static JMenuBar menuBar;
    public static int screenNumber = Utils.getDefaultScreenNumber();
    public static boolean shuffle = false;
    public static ThemeLoader themeLoader;
    public static boolean wasOffline;
    public static CountryCode countryCode;
    public static boolean enableMediaControl = true;
    public static OkHttpClient defaultHttpClient;
    public static VLCPlayer vlcPlayer = new DummyVLCPlayer();
    public static boolean updaterDisabled = false;
    public static LogPrintStream logPrintStream;
    public static boolean userFocusedInputField = false;
    //Devstuff
    public static boolean locationFinderActive = false;
    //----
}
