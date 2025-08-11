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


import com.spotifyxp.logging.ConsoleLoggingModules;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


/**
 * Simple worker thread that processes tasks sequentially
 *
 * @param <REQ> The type of task/input that AsyncProcessor handles.
 * @param <RES> Return type of our processor implementation
 */
public class AsyncProcessor<REQ, RES> implements Closeable {
    private final String name;
    private final Function<REQ, RES> processor;
    private final ExecutorService executor;

    /**
     * @param name      name of async processor - used for thread name and logging
     * @param processor actual processing implementation ran on background thread
     */
    public AsyncProcessor(@NotNull String name, @NotNull Function<REQ, RES> processor) {
        executor = SharedSchedulers.scheduler();
        this.name = name;
        this.processor = processor;
        ConsoleLoggingModules.debug("AsyncProcessor{{}} has started", name);
    }

    public Future<RES> submit(@NotNull REQ task) {
        return executor.submit(() -> processor.apply(task));
    }

    public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        if (!executor.isShutdown())
            throw new IllegalStateException(String.format("AsyncProcessor{%s} hasn't been shut down yet", name));

        if (executor.awaitTermination(timeout, unit)) {
            ConsoleLoggingModules.debug("AsyncProcessor{{}} is shut down", name);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void close() {
        ConsoleLoggingModules.debug("AsyncProcessor{{}} is shutting down", name);
        executor.shutdown();
    }
}
