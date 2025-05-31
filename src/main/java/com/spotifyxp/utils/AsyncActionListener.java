/*
 * Copyright [2024-2025] [Gianluca Beil]
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AsyncActionListener implements ActionListener {
    private final ActionListener listener;

    public AsyncActionListener(ActionListener listener) {
        this.listener = listener;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Thread thread = new Thread(() -> listener.actionPerformed(e));
        thread.start();
    }
}
