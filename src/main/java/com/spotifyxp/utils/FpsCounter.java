/*
 * Copyright [2023-2024] [Gianluca Beil]
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

/*
 * Copyright (c) 2006 Michael Nischt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * Neither the name of the project's author nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import java.util.ArrayList;
import java.util.EventListener;

/**
 * A utility class that can be used to compute the average frames per second.
 *
 * @author Michael Nischt
 * @version 1.0
 */
public final class FpsCounter {

    /**
     * An event object which contains the actual, average or aggregated, frames per second.
     */
    public static final class Event extends java.util.EventObject {

        /**
         * Creates a new instance of FPSCounterEvent
         *
         * @param source the FPSCounter that originated the event
         */
        public Event(FpsCounter source) {
            super(source);
            this.averageFps = source.getAverageFps();
            this.aggregateFps = source.getAggregateFps();
        }

        /**
         * Returns the object that originated the event.
         *
         * @return the object that originated the event
         */
        public FpsCounter getFPSCounter() {
            return (FpsCounter) this.source;
        }

        /**
         * Returns the average frames per second based on the FPSCounter's frames per second count.
         *
         * @return the average frames per second.
         */
        public double getAverageFps() {
            return this.averageFps;
        }

        /**
         * Returns the aggregated average frames per second based on the FPSCounter's frames per second count.
         *
         * @return the aggregated average frames per second
         */
        public double getAggregateFps() {
            return this.aggregateFps;
        }

        private final double averageFps;
        private final double aggregateFps;
    }

    /**
     * The listener interface for receiving FPSCounter events. Classes that are interested in the average or aggregated
     * FPS can implement this interface to receive events.
     */
    public interface Listener extends EventListener {

        /**
         * Invoked by the FPSCounter when the nextFrame method is called.
         *
         * @param e The FPSCounter event, which can be used to retrieve the average/aggregated frames per second.
         */
        void averageFramesElapsed(Event e);
    }

    /**
     * Creates a new instance of FPSCounter, which computes the average or the aggregate frame-rate based on 100 frames.
     */
    public FpsCounter() {
    }

    /**
     * Creates a new instance of FPSCounter with the specified number of frames for which the average or the aggregate
     * frame-rate is computed.
     *
     * @param avgFrameCount The number of frames for which the average or the aggregate frame-rate is computed.
     */
    public FpsCounter(int avgFrameCount) {
        this.setAverageFrameCount(avgFrameCount);
    }

    /**
     * Gets the number of frames for which the average or the aggregate frame-rate is computed.
     *
     * @return The number of frames for which the average or the aggregate frame-rate is computed.
     */
    public int getAverageFrameCount() {
        return averageFrameCount;
    }

    /**
     * Sets the number of frames for which the average or the aggregate frame-rate is computed.
     *
     * @param avgFrameCount The number of frames for which the average or the aggregate frame-rate is computed.
     */
    public void setAverageFrameCount(int avgFrameCount) {
        this.averageFrameCount = avgFrameCount;
    }

    /**
     * Return the average frames per second.
     *
     * @return the average frames per second
     */
    public double getAverageFps() {
        return this.avgFps;
    }

    /**
     * Returns the average aggregated frames per second.
     *
     * @return the average aggregated frames per second
     */
    public double getAggregateFps() {
        return this.aggFps;
    }

    /**
     * Adds the specified FPSCounter listener to receive events when nextFrame method is called.
     *
     * @param l the FPSCounter listener
     */
    public void addFPSCounterListener(Listener l) {
        this.listeners.add(l);
    }

    /**
     * Removes the specified FPSCounter listener so that it no longer receives events when nextFrame method is called.
     *
     * @param l the FPSCounter listener
     */
    public void removeFPSCounterListener(Listener l) {
        this.listeners.remove(l);
    }

    /**
     * Returns an array of all registered FPSCounter listeners.
     *
     * @return all of the registered FPSCounter listeners or an empty array if no listeners are currently registered
     */
    public Listener[] getFPSCounterListeners() {
        return this.listeners.toArray(new Listener[0]);
    }

    /**
     * (Re-)starts the counter.
     */
    public void start() {
        this.started = true;

        this.frameCount = 0;
        this.passedTime = 0;
        this.fpsSum = 0.0;

        this.lastTime = System.nanoTime();
    }

    /**
     * Stops the counter
     */
    public void stop() {
        this.started = false;

        this.frameCount = 0;
        this.passedTime = 0;
        this.fpsSum = 0.0;
    }

    /**
     * Advances to the next frame and measures the time between the last call.
     *
     * @return The number of frames per Second based only on the time passed for the single frame between the last call.
     */
    public double nextFrame() {
        if (!this.started) {
            this.start();
        }
        return this.nextFrame(System.nanoTime() - this.lastTime);
    }

    private double nextFrame(long deltaNanos) {
        this.frameCount++;
        this.passedTime += deltaNanos;

        this.lastTime = System.nanoTime();
        final double fps = 1.e9 / (double) deltaNanos;
        this.fpsSum += fps;

        if (frameCount >= getAverageFrameCount()) {
            this.aggFps = (double) this.frameCount / (this.passedTime / 1.e9);
            this.avgFps = this.fpsSum / (double) this.frameCount;
            Event event = new Event(this);
            for (Listener listener : this.listeners) {
                listener.averageFramesElapsed(event);
            }
            this.frameCount = 0;
            this.passedTime = 0;
            this.fpsSum = 0.0;
        }

        return fps;
    }

    private int averageFrameCount = 100; // Report frame rate after maxframe number of frames have been rendered
    private boolean started = true; // flag to indicate the counter has been started
    private long lastTime;          // last system nanos measured
    private long frameCount;        // elapsed frames since the last averageFrameCount occurence
    private double passedTime;      // measured nanos since the last averageFrameCount occurence
    private double fpsSum;
    private double aggFps, avgFps;
    private final ArrayList<Listener> listeners = new ArrayList<>();
}