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
package com.github.kwhat.jnativehook.keyboard;

import com.github.kwhat.jnativehook.GlobalScreen;

import java.util.EventListener;

/**
 * The listener interface for receiving global <code>NativeKeyEvents</code>.
 * <p>
 * <p>
 * The class that is interested in processing a <code>NativeKeyEvent</code> implements this
 * interface, and the object created with that class is registered with the
 * <code>GlobalScreen</code> using the {@link GlobalScreen#addNativeKeyListener(NativeKeyListener)}
 * method. When the
 * <code>NativeKeyEvent</code> occurs, that object's appropriate method is
 * invoked.
 *
 * @author Alexander Barker (<a href="mailto:alex@1stleg.com">alex@1stleg.com</a>)
 * @version 2.0
 * @see NativeKeyEvent
 * @since 1.0
 */
public interface NativeKeyListener extends EventListener {

    /**
     * Invoked when a key has been typed.
     *
     * @param nativeEvent the native key event.
     * @since 1.1
     */
    default void nativeKeyTyped(NativeKeyEvent nativeEvent) {
    }

    /**
     * Invoked when a key has been pressed.
     *
     * @param nativeEvent the native key event.
     */
    default void nativeKeyPressed(NativeKeyEvent nativeEvent) {
    }

    /**
     * Invoked when a key has been released.
     *
     * @param nativeEvent the native key event.
     */
    default void nativeKeyReleased(NativeKeyEvent nativeEvent) {
    }
}
