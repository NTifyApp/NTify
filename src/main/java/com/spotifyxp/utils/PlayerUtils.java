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
package com.spotifyxp.utils;

import com.spotify.connectstate.Connect;
import com.spotifyxp.PublicValues;
import com.spotifyxp.dialogs.LoginDialog;
import com.spotifyxp.logging.ConsoleLogging;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.gianlu.librespot.ZeroconfServer;
import xyz.gianlu.librespot.audio.decoders.AudioQuality;
import xyz.gianlu.librespot.audio.decoders.Decoders;
import xyz.gianlu.librespot.audio.decoders.Mp3Decoder;
import xyz.gianlu.librespot.audio.decoders.VorbisDecoder;
import xyz.gianlu.librespot.audio.format.SuperAudioFormat;
import xyz.gianlu.librespot.common.Utils;
import xyz.gianlu.librespot.core.OAuth;
import xyz.gianlu.librespot.core.Session;
import xyz.gianlu.librespot.core.TokenProvider;
import xyz.gianlu.librespot.player.Player;
import xyz.gianlu.librespot.player.PlayerConfiguration;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PlayerUtils {
    private static LoginDialog loginDialog;

    Session authViaZeroconf(Session.Configuration configuration, ZeroconfServer.CancelCallback cancelCallback) throws InterruptedException, ExecutionException {
        CompletableFuture<Session> sessionFuture = new CompletableFuture<>();
        try (ZeroconfServer zeroconfServer = new ZeroconfServer.Builder(configuration)
                .setPreferredLocale(PublicValues.config.getFields().preferredLocale)
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

    Session authViaOauth(Session.Configuration configuration, OAuth.CallbackURLReceiver receiver, OAuth.CancelCallback onCancelCallback) throws Session.SpotifyAuthenticationException, GeneralSecurityException, IOException, TokenProvider.TokenException, CancellationException {
        return new Session.Builder(configuration)
                .setPreferredLocale(PublicValues.config.getFields().preferredLocale)
                .setDeviceType(Connect.DeviceType.COMPUTER)
                .setDeviceName(PublicValues.deviceName)
                .setDeviceId(Utils.randomHexString(new SecureRandom(), 40).toLowerCase())
                .oauth(receiver, onCancelCallback)
                .create();
    }

    Session authViaStored(Session.Configuration configuration) throws IOException, Session.SpotifyAuthenticationException, GeneralSecurityException, TokenProvider.TokenException {
        return new Session.Builder(configuration)
                .setPreferredLocale(PublicValues.config.getFields().preferredLocale)
                .setDeviceType(Connect.DeviceType.COMPUTER)
                .setDeviceName(PublicValues.deviceName)
                .setDeviceId(Utils.randomHexString(new SecureRandom(), 40).toLowerCase())
                .stored(new File(PublicValues.fileslocation, "credentials.json"))
                .create();
    }

    public Player buildPlayer() {
        PlayerConfiguration playerconfig = new PlayerConfiguration.Builder()
                .setAutoplayEnabled(PublicValues.config.getFields().autoplayEnabled)
                .setCrossfadeDuration(PublicValues.config.getFields().crossfadeDuration)
                .setEnableNormalisation(PublicValues.config.getFields().enableNormalization)
                .setInitialVolume(65536)
                .setLogAvailableMixers(true)
                .setMetadataPipe(new File(PublicValues.fileslocation, "metapipe"))
                .setMixerSearchKeywords(PublicValues.config.getFields().mixerSearchKeywords.split(","))
                .setNormalisationPregain(PublicValues.config.getFields().normalizationPregain)
                .setOutput(PlayerConfiguration.AudioOutput.MIXER)
                .setOutputClass("")
                .setPreferredQuality(AudioQuality.valueOf(PublicValues.config.getFields().audioQuality))
                .setPreloadEnabled(PublicValues.config.getFields().preloadEnabled)
                .setReleaseLineDelay(PublicValues.config.getFields().releaseLineDelay)
                .setVolumeSteps(64)
                .setBypassSinkVolume(PublicValues.config.getFields().bypassSinkVolume)
                .setLocalFilesPath(new File(PublicValues.fileslocation))
                .build();
        Session.Configuration.Builder configurationBuilder = new Session.Configuration.Builder()
                .setConnectionTimeout(6)
                .setOkHttpClient(PublicValues.defaultHttpClient)
                .setCacheDir(new File(PublicValues.fileslocation, "cache"))
                .setStoredCredentialsFile(new File(PublicValues.fileslocation, "credentials.json"));
        if (PublicValues.config.getFields().cacheDisabled) {
            if (new File(PublicValues.fileslocation, "cache").exists()) {
                FileUtils.deleteDir(new File(PublicValues.fileslocation, "cache"));
            }
            configurationBuilder.setCacheEnabled(false);
        }
        if (PublicValues.config.getFields().enableProxy) {
            configurationBuilder.setProxyEnabled(PublicValues.config.getFields().enableProxy);
            configurationBuilder.setProxyAddress(PublicValues.config.getFields().proxyAddress.split(":")[0]);
            configurationBuilder.setProxyPort(Integer.parseInt(PublicValues.config.getFields().proxyAddress.split(":")[1]));
            configurationBuilder.setProxyAuth(!PublicValues.config.getFields().proxyPassword.isEmpty() || !PublicValues.config.getFields().proxyUsername.isEmpty());
            configurationBuilder.setProxyUsername(PublicValues.config.getFields().proxyUsername);
            configurationBuilder.setProxyPassword(PublicValues.config.getFields().proxyPassword);
            configurationBuilder.setProxyType(Proxy.Type.valueOf(PublicValues.config.getFields().proxyType));
        }
        Session.Configuration configuration = configurationBuilder.build();
        try {
            Session session;
            if (new File(PublicValues.fileslocation, "credentials.json").exists()) {
                session = authViaStored(configuration);
            } else {
                session = authenticate(configuration);
            }

            Decoders.unregisterDecoder(VorbisDecoder.class);
            Decoders.unregisterDecoder(Mp3Decoder.class);
            Decoders.registerDecoder(SuperAudioFormat.VORBIS, com.spotifyxp.audio.VorbisDecoder.class);
            Decoders.registerDecoder(SuperAudioFormat.MP3, com.spotifyxp.audio.Mp3Decoder.class);

            Player player = new Player(playerconfig, session);
            player.waitReady();

            PublicValues.session = session;

            PublicValues.session.addReconnectionListener(connectionListener);
            return player;
        } catch (ConnectException | Session.SpotifyAuthenticationException | IllegalArgumentException |
                 EOFException e) {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(2));
            } catch (InterruptedException ignored) {
            }
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

    Session authenticate(Session.Configuration configuration) throws IOException {
        CompletableFuture<Session> sessionFuture = new CompletableFuture<>();
        if (loginDialog == null) loginDialog = new LoginDialog();
        loginDialog.onZeroconfExecute = data -> {
            Thread zeroconfthread = new Thread(() -> {
                try {
                    Session session = authViaZeroconf(configuration, data2 -> {
                        loginDialog.onZeroconfCancel = data2;
                    });
                    if (session == null) return;
                    sessionFuture.complete(session);
                } catch (Exception e) {
                    sessionFuture.completeExceptionally(e);
                }
            });
            zeroconfthread.start();
        };
        loginDialog.onOauthExecute =  data -> {
            Thread oauthThread = new Thread(() -> {
                try {
                    Session session = authViaOauth(configuration, callbackURL -> {
                        data.run(callbackURL);
                    }, data1 -> loginDialog.onOauthCancel = data1);
                    if (session == null) return;
                    sessionFuture.complete(session);
                } catch (Exception e) {
                    sessionFuture.completeExceptionally(e);
                }
            });
            oauthThread.start();
        };

        if (loginDialog.frame == null)
            loginDialog.open();

        try {
            synchronized (sessionFuture) {
                Session session = sessionFuture.get();
                loginDialog.close();
                return session;
            }
        }catch (ExecutionException | IllegalStateException e) {
            return authenticate(configuration);
        }catch (InterruptedException e) {
            ConsoleLogging.info("User interrupted the login process");
            System.exit(0);
        }

        return null;
    }

    Session.ReconnectionListener connectionListener = new Session.ReconnectionListener() {
        @Override
        public void onConnectionDropped() {
            PublicValues.wasOffline = false;
        }

        @Override
        public void onConnectionEstablished() {
            PublicValues.wasOffline = true;
        }
    };
}
