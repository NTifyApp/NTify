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
package com.spotifyxp.utils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Prevents an action from being re-entered while a previous invocation is still running -
 * e.g. a user rapidly double-clicking two different rows before the first row's table load
 * finishes. Guards against launching a second concurrent modification of the same UI state
 * rather than trying to make concurrent modification itself safe.
 */
public class ReentryGuard {
    private final AtomicBoolean busy = new AtomicBoolean(false);

    /**
     * @return true if the caller acquired the guard and should proceed; false if already busy,
     * meaning the caller must do nothing (the earlier call is still running).
     */
    public boolean tryEnter() {
        return busy.compareAndSet(false, true);
    }

    /**
     * Must be called once the guarded action finishes, including on error paths.
     */
    public void exit() {
        busy.set(false);
    }

    /**
     * Runs the action if not already busy; ignores the call entirely otherwise.
     */
    public void runIfFree(Runnable action) {
        if (!tryEnter()) return;
        try {
            action.run();
        } finally {
            exit();
        }
    }
}
