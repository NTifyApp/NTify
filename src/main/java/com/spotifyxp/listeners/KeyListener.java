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
package com.spotifyxp.listeners;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;

@SuppressWarnings("CanBeFinal")
public class KeyListener {
    public static boolean playpausepressed = false;
    public static boolean nextpressed = false;
    public static boolean previouspressed = false;

    /**
     * Starts a key listener
     * <br> Listens for playpause, previous and next
     */
    public void start() {
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
                @Override
                public void nativeKeyPressed(NativeKeyEvent nativeEvent) {
                    switch (nativeEvent.getKeyCode()) {
                        case NativeKeyEvent.VC_MEDIA_PLAY:
                            //PlayPause
                            playpausepressed = true;
                            InstanceManager.getSpotifyPlayer().playPause();
                            break;
                        case NativeKeyEvent.VC_MEDIA_NEXT:
                            //Next
                            nextpressed = true;
                            InstanceManager.getSpotifyPlayer().next();
                            break;
                        case NativeKeyEvent.VC_MEDIA_PREVIOUS:
                            //Previous
                            previouspressed = true;
                            InstanceManager.getSpotifyPlayer().previous();
                            break;
                    }
                }

                @Override
                public void nativeKeyReleased(NativeKeyEvent nativeEvent) {
                    switch (nativeEvent.getKeyCode()) {
                        case NativeKeyEvent.VC_MEDIA_PLAY:
                            //PlayPause
                            playpausepressed = false;
                            break;
                        case NativeKeyEvent.VC_MEDIA_NEXT:
                            //Next
                            nextpressed = false;
                            break;
                        case NativeKeyEvent.VC_MEDIA_PREVIOUS:
                            //Previous
                            previouspressed = false;
                            break;
                    }
                }
            });
        } catch (Exception ex) {
            try {
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException e) {
                throw new RuntimeException(e);
            }
            ConsoleLogging.Throwable(ex);
        }
    }
}
