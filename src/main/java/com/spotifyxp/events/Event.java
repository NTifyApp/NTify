/*
 * Copyright [2026] [Gianluca Beil]
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

import com.spotifyxp.logging.ConsoleLogging;

import java.util.ArrayList;

/**
 * Class holding an Event
 * @param <T> Type of the event payload
 * @implNote Data Types {@link T} of {@link Object} represents that the event has no data
 */
public class Event<T> {
    private final ArrayList<EventSubscriber<T>> subscribers = new ArrayList<>();

    /**
     * Subscribe to the event
     * @param subscriber Subscriber to add
     */
    public void subscribe(EventSubscriber<T> subscriber) {
        this.subscribers.add(subscriber);
    }

    /**
     * Unsubscribe from the event
     * @param subscriber Subscriber to remove
     */
    public void unsubscribe(EventSubscriber<T> subscriber) {
        this.subscribers.remove(subscriber);
    }

    /**
     * Triggers the event with the given data
     * @param data The event payload
     */
    public void trigger(T data) {
        for (EventSubscriber<T> subscriber : new ArrayList<>(subscribers)) {
            try {
                subscriber.run(data);
            }catch (Throwable e) {
                ConsoleLogging.Throwable(e);
            }
        }
    }

    /**
     * Triggers the event with no data
     */
    public void trigger() {
        this.trigger(null);
    }
}