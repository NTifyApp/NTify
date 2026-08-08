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
package com.spotifyxp;


import com.google.gson.Gson;
import com.spotify.clienttoken.http.v0.ClientToken;
import com.spotifyxp.audio.Quality;
import com.spotifyxp.background.BackgroundService;
import com.spotifyxp.cache.Cache;
import com.spotifyxp.configuration.Config;
import com.spotifyxp.configuration.ConfigValues;
import com.spotifyxp.history.PlaybackHistory;
import com.spotifyxp.injector.Injector;
import com.spotifyxp.lib.libDetect;
import com.spotifyxp.lib.libLanguage;
import com.spotifyxp.listeners.KeyListener;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.logging.ConsoleLoggingModules;
import com.spotifyxp.logging.LogPrintStream;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.panels.PlayerArea;
import com.spotifyxp.panels.SplashPanel;
import com.spotifyxp.setup.Setup;
import com.spotifyxp.spotapi.SpotAPI;
import com.spotifyxp.spotapi.pojos.UserInfo;
import com.spotifyxp.stabilizer.GlobalExceptionHandler;
import com.spotifyxp.support.SupportModuleLoader;
import com.spotifyxp.theming.ThemeLoader;
import com.spotifyxp.updater.Updater;
import com.spotifyxp.updater.UpdaterUI;
import com.spotifyxp.utils.ApplicationUtils;
import com.spotifyxp.utils.ArchitectureDetection;
import com.spotifyxp.utils.GraphicalMessage;
import com.spotifyxp.utils.Utils;
import okhttp3.*;
import okhttp3.Authenticator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.gianlu.librespot.core.TokenProvider;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.*;
import java.io.IOException;
import java.io.PrintStream;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Initiator {
    static final Thread hook = new Thread(() -> {
        PlayerArea.saveCurrentState();
        if (PublicValues.cache != null) {
            PublicValues.cache.clearAll();
        }
        PlaybackHistory.shutdown();
    }, "Save play state");

    public static void main(String[] args) {
        try {
            PublicValues.argParser.parseArguments(args); //Parsing the arguments
            new SplashPanel().show(); //Initializing the splash panel
            System.setProperty("http.agent", ApplicationUtils.getUserAgent()); //Setting the user agent string that NTify uses
            checkDebug(); //Checking if debug is enabled
            detectOS(); //Detecting the operating system
            detectArchitecture();
            checkSetup();
            initLanguageSupport(); //Initializing the language support
            PublicValues.themeLoader = new ThemeLoader();
            initConfig(); //Initializing the configuration
            try {
                PublicValues.cache = new Cache(); //Initialize cache
            } catch (IOException e) {
                GraphicalMessage.sorryErrorExit("Failed to create cache: " + e.getMessage());
            }
            checkLogPrintStream(); //Checking some stuff after config is available
            setLanguage(); //Set the language to the one specified in the config
            creatingLock(); //Creating the 'LOCK' file
            PublicValues.defaultHttpClient = new OkHttpClient.Builder()
                    .addNetworkInterceptor(new Interceptor() {
                        @Override
                        public @NotNull Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
                            if (Objects.requireNonNull(chain.request().headers().get("User-Agent")).contains("Spotify/"))
                                return chain.proceed(chain.request());
                            return chain.proceed(chain.request().newBuilder()
                                    .header("User-Agent", ApplicationUtils.getUserAgent())
                                    .build());
                        }
                    })
                    .build(); //Creating the default http client
            initProxy();
            checkTrustStore();
            checkUpdate();
            if (Flags.videoPlaybackSupport) initializeVideoPlayback();
            loadExtensions(); //Loading extensions if there are any
            initGEH(); //Initializing the global exception handler
            PublicValues.args = args; //Storing the program arguments in PublicValues.class
            parseAudioQuality(); //Parsing the audio quality
            initThemes(); //Initializing the theming support
            addShutdownHook(); //Adding the shutdown hook
            initAPI(); //Initializing all the apis used
            if (PublicValues.enableMediaControl)
                createKeyListener(); //Starting the key listener (For Play/Pause/Previous/Next)
            initTrayIcon(); //Creating the tray icon
            try {
                initGUI(); //Initializing the GUI
            } catch (IOException e) {
                ConsoleLogging.Throwable(e);
                GraphicalMessage.sorryError("Critical exception in GUI initialization");
            }
            SplashPanel.hide(); //Hiding the splash panel
        }catch (Exception e) {
            ConsoleLogging.Throwable(e);
            GraphicalMessage.openException(e);
        }
    }

    static void checkTrustStore() {
        try {
            Request request = new Request.Builder()
                    .url("https://spclient.wg.spotify.com/reachability/check")
                    .build();

            PublicValues.defaultHttpClient.newCall(request).execute();
        }catch (SSLHandshakeException e) {
            // TrustStore outdated
            int response = GraphicalMessage.showConfirmDialog("dialogs.trust_store_outdated.title", "dialogs.trust_store_outdated.message", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
            if (response == JOptionPane.OK_OPTION) {
                try {
                    Utils.openBrowser("https://github.com/JohnTHaller/RootCertificateUpdatesForLegacyWindows");
                } catch (URISyntaxException | IOException ex) {
                    ConsoleLogging.Throwable(ex);
                }
            }

            System.exit(0);
        } catch (IOException e) {
            ConsoleLogging.warning("Failed to check for trust store issues");
        }
    }

    static void initProxy() {
        if (PublicValues.config.getFields().enableProxy) {
            SplashPanel.linfo.setText("Initializing proxy...");
            try {
                OkHttpClient.Builder clientBuilder = PublicValues.defaultHttpClient.newBuilder();
                clientBuilder.setProxyAuthenticator$okhttp(new Authenticator() {
                    @Override
                    public @NotNull Request authenticate(@Nullable Route route, @NotNull Response response) throws IOException {
                        String credential = Credentials.basic(
                                PublicValues.config.getFields().proxyUsername,
                                PublicValues.config.getFields().proxyPassword
                        );
                        return response.request().newBuilder()
                                .header("Proxy-Authorization", credential)
                                .build();
                    }
                });
                clientBuilder.setProxy$okhttp(new Proxy(
                        Proxy.Type.valueOf(PublicValues.config.getFields().proxyType),
                        new InetSocketAddress(
                                InetAddress.getByName(PublicValues.config.getFields().proxyAddress.split(":")[0]),
                                Integer.parseInt(PublicValues.config.getFields().proxyAddress.split(":")[1])
                        )
                ));
                if (PublicValues.config.getFields().proxyTrustAll) {
                    TrustManager[] trustAllCerts = new TrustManager[]{
                            new X509TrustManager() {
                                @Override
                                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                                }

                                @Override
                                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                                }

                                @Override
                                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                    return new java.security.cert.X509Certificate[]{};
                                }
                            }
                    };
                    SSLContext sslContext = SSLContext.getInstance("SSL");
                    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                    clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
                    clientBuilder.hostnameVerifier((hostname, session) -> true);
                }
                PublicValues.defaultHttpClient = clientBuilder.build();
            } catch (UnknownHostException e) {
                ConsoleLogging.Throwable(e);
                ConsoleLogging.error("Invalid proxy address");
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                ConsoleLogging.Throwable(e);
                ConsoleLogging.error("Proxy init failed");
            }
        }
    }

    static void checkDebug() {
        PrintStream out = System.out;
        if (PublicValues.debug) {
            ConsoleLogging.setColored(!System.getProperty("os.name").toLowerCase().contains("win"));
            ConsoleLoggingModules.setColored(!System.getProperty("os.name").toLowerCase().contains("win"));
            try {
                LogPrintStream stream = new LogPrintStream(true, out);
                PublicValues.logPrintStream = stream;
                System.setOut(stream.asPrintStream());
                System.setErr(stream.asPrintStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                LogPrintStream stream = new LogPrintStream(false, out);
                PublicValues.logPrintStream = stream;
                System.setOut(stream.asPrintStream());
                System.setErr(stream.asPrintStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static void detectOS() {
        SplashPanel.linfo.setText("Detecting operating system...");
        PublicValues.osType = libDetect.getDetectedOS();
        new SupportModuleLoader().loadModules();
        if(!Flags.linuxSupport) {
            if(PublicValues.osType == libDetect.OSType.Linux) {
                JOptionPane.showMessageDialog(null, "NTify was built without Linux support", "Fatal error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
        if(!Flags.macosSupport) {
            if(PublicValues.osType == libDetect.OSType.MacOS) {
                JOptionPane.showMessageDialog(null, "NTify was built without MacOS support", "Fatal error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
    }

    static void detectArchitecture() {
        SplashPanel.linfo.setText("Detecting architecture...");
        new ArchitectureDetection();
    }

    static void checkLogPrintStream() {
        PublicValues.logPrintStream.setLogging(PublicValues.config.getFields().enableLogging);
        PublicValues.logPrintStream.checkLogFiles();
    }

    static void initializeVideoPlayback() {
        if(Flags.videoPlaybackSupport) {
            SplashPanel.linfo.setText("Initializing video playback...");
            try {
                Class<?> util = Class.forName("uk.co.caprica.vlcj.SPXPInit");
                util.getMethod("init").invoke(util);
            } catch (Exception ex) {
                ConsoleLogging.Throwable(ex);
            }
        }
    }

    static void checkUpdate() {
        try {
            if (Initiator.class.getResourceAsStream("commit_id.txt") == null) {
                PublicValues.updaterDisabled = true;
                return;
            }
            SplashPanel.linfo.setText("Checking for updates...");
            Optional<Updater.UpdateInfo> updateInfoOptional = Updater.updateAvailable();
            if(updateInfoOptional.isPresent()) {
                SplashPanel.frame.setAlwaysOnTop(false);
                CompletableFuture<Boolean> usersChoiceFuture = new UpdaterUI().openWithoutUpdateFunctionality(updateInfoOptional.get());
                usersChoiceFuture.get();
                SplashPanel.frame.setAlwaysOnTop(true);
            }
        } catch (IOException | ExecutionException | InterruptedException e) {
            ConsoleLogging.Throwable(e);
        }
    }

    static void initConfig() {
        SplashPanel.linfo.setText("Initializing config...");
        try {
            PublicValues.config = Config.newInstance(PublicValues.configfilepath, ConfigValues.class, PublicValues.gson);
        } catch (IOException | IllegalAccessException | InstantiationException | NoSuchFieldException e) {
            ConsoleLogging.Throwable(e);
            GraphicalMessage.sorryErrorExit("Failed to initialize config! Exception: " + e.getMessage());
        }
    }

    static void loadExtensions() {
        SplashPanel.linfo.setText("Loading Extensions...");
        new Injector().autoInject();
    }

    static void initGEH() {
        SplashPanel.linfo.setText("Setting GlobalExceptionHandler...");
        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
    }

    static void initLanguageSupport() {
        SplashPanel.linfo.setText("Init Language...");
        PublicValues.language = new libLanguage(Initiator.class);
        PublicValues.language.setLanguageFolder("lang");
    }

    static void setLanguage() {
        SplashPanel.linfo.setText("Setting language...");
        PublicValues.language.setNoAutoFindLanguage(libLanguage.Language.getCodeFromName(PublicValues.config.getFields().language));
        try {
            PublicValues.language.setTranslationProvider(libLanguage.TranslationProviders.YAML);
        }catch (IOException e) {
            ConsoleLogging.Throwable(e);
        }
    }

    static void parseAudioQuality() {
        SplashPanel.linfo.setText("Parsing audio quality info...");
        try {
            PublicValues.quality = Quality.valueOf(PublicValues.config.getFields().audioQuality);
        } catch (Exception exception) {
            //This should not happen but when it happens don't crash NTify
            PublicValues.quality = Quality.NORMAL;
            ConsoleLogging.warning("Can't find the right audio quality! Defaulting to 'NORMAL'");
        }
    }

    static void checkSetup() {
        SplashPanel.linfo.setText("Checking setup...");
        if (!PublicValues.foundSetupArgument) {
            try {
                new Setup();
            } catch (IOException e) {
                ConsoleLogging.Throwable(e);
                JOptionPane.showMessageDialog(null, "Failed to open setup! Exception: " + e.getMessage());
            }
        }
    }

    static void initThemes() {
        SplashPanel.linfo.setText("Setting application theme...");
        ThemeLoader loader = PublicValues.themeLoader;
        try {
            loader.loadTheme(PublicValues.config.getFields().theme);
        } catch (ThemeLoader.UnknownThemeException e) {
            ConsoleLogging.warning("Unknown Theme: '" + PublicValues.config.getFields().theme + "'! Trying to load theme differently");
            try {
                loader.tryLoadTheme(PublicValues.config.getFields().theme);
            } catch (Exception e2) {
                ConsoleLogging.warning("Failed loading theme! NTify is now ugly");
            }
        }
    }

    static void creatingLock() {
        try {
            if (Utils.checkOrLockFile()) {
                JOptionPane.showMessageDialog(null, "Another instance of NTify is already running! Exiting...");
                System.exit(-1);
            }
        } catch (Exception e) {
            GraphicalMessage.openException(e);
            ConsoleLogging.Throwable(e);
            ConsoleLogging.warning("Couldn't create LOCK! NTify may be unstable");
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

    static void initAPI() throws IOException, TokenProvider.TokenException {
        SplashPanel.linfo.setText("Connecting to spotify...");
        InstanceManager.getPlayer();

        String appPlatform = "Linux";
        switch (PublicValues.osType) {
            case Windows:
                appPlatform = "Win32";
                break;
            case MacOS:
                appPlatform = "MacOS";
                break;
            case Other:
                appPlatform = "Unknown";
        }

        PublicValues.spotAPI = new SpotAPI.Builder()
                .setAcceptLanguage(Locale.getDefault().toString().replace("_", "-"))
                .setAppPlatform(appPlatform)
                .setClientToken(PublicValues.session.api().getClientToken())
                .setGson(PublicValues.gson)
                .setHttpClient(PublicValues.defaultHttpClient)
                .setSpotifyClientHost(PublicValues.session.apResolver().getRandomSpclient())
                .setUserAgent(ApplicationUtils.getUserAgent())
                .setToken(PublicValues.session.tokens().get())
                .setTokenProvider(new com.spotifyxp.spotapi.TokenProvider() {
                    @Override
                    public String requestFreshToken() {
                        try {
                            return PublicValues.session.tokens().get();
                        }catch (TokenProvider.TokenException | IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    }
                })
                .setUserInfo(new UserInfo(
                        PublicValues.session.username(),
                        PublicValues.session.countryCode(),
                        PublicValues.session.getUserAttribute("type", "PREMIUM").toUpperCase(Locale.ENGLISH)
                ))
                .build();
    }

    static void initGUI() throws IOException {
        SplashPanel.linfo.setText("Building the ui...");
        new ContentPanel().open();
    }

    static void initTrayIcon() {
        SplashPanel.linfo.setText("Creating the tray icon...");
        new BackgroundService().start();
    }
}
