/*
 * Copyright [2024-2026] [Gianluca Beil]
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

/**
 * Represents a subscriber of an event
 * @param <T> Data of the event
 * @implNote Data Types {@link T} of {@link Object} represents that the event has no data
 */
@FunctionalInterface
public interface EventSubscriber<T> {
    void run(T data);
}
