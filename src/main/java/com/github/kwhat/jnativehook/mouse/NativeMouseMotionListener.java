/* JNativeHook: Global keyboard and mouse listeners for Java.
 * Copyright (C) 2006-2021 Alexander Barker.  All Rights Reserved.
 * https://github.com/kwhat/jnativehook/
 *
 * JNativeHook is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JNativeHook is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.kwhat.jnativehook.mouse;

import com.github.kwhat.jnativehook.GlobalScreen;

import java.util.EventListener;

/**
 * The listener interface for receiving native mouse motion events. (For clicks and other mouse
 * events, use the <code>NativeMouseListener</code>.)
 * <p>
 * <p>
 * The class that is interested in processing a <code>NativeMouseMotionEvent</code> implements this
 * interface, and the object created with that class is registered with the
 * <code>GlobalScreen</code> using the {@link GlobalScreen#addNativeMouseMotionListener} method.
 * When the NativeMouseMotion event occurs, that object's appropriate method is invoked.
 *
 * @author Alexander Barker (<a href="mailto:alex@1stleg.com">alex@1stleg.com</a>)
 * @version 2.0
 * @see NativeMouseEvent
 * @since 1.0
 */
public interface NativeMouseMotionListener extends EventListener {

    /**
     * Invoked when the mouse has been moved.
     *
     * @param nativeEvent the native mouse event.
     */
    default void nativeMouseMoved(NativeMouseEvent nativeEvent) {
    }

    /**
     * Invoked when the mouse has been moved while a button is depressed.
     *
     * @param nativeEvent the native mouse event
     * @since 1.1
     */
    default void nativeMouseDragged(NativeMouseEvent nativeEvent) {
    }
}
