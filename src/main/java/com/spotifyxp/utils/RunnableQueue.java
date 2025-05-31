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
Created from Nicolas Repiquet

Date: 3.11.2010

https://stackoverflow.com/questions/4086108/java-runnable-queue
 */

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

public final class RunnableQueue {

    private final ExecutorService m_executorService;
    private final Queue<Runnable> m_runnables;
    private final Runnable m_loop;

    public RunnableQueue(ExecutorService executorService) {
        m_executorService = executorService;
        m_runnables = new LinkedList<>();

        m_loop = () -> {

            Runnable l_runnable = current();

            while (l_runnable != null) {
                l_runnable.run();
                l_runnable = next();
            }
        };
    }

    private Runnable current() {
        synchronized (m_runnables) {
            return m_runnables.peek();
        }
    }

    private Runnable next() {
        synchronized (m_runnables) {
            m_runnables.remove();
            return m_runnables.peek();
        }
    }

    public void enqueue(Runnable runnable) {
        if (runnable != null) {
            synchronized (m_runnables) {
                m_runnables.add(runnable);
                if (m_runnables.size() == 1) {
                    m_executorService.execute(m_loop);
                }
            }
        }
    }
}
