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
package com.spotifyxp.guielements;

import com.spotifyxp.utils.RunnableQueue;

import javax.swing.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DefTable extends JTable {
    final RunnableQueue queue = new RunnableQueue(new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()));

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public int getSelectedRow() {
        if (getSelectedRowCount() > 1) {
            return super.getSelectedRows()[0];
        }
        return super.getSelectedRow();
    }

    public void addModifyAction(Runnable runnable) {
        queue.enqueue(runnable);
    }
}
