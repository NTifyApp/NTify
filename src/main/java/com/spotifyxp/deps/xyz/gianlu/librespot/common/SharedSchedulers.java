/*
 * Copyright 2021 devgianlu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.spotifyxp.deps.xyz.gianlu.librespot.common;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SharedSchedulers {
    private static final ScheduledThreadPoolExecutor SCHEDULER;

    static {
        SCHEDULER = new ScheduledThreadPoolExecutor(
                4,
                new CustomThreadFactory("librespot-scheduler")
        );
        SCHEDULER.setRemoveOnCancelPolicy(true);
        SCHEDULER.setKeepAliveTime(60, TimeUnit.SECONDS);
        SCHEDULER.allowCoreThreadTimeOut(true);
    }

    private SharedSchedulers() {
    }

    public static ScheduledExecutorService scheduler() {
        return SCHEDULER;
    }

    private static class CustomThreadFactory implements ThreadFactory {
        private final String baseName;
        private final AtomicInteger count = new AtomicInteger(0);

        public CustomThreadFactory(String baseName) {
            this.baseName = baseName;
        }

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread t = new Thread(r, baseName + "-" + count.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
