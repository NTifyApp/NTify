package com.spotifyxp;


import com.spotifyxp.audio.Quality;
import com.spotifyxp.background.BackgroundService;
import com.spotifyxp.configuration.Config;
import com.spotifyxp.configuration.ConfigValues;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.injector.Injector;
import com.spotifyxp.lib.libDetect;
import com.spotifyxp.lib.libLanguage;
import com.spotifyxp.listeners.KeyListener;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.logging.ConsoleLoggingModules;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.panels.PlayerArea;
import com.spotifyxp.panels.SplashPanel;
import com.spotifyxp.setup.Setup;
import com.spotifyxp.stabilizer.GlobalExceptionHandler;
import com.spotifyxp.support.SupportModuleLoader;
import com.spotifyxp.theming.ThemeLoader;
import com.spotifyxp.utils.ApplicationUtils;
import com.spotifyxp.utils.GraphicalMessage;
import com.spotifyxp.utils.Utils;
import okhttp3.OkHttpClient;
import org.apache.commons.io.output.NullPrintStream;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class Initiator {
    static final Thread hook = new Thread(PlayerArea::saveCurrentState, "Save play state");

    public static void main(String[] args) {
        PublicValues.argParser.parseArguments(args); //Parsing the arguments
        initEvents(); //Initializing the event support
        new SplashPanel().show(); //Initializing the splash panel
        System.setProperty("http.agent", ApplicationUtils.getUserAgent()); //Setting the user agent string that SpotifyXP uses
        checkDebug(); //Checking if debug is enabled
        checkSetup();
        detectOS(); //Detecting the operating system
        initLanguageSupport(); //Initializing the language support
        initConfig(); //Initializing the configuration
        setLanguage(); //Set the language to the one specified in the config
        creatingLock(); //Creating the 'LOCK' file
        PublicValues.defaultHttpClient = new OkHttpClient(); //Creating the default http client
        initializeVideoPlayback();
        loadExtensions(); //Loading extensions if there are any
        initGEH(); //Initializing the global exception handler
        storeArguments(args); //Storing the program arguments in PublicValues.class
        parseAudioQuality(); //Parsing the audio quality
        initThemes(); //Initializing the theming support
        addShutdownHook(); //Adding the shutdown hook
        initAPI(); //Initializing all the apis used
        if (PublicValues.enableMediaControl)
            createKeyListener(); //Starting the key listener (For Play/Pause/Previous/Next)
        initTrayIcon(); //Creating the tray icon
        try {
            initGUI(); //Initializing the GUI
        }catch (IOException e) {
            ConsoleLogging.Throwable(e);
            GraphicalMessage.sorryError("Critical exception in GUI initialization");
        }
        SplashPanel.hide(); //Hiding the splash panel
    }

    static void checkDebug() {
        if (PublicValues.debug) {
            ConsoleLogging.setColored(!System.getProperty("os.name").toLowerCase().contains("win"));
            ConsoleLoggingModules.setColored(!System.getProperty("os.name").toLowerCase().contains("win"));
        } else {
            System.setOut(new NullPrintStream());
            System.setErr(new NullPrintStream());
        }
    }

    static void detectOS() {
        SplashPanel.linfo.setText("Detecting operating system...");
        PublicValues.osType = libDetect.getDetectedOS();
        new SupportModuleLoader().loadModules();
    }

    static void initializeVideoPlayback() {
        try {
            Class<?> util = Class.forName("com.spotifyxp.deps.uk.co.caprica.vlcj.SPXPInit");
            util.getMethod("init").invoke(util);
        } catch (Exception ex) {
            ex.printStackTrace();
            ConsoleLogging.info("SpotifyXP was built without video playback support");
        }
    }

    static void initEvents() {
        for (SpotifyXPEvents s : SpotifyXPEvents.values()) {
            Events.register(s.getName(), true);
        }
    }

    static void initConfig() {
        SplashPanel.linfo.setText("Initializing config...");
        PublicValues.config = new Config();
        PublicValues.config.checkConfig();
    }

    static void loadExtensions() {
        SplashPanel.linfo.setText("Loading Extensions...");
        new Injector().autoInject();
    }

    static void initGEH() {
        SplashPanel.linfo.setText("Setting up globalexceptionhandler...");
        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
    }

    static void storeArguments(String[] args) {
        SplashPanel.linfo.setText("Storing program arguments...");
        PublicValues.args = args;
    }

    static void initLanguageSupport() {
        SplashPanel.linfo.setText("Init Language...");
        PublicValues.language = new libLanguage();
        PublicValues.language.setLanguageFolder("lang");
    }

    static void setLanguage() {
        SplashPanel.linfo.setText("Setting language...");
        PublicValues.language.setNoAutoFindLanguage(libLanguage.Language.getCodeFromName(PublicValues.config.getString(ConfigValues.language.name)));
    }

    static void parseAudioQuality() {
        SplashPanel.linfo.setText("Parsing audio quality info...");
        try {
            PublicValues.quality = Quality.valueOf(PublicValues.config.getString(ConfigValues.audioquality.name));
        } catch (Exception exception) {
            //This should not happen but when it happens don't crash SpotifyXP
            PublicValues.quality = Quality.NORMAL;
            ConsoleLogging.warning("Can't find the right audio quality! Defaulting to 'NORMAL'");
        }
    }

    static void checkSetup() {
        SplashPanel.linfo.setText("Checking setup...");
        if (!PublicValues.foundSetupArgument) {
            new Setup();
        }
    }

    static void initThemes() {
        SplashPanel.linfo.setText("Init Themes...");
        ThemeLoader loader = PublicValues.themeLoader;
        try {
            loader.loadTheme(PublicValues.config.getString(ConfigValues.theme.name));
        } catch (ThemeLoader.UnknownThemeException e) {
            ConsoleLogging.warning("Unknown Theme: '" + PublicValues.config.getString(ConfigValues.theme.name) + "'! Trying to load theme differently");
            try {
                loader.tryLoadTheme(PublicValues.config.getString(ConfigValues.theme.name));
            } catch (Exception e2) {
                ConsoleLogging.warning("Failed loading theme! SpotifyXP is now ugly");
            }
        }
    }

    static void creatingLock() {
        try {
            if(Utils.checkOrLockFile()) {
                JOptionPane.showMessageDialog(null, "Another instance of SpotifyXP is already running! Exiting...");
                System.exit(-1);
            }
        } catch (Exception e) {
            GraphicalMessage.openException(e);
            ConsoleLogging.Throwable(e);
            ConsoleLogging.warning("Couldn't create LOCK! SpotifyXP may be unstable");
        }
    }

    static void addShutdownHook() {
        SplashPanel.linfo.setText("Add shutdown hook...");
        Runtime.getRuntime().addShutdownHook(hook);
    }

    static void createKeyListener() {
        SplashPanel.linfo.setText("Creating keylistener...");
        new KeyListener().start();
    }

    static void initAPI() {
        SplashPanel.linfo.setText("Creating api...");
        InstanceManager.getSpotifyAPI();
        InstanceManager.getPlayer();
        InstanceManager.getPkce();
        SplashPanel.linfo.setText("Create advanced api key...");
        InstanceManager.getUnofficialSpotifyApi();
    }

    static void initGUI() throws IOException {
        SplashPanel.linfo.setText("Creating contentPanel...");
        new ContentPanel().open();
    }

    static void initTrayIcon() {
        SplashPanel.linfo.setText("Creating the tray icon...");
        new BackgroundService().start();
    }
}
