/*
 * Copyright [2023-2025] [Gianluca Beil]
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

import com.spotifyxp.PublicValues;
import com.spotifyxp.dialogs.ErrorDisplay;
import com.spotifyxp.exception.ExceptionDialog;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.panels.SplashPanel;

import javax.swing.*;

@SuppressWarnings("unused")
public class GraphicalMessage {
    public static void bug(String where) {
        if (SplashPanel.frame.isVisible()) {
            SplashPanel.frame.setAlwaysOnTop(false);
        }
        JOptionPane.showConfirmDialog(ContentPanel.frame, PublicValues.language.translate("ui.graphicalmessage.bug") + where, PublicValues.language.translate("joptionpane.info"), JOptionPane.OK_CANCEL_OPTION);
    }

    public static void sorryError() {
        if (SplashPanel.frame.isVisible()) {
            SplashPanel.frame.setAlwaysOnTop(false);
        }
        JOptionPane.showConfirmDialog(ContentPanel.frame, PublicValues.language.translate("critical.sorry.text"), PublicValues.language.translate("critical.sorry.title"), JOptionPane.OK_CANCEL_OPTION);
    }

    public static void pleaseRestart() {
        if (SplashPanel.frame.isVisible()) {
            SplashPanel.frame.setAlwaysOnTop(false);
        }
        JOptionPane.showConfirmDialog(ContentPanel.frame, PublicValues.language.translate("ui.settings.pleaserestart"), PublicValues.language.translate("joptionpane.info"), JOptionPane.OK_CANCEL_OPTION);
    }

    public static void debug(Object o) {
        if (SplashPanel.frame.isVisible()) {
            SplashPanel.frame.setAlwaysOnTop(false);
        }
        JOptionPane.showMessageDialog(ContentPanel.frame, o.toString(), "Debug", JOptionPane.ERROR_MESSAGE);
    }

    public static void sorryError(String additional) {
        if (SplashPanel.frame.isVisible()) {
            SplashPanel.frame.setAlwaysOnTop(false);
        }
        JOptionPane.showConfirmDialog(ContentPanel.frame, PublicValues.language.translate("critical.sorry.text") + " Additional Info => " + additional, PublicValues.language.translate("critical.sorry.title"), JOptionPane.OK_CANCEL_OPTION);
    }

    public static void sorryErrorExit() {
        if (SplashPanel.frame.isVisible()) {
            SplashPanel.frame.setAlwaysOnTop(false);
        }
        int selection = JOptionPane.showConfirmDialog(ContentPanel.frame, PublicValues.language.translate("critical.sorry.text"), PublicValues.language.translate("critical.sorry.title"), JOptionPane.OK_CANCEL_OPTION);
        if (selection == JOptionPane.CANCEL_OPTION) {
            openException(new UnknownError());
            return;
        }
        System.exit(2);
    }

    public static void sorryErrorExit(String additional) {
        if (SplashPanel.frame.isVisible()) {
            SplashPanel.frame.setAlwaysOnTop(false);
        }
        int selection = JOptionPane.showConfirmDialog(ContentPanel.frame, PublicValues.language.translate("critical.sorry.text") + " Additional Info => " + additional, PublicValues.language.translate("critical.sorry.title"), JOptionPane.OK_CANCEL_OPTION);
        if (selection == JOptionPane.CANCEL_OPTION) {
            openException(new Throwable(additional));
            return;
        }
        System.exit(2);
    }

    public static int showConfirmDialog(String titleTranslation, String messageTranslation, int options, int messageType) {
        return JOptionPane.showConfirmDialog(ContentPanel.frame, PublicValues.language.translate(messageTranslation), PublicValues.language.translate(titleTranslation), options, messageType);
    }

    public static void showMessageDialog(String titleTranslation, String messageTranslation, int messageType) {
        JOptionPane.showMessageDialog(ContentPanel.frame, PublicValues.language.translate(messageTranslation), PublicValues.language.translate(titleTranslation), messageType);
    }

    /**
     * Adds an exception to the list (add to the exception counter)
     *
     * @param ex instance of an Exception
     */
    public static void openException(Throwable ex) {
        if (ErrorDisplay.errorQueue != null) {
            ErrorDisplay.errorQueue.add(new ExceptionDialog(ex));
        }
    }
}
