/*
 * Copyright [2024-2025] [Gianluca Beil]
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

import com.spotifyxp.PublicValues;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.deps.com.spotify.context.ContextTrackOuterClass;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Track;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.swingextension.DDReorderList;
import com.spotifyxp.utils.TrackUtils;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;

public class Queue extends JScrollPane implements View {
    public static DDReorderList<String> queueList;
    public static final ArrayList<String> queueUriCache = new ArrayList<>();
    public static final DefaultListModel<String> queueListModel = new DefaultListModel<>();
    public static ContextMenu contextMenu;


    //ToDo: Implement the reordering of the queue after dragging
    public Queue() throws IOException {
        setVisible(false);

        queueList = new DDReorderList<>(queueListModel);
        // ToDo: Fix multiple firing of onReordered when dragged
        queueList.setOnReordered((newIndex, oldIndex, name) -> {
            String uri = queueUriCache.get(oldIndex);
            queueUriCache.remove(oldIndex);
            queueUriCache.add(newIndex, uri);
            try {
                InstanceManager.getPlayer().getPlayer().clearQueue();
            } catch (Exception exc) {
                ConsoleLogging.warning("Couldn't queue tracks");
                ConsoleLogging.Throwable(exc);
                return;
            }
            ArrayList<ContextTrackOuterClass.ContextTrack> tracks = new ArrayList<>();
            for(String s : queueUriCache) {
                tracks.add(ContextTrackOuterClass.ContextTrack.newBuilder().setUri(s).build());
            }
            InstanceManager.getPlayer().getPlayer().setQueue(tracks);
        });
        queueList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    int selectedIndex = queueList.getSelectedIndex();
                    ((DefaultListModel<String>) queueList.getModel()).removeRange(0, selectedIndex);
                    String uri = queueUriCache.get(selectedIndex);
                    queueUriCache.subList(0, selectedIndex).clear();
                    InstanceManager.getPlayer().getPlayer().load(uri, true, PublicValues.shuffle);
                    InstanceManager.getPlayer().getPlayer().clearQueue();
                    ArrayList<ContextTrackOuterClass.ContextTrack> tracks = new ArrayList<>();
                    for(String s : queueUriCache) {
                        tracks.add(ContextTrackOuterClass.ContextTrack.newBuilder().setUri(s).build());
                    }
                    InstanceManager.getPlayer().getPlayer().setQueue(tracks);
                }
            }
        });
        queueList.setForeground(PublicValues.globalFontColor);
        queueList.setBackground(getBackground());

        setViewportView(queueList);
        Events.subscribe(SpotifyXPEvents.addtoqueue.getName(), data -> {
            if(!queueUriCache.isEmpty()) {
                queueUriCache.add((String) data[0]);
                try {
                    Track track = InstanceManager.getSpotifyApi().getTrack(((String) data[0]).split(":")[2]).build().execute();
                    String a = TrackUtils.getArtists(track.getArtists());
                    queueListModel.addElement(track.getName() + " - " + a);
                } catch (ArrayIndexOutOfBoundsException e) {
                    // This happens when (psst... i dont know)
                } catch (Exception e) {
                    throw new RuntimeException("Failed to list tracks in queue");
                }
            }
        });
        Events.subscribe(SpotifyXPEvents.queueUpdate.getName(), (Object... data) -> {
            if(queueUriCache.isEmpty()) return;
            if(InstanceManager.getPlayer().getPlayer().tracks(true).next.size() > queueUriCache.size()) {
                queueUriCache.clear();
                ((DefaultListModel<?>) queueList.getModel()).clear();
                try {
                    for (ContextTrackOuterClass.ContextTrack t : InstanceManager.getSpotifyPlayer().tracks(true).next) {
                        Track track = InstanceManager.getSpotifyApi().getTrack(t.getUri().split(":")[2]).build().execute();
                        queueUriCache.add(t.getUri());
                        String a = TrackUtils.getArtists(track.getArtists());
                        queueListModel.addElement(track.getName() + " - " + a);
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    // This happens when (psst... i dont know)
                } catch (Exception e) {
                    throw new RuntimeException("Failed to list tracks in queue", e);
                }
                return;
            }
            if (!queueListModel.isEmpty() && !queueUriCache.isEmpty() && data.length == 1 && data[0] != null && (data[0] instanceof String)) {
                if(queueUriCache.get(0).equals(data[0])) {
                    Events.triggerEvent(SpotifyXPEvents.queueAdvance.getName());
                } else {
                    Events.triggerEvent(SpotifyXPEvents.queueRegress.getName());
                }
                return;
            }
            if (queueListModel.isEmpty()) {
                return;
            }
            queueUriCache.clear();
            ((DefaultListModel<?>) queueList.getModel()).clear();
            try {
                for (ContextTrackOuterClass.ContextTrack t : InstanceManager.getSpotifyPlayer().tracks(true).next) {
                    Track track = InstanceManager.getSpotifyApi().getTrack(t.getUri().split(":")[2]).build().execute();
                    queueUriCache.add(t.getUri());
                    String a = TrackUtils.getArtists(track.getArtists());
                    queueListModel.addElement(track.getName() + " - " + a);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // This happens when (psst... i dont know)
            } catch (Exception e) {
                throw new RuntimeException("Failed to list tracks in queue");
            }
        });
        Events.subscribe(SpotifyXPEvents.queueAdvance.getName(), (Object... data) -> {
            if (queueListModel.isEmpty()) {
                return;
            }
            queueUriCache.remove(0);
            queueListModel.remove(0);
        });
        Events.subscribe(SpotifyXPEvents.queueRegress.getName(), (Object... data) -> {
            if (queueListModel.isEmpty()) {
                return;
            }
            queueUriCache.clear();
            ((DefaultListModel<?>) queueList.getModel()).clear();
            try {
                for (ContextTrackOuterClass.ContextTrack t : InstanceManager.getSpotifyPlayer().tracks(true).next) {
                    Track track = InstanceManager.getSpotifyApi().getTrack(t.getUri().split(":")[2]).build().execute();
                    queueUriCache.add(t.getUri());
                    String a = TrackUtils.getArtists(track.getArtists());
                    queueListModel.addElement(track.getName() + " - " + a);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // This happens when (psst... i dont know)
            } catch (Exception e) {
                throw new RuntimeException("Failed to list tracks in queue");
            }
        });

        contextMenu = new ContextMenu(queueList, queueUriCache, getClass());
        contextMenu.addItem("Remove", () -> {
            String uri = queueUriCache.get(queueList.getSelectedIndex());
            ArrayList<ContextTrackOuterClass.ContextTrack> tracks = new ArrayList<>();
            for(ContextTrackOuterClass.ContextTrack track : InstanceManager.getPlayer().getPlayer().tracks(true).next) {
                if(track.hasUri() && track.getUri().equals(uri)) {
                    continue;
                }
                tracks.add(track);
            }
            InstanceManager.getPlayer().getPlayer().setQueue(tracks);
            queueUriCache.remove(queueUriCache.get(queueList.getSelectedIndex()));
            ((DefaultListModel<?>) queueList.getModel()).remove(queueList.getSelectedIndex());
        });
    }

    @Override
    public void makeVisible() {
        if (Queue.queueListModel.isEmpty()) {
            ((DefaultListModel<?>) Queue.queueList.getModel()).removeAllElements();
            Queue.queueUriCache.clear();
            Thread queueworker = new Thread(() -> {
                try {
                    for (ContextTrackOuterClass.ContextTrack track : InstanceManager.getPlayer().getPlayer().tracks(true).next) {
                        Track t = InstanceManager.getSpotifyApi().getTrack(track.getUri().split(":")[2]).build().execute();
                        String a = TrackUtils.getArtists(t.getArtists());
                        Queue.queueUriCache.add(track.getUri());
                        Queue.queueListModel.addElement(t.getName() + " - " + a);
                    }
                } catch (IOException ex) {
                    ConsoleLogging.Throwable(ex);
                } catch (NullPointerException exc) {
                    // Nothing in queue
                }
            }, "Queue worker (ContentPanel)");
            queueworker.start();
        }
        setVisible(true);
    }

    @Override
    public void makeInvisible() {
        setVisible(false);
    }
}
