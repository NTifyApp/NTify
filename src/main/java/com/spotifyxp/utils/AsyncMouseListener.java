/*
 * Copyright [2024] [Gianluca Beil]
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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class AsyncMouseListener implements MouseListener {
    private final MouseListener listener;

    public AsyncMouseListener(MouseListener listener) {
        this.listener = listener;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Thread thread = new Thread(() -> listener.mouseClicked(e), "AsyncMouseListener clicked");
        thread.start();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Thread thread = new Thread(() -> listener.mousePressed(e), "AsyncMouseListener pressed");
        thread.start();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Thread thread = new Thread(() -> listener.mouseReleased(e), "AsyncMouseListener released");
        thread.start();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        Thread thread = new Thread(() -> listener.mouseEntered(e));
        thread.start();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        Thread thread = new Thread(() -> listener.mouseEntered(e));
        thread.start();
    }
}
