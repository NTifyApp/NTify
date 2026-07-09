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

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared bounded thread pool for backgrounding blocking work triggered from UI listeners
 * (network/disk/DB calls). Replaces spawning a new raw Thread per UI event.
 */
public class AsyncUtils {
    private static final AtomicInteger threadCount = new AtomicInteger(0);

    private static final ThreadFactory threadFactory = r -> {
        Thread t = new Thread(r, "AsyncUtils-worker-" + threadCount.incrementAndGet());
        t.setDaemon(true);
        return t;
    };

    private static final ExecutorService pool = Executors.newCachedThreadPool(threadFactory);

    private AsyncUtils() {
    }

    public static void run(Runnable task) {
        pool.execute(task);
    }

    /**
     * Submits a task to the shared pool and returns a Future for its result - use to fan out
     * independent blocking calls (e.g. one per item in a loop) in parallel instead of awaiting
     * each one sequentially, then collect results with Future#get().
     */
    public static <T> Future<T> submit(Callable<T> task) {
        return pool.submit(task);
    }
}
