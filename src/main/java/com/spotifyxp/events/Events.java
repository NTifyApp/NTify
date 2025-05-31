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
package com.spotifyxp.events;

import com.spotifyxp.PublicValues;
import com.spotifyxp.logging.ConsoleLogging;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Events {
    private static class PrivateEvent {
        public final String name;
        public final boolean isProtected;

        public PrivateEvent(String name, boolean isProtected) {
            this.name = name;
            this.isProtected = isProtected;
        }

        public ArrayList<EventSubscriber> subscribers = new ArrayList<>();

        public void trigger(Object... data) {
            for (EventSubscriber subscriber : new ArrayList<>(subscribers)) {
                try {
                    subscriber.run(data);
                }catch (Exception e) {
                    ConsoleLogging.Throwable(e);
                }
            }
        }
    }

    public static class Event {
        private final String name;
        private final boolean isProtected;

        public Event(String name, boolean isProtected, ArrayList<EventSubscriber> subscribers) {
            this.name = name;
            this.isProtected = isProtected;
            this.subscribers = subscribers;
        }

        private final ArrayList<EventSubscriber> subscribers;

        public ArrayList<EventSubscriber> getSubscribers() {
            return subscribers;
        }

        public String getName() {
            return name;
        }

        public boolean isProtected() {
            return isProtected;
        }
    }

    private static final boolean debug = PublicValues.debug;
    private static final ArrayList<PrivateEvent> registeredEvents = new ArrayList<>();

    private static boolean containsEventWithName(String name) {
        for (PrivateEvent e : registeredEvents) {
            if (e.name.equals(name)) return true;
        }
        return false;
    }

    private static PrivateEvent getEventWithName(String name) {
        for (PrivateEvent e : registeredEvents) {
            if (e.name.equals(name)) return e;
        }
        return null;
    }

    private static int getArrayNumberOfEventWithName(String name) {
        for (int i = 0; i < registeredEvents.size(); i++) {
            if (registeredEvents.get(i).name.equals(name)) return i;
        }
        return -1;
    }

    private static Event convertPrivateToPublic(PrivateEvent event) {
        return new Event(event.name, event.isProtected, event.subscribers);
    }

    public static void register(String name, boolean isProtected) {
        if (debug) ConsoleLogging.debug("[Events] Registering event with name:" + name + " protected:" + isProtected);
        if (containsEventWithName(name)) return;
        registeredEvents.add(new PrivateEvent(name, isProtected));
    }

    public static void unregister(String name) {
        if (debug) ConsoleLogging.debug("[Events] Unregistering event with name:" + name);
        if (Objects.requireNonNull(getEventWithName(name)).isProtected) {
            ConsoleLogging.error("[Events] Illegal operation performed! Tried to remove protected event");
            return;
        }
        int result = getArrayNumberOfEventWithName(name);
        if (result == -1) return;
        registeredEvents.remove(result);
    }

    public static void subscribe(String name, EventSubscriber runnable) {
        if (debug) ConsoleLogging.debug("[Events] Subscribing to " + name);
        Objects.requireNonNull(getEventWithName(name)).subscribers.add(runnable);
    }

    public static void unsubscribe(String name, EventSubscriber runnable) {
        if (debug) ConsoleLogging.debug("[Events] Unsubscribing from " + name);
        Objects.requireNonNull(getEventWithName(name)).subscribers.remove(runnable);
    }

    public static ArrayList<Event> getEventsList() {
        ArrayList<Event> events = new ArrayList<>();
        for (PrivateEvent e : registeredEvents) {
            events.add(convertPrivateToPublic(e));
        }
        return events;
    }

    public static void triggerEvent(String name, Object... data) {
        if (debug) ConsoleLogging.debug("[Events] Triggering event:" + name);
        Objects.requireNonNull(getEventWithName(name)).trigger(data);
    }

    public static void triggerEventButWait(String name, CompletableFuture<Boolean> waitFor, Object... data) {
        Thread waitForThread = new Thread(() -> {
            try {
                waitFor.get();
                triggerEvent(name, data);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }, "Wait for future (Events)");
        waitForThread.start();
    }
}
