/*
 * Copyright [2023-2025] [Gianluca Beil]
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
package com.spotifyxp.utils;

import com.spotifyxp.PublicValues;
import com.spotifyxp.configuration.ConfigValues;
import com.spotifyxp.deps.com.spotify.connectstate.Connect;
import com.spotifyxp.deps.xyz.gianlu.librespot.ZeroconfServer;
import com.spotifyxp.deps.xyz.gianlu.librespot.audio.decoders.AudioQuality;
import com.spotifyxp.deps.xyz.gianlu.librespot.common.Utils;
import com.spotifyxp.deps.xyz.gianlu.librespot.core.OAuth;
import com.spotifyxp.deps.xyz.gianlu.librespot.core.Session;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryClient;
import com.spotifyxp.deps.xyz.gianlu.librespot.player.Player;
import com.spotifyxp.deps.xyz.gianlu.librespot.player.PlayerConfiguration;
import com.spotifyxp.dialogs.LoginDialog;
import com.spotifyxp.events.EventSubscriber;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.logging.ConsoleLogging;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class PlayerUtils {
    Session authViaZeroconf(Session.Configuration configuration, EventSubscriber cancelCallback) throws InterruptedException, ExecutionException {
        CompletableFuture<Session> sessionFuture = new CompletableFuture<>();
        try (ZeroconfServer zeroconfServer = new ZeroconfServer.Builder(configuration)
                .setPreferredLocale(PublicValues.config.getString(ConfigValues.other_preferredlocale.name))
                .setDeviceType(Connect.DeviceType.COMPUTER)
                .setDeviceName(PublicValues.deviceName)
                .setDeviceId(Utils.randomHexString(new SecureRandom(), 40).toLowerCase())
                .setCancelCallback(cancelCallback)
                .setListenAll(true).create()) {
            zeroconfServer.addSessionListener(new ZeroconfServer.SessionListener() {
                @Override
                public void sessionClosing(@NotNull Session var1) {
                    ConsoleLogging.warning("sessionClosing in zeroconf server! Unimplemented");
                }

                @Override
                public void sessionChanged(@NotNull Session var1) {
                    sessionFuture.complete(var1);
                    try {
                        zeroconfServer.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void cancelled() {
                    sessionFuture.complete(null);
                }
            });
            synchronized (sessionFuture) {
                return sessionFuture.get();
            }
        } catch (IOException e) {
            ConsoleLogging.Throwable(e);
            GraphicalMessage.sorryError("Failed to build player");
            System.exit(0);
        }
        return null;
    }

    Session authViaOauth(Session.Configuration configuration, OAuth.CallbackURLReceiver receiver, EventSubscriber onCancelCallback) throws Session.SpotifyAuthenticationException, GeneralSecurityException, IOException, MercuryClient.MercuryException, CancellationException {
        return new Session.Builder(configuration)
                .setPreferredLocale(PublicValues.config.getString(ConfigValues.other_preferredlocale.name))
                .setDeviceType(Connect.DeviceType.COMPUTER)
                .setDeviceName(PublicValues.deviceName)
                .setDeviceId(Utils.randomHexString(new SecureRandom(), 40).toLowerCase())
                .oauth(receiver, onCancelCallback)
                .create();
    }

    Session authViaStored(Session.Configuration configuration) throws IOException, Session.SpotifyAuthenticationException, GeneralSecurityException, MercuryClient.MercuryException {
        return new Session.Builder(configuration)
                .setPreferredLocale(PublicValues.config.getString(ConfigValues.other_preferredlocale.name))
                .setDeviceType(Connect.DeviceType.COMPUTER)
                .setDeviceName(PublicValues.deviceName)
                .setDeviceId(Utils.randomHexString(new SecureRandom(), 40).toLowerCase())
                .stored(new File(PublicValues.fileslocation, "credentials.json"))
                .create();
    }

    public Player buildPlayer() {
        PlayerConfiguration playerconfig = new PlayerConfiguration.Builder()
                .setAutoplayEnabled(PublicValues.config.getBoolean(ConfigValues.other_autoplayenabled.name))
                .setCrossfadeDuration(PublicValues.config.getInt(ConfigValues.other_crossfadeduration.name))
                .setEnableNormalisation(PublicValues.config.getBoolean(ConfigValues.other_enablenormalization.name))
                .setInitialVolume(65536)
                .setLogAvailableMixers(true)
                .setMetadataPipe(new File(PublicValues.fileslocation, "metapipe"))
                .setMixerSearchKeywords(PublicValues.config.getString(ConfigValues.other_mixersearchkeywords.name).split(","))
                .setNormalisationPregain(PublicValues.config.getInt(ConfigValues.other_normalizationpregain.name))
                .setOutput(PlayerConfiguration.AudioOutput.MIXER)
                .setOutputClass("")
                .setOutputPipe(new File(PublicValues.fileslocation, "outputpipe"))
                .setPreferredQuality(AudioQuality.valueOf(PublicValues.config.getString(ConfigValues.audioquality.name)))
                .setPreloadEnabled(PublicValues.config.getBoolean(ConfigValues.other_preloadenabled.name))
                .setReleaseLineDelay(PublicValues.config.getInt(ConfigValues.other_releaselinedelay.name))
                .setVolumeSteps(64)
                .setBypassSinkVolume(PublicValues.config.getBoolean(ConfigValues.other_bypasssinkvolume.name))
                .setLocalFilesPath(new File(PublicValues.fileslocation))
                .build();
        Session.Configuration.Builder configurationBuilder = new Session.Configuration.Builder()
                .setCacheDir(new File(PublicValues.fileslocation, "cache"))
                .setStoredCredentialsFile(new File(PublicValues.fileslocation, "credentials.json"));
        if(PublicValues.config.getBoolean(ConfigValues.cache_disabled.name)) {
            if(new File(PublicValues.fileslocation, "cache").exists()) {
                FileUtils.deleteDir(new File(PublicValues.fileslocation, "cache"));
            }
            configurationBuilder.setCacheEnabled(false);
        }
        Session.Configuration configuration = configurationBuilder.build();
        try {
            Session session;
            if (new File(PublicValues.fileslocation, "credentials.json").exists()) {
               session = authViaStored(configuration);
            } else {
                session = authenticate(configuration);
            }
            Player player = new Player(playerconfig, session);
            PublicValues.session = session;
            Events.subscribe(SpotifyXPEvents.internetConnectionDropped.getName(), connectionDroppedListener());
            Events.subscribe(SpotifyXPEvents.internetConnectionReconnected.getName(), connectionReconnectedListener());
            return player;
        } catch (ConnectException | Session.SpotifyAuthenticationException | IllegalArgumentException e) {
            ConsoleLogging.Throwable(e);
            return buildPlayer();
        } catch (UnknownHostException offline) {
            GraphicalMessage.sorryErrorExit("No internet connection!");
        } catch (Exception e) {
            ConsoleLogging.Throwable(e);
            GraphicalMessage.sorryError("Failed to build player");
            System.exit(0);
        }
        return null;
    }

    Session authenticate(Session.Configuration configuration) throws ExecutionException, InterruptedException {
        CompletableFuture<Session> sessionFuture = new CompletableFuture<>();
        final Runnable[] cancelRunnable = new Runnable[1];
        LoginDialog.open(
                data -> {
                    cancelRunnable[0].run();
                },
                data -> {
                    Thread zeroconfthread = new Thread(() -> {
                        try {
                            Session session = authViaZeroconf(configuration, data2 -> {
                                cancelRunnable[0] = (Runnable) data2[0];
                            });
                            sessionFuture.complete(session);
                        }catch (Exception e) {
                            sessionFuture.completeExceptionally(e);
                        }
                    });
                    zeroconfthread.start();
                },
                data -> {
                    cancelRunnable[0].run();
                },
                data -> {
                    Thread oauthThread = new Thread(() -> {
                        try {
                            Session session = authViaOauth(configuration, callbackURL ->  {
                                ((EventSubscriber) data[0]).run(callbackURL);
                            }, data1 -> cancelRunnable[0] = (Runnable) data1[0]);
                            sessionFuture.complete(session);
                        }catch (Exception e) {
                            sessionFuture.completeExceptionally(e);
                        }
                    });
                    oauthThread.start();
                }
        );
        synchronized (sessionFuture) {
            Session session = sessionFuture.get();
            if(session == null) {
                return authenticate(configuration);
            }
            LoginDialog.close();
            return sessionFuture.get();
        }
    }

    EventSubscriber connectionReconnectedListener() {
        return data -> {
            PublicValues.wasOffline = false;
            System.out.println("Connection re established!");
        };
    }

    EventSubscriber connectionDroppedListener() {
        return data -> {
            PublicValues.wasOffline = true;
            System.out.println("Connection dropped! Reconnecting...");
        };
    }
}
