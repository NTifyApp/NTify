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
package com.spotifyxp.logging;

import com.spotifyxp.utils.GraphicalMessage;

public class ConsoleLogging {
    private static final boolean killSwitch = false;

    private enum Prefixes {
        INFO("[INFO::{CLASSNAME} ] "),
        ERROR("[ERROR::{CLASSNAME} ] "),
        THROWABLE("[THROWABLE (CLASSNAME)] "),
        WARNING("[WARNING::{CLASSNAME} ] "),
        DEBUG("[DEBUG::{CLASSNAME} ] ");

        private final String prefix;

        Prefixes(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return this.prefix;
        }
    }

    private enum ColoredPrefixes {
        INFO(ColorMap.WHITE + "[" + ColorMap.BLUE_BOLD + "INFO::{CLASSNAME}" + ColorMap.WHITE + " ]" + ColorMap.RESET + " "),
        ERROR(ColorMap.WHITE + "[" + ColorMap.RED + "ERROR::{CLASSNAME}" + ColorMap.WHITE + " ]" + ColorMap.RESET + " "),
        THROWABLE(ColorMap.WHITE + "[" + ColorMap.RED_BOLD + "THROWABLE" + ColorMap.WHITE + " (CLASSNAME)]" + ColorMap.RESET + " "),
        WARNING(ColorMap.WHITE + "[" + ColorMap.YELLOW + "WARNING::{CLASSNAME}" + ColorMap.WHITE + " ]" + ColorMap.RESET + " "),
        DEBUG(ColorMap.WHITE + "[" + ColorMap.YELLOW + "DEBUG::{CLASSNAME}" + ColorMap.WHITE + " ]" + ColorMap.RESET + " ");

        private final String prefix;

        ColoredPrefixes(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return this.prefix;
        }
    }

    private enum PrefixTypes {
        INFO,
        ERROR,
        THROWABLE,
        WARNING,
        DEBUG
    }

    private static boolean isColored = false;
    private static boolean showClassName = false;

    public static void setColored(boolean colored) {
        isColored = colored;
    }

    public static void setShowClassName(boolean show) {
        showClassName = show;
    }

    private static String getPrefix(PrefixTypes type) {
        if (isColored) {
            String toOut = ColoredPrefixes.valueOf(type.name()).getPrefix();
            if (!showClassName) {
                toOut = toOut.replace("::{CLASSNAME}", "");
            }
            return toOut;
        } else {
            String toOut = Prefixes.valueOf(type.name()).getPrefix();
            if (!showClassName) {
                toOut = toOut.replace("::{CLASSNAME}", "");
            }
            return toOut;
        }
    }

    //Logging with {} values
    public static void info(String message, Object... object) {
        int counter = 0;
        StringBuilder builder = new StringBuilder();
        for (String m : message.split("\\{\\}")) {
            try {
                builder.append(m).append(object[counter]);
            } catch (ArrayIndexOutOfBoundsException exc) {
                builder.append(m);
            }
            counter++;
        }
        info(builder.toString());
    }

    public static void error(String message, Object... object) {
        int counter = 0;
        StringBuilder builder = new StringBuilder();
        for (String m : message.split("\\{\\}")) {
            try {
                builder.append(m).append(object[counter]);
            } catch (ArrayIndexOutOfBoundsException exc) {
                builder.append(m);
            }
            counter++;
        }
        error(builder.toString());
    }

    public static void warning(String message, Object... object) {
        int counter = 0;
        StringBuilder builder = new StringBuilder();
        for (String m : message.split("\\{\\}")) {
            try {
                builder.append(m).append(object[counter]);
            } catch (ArrayIndexOutOfBoundsException exc) {
                builder.append(m);
            }
            counter++;
        }
        warning(builder.toString());
    }

    public static void debug(String message, Object... object) {
        int counter = 0;
        StringBuilder builder = new StringBuilder();
        for (String m : message.split("\\{\\}")) {
            try {
                builder.append(m).append(object[counter]);
            } catch (ArrayIndexOutOfBoundsException exc) {
                builder.append(m);
            }
            counter++;
        }
        debug(builder.toString());
    }
    //-------------------------

    public static void Throwable(Throwable throwable) {
        if (killSwitch) return;
        System.out.println(getPrefix(PrefixTypes.THROWABLE).replace("(CLASSNAME)", throwable.getClass().getName()) + throwable.getMessage());
        for (StackTraceElement s : throwable.getStackTrace()) {
            System.out.println(getPrefix(PrefixTypes.THROWABLE).replace("(CLASSNAME)", throwable.getClass().getName()) + s);
        }
        GraphicalMessage.openException(throwable);
    }

    public static void info(String message) {
        if (killSwitch) return;
        System.out.println(getPrefix(PrefixTypes.INFO).replace("{CLASSNAME}", Thread.currentThread().getStackTrace()[Thread.currentThread().getStackTrace().length - 1].getClassName()) + message);
    }

    public static void error(String message) {
        if (killSwitch) return;
        System.out.println(getPrefix(PrefixTypes.ERROR).replace("{CLASSNAME}", Thread.currentThread().getStackTrace()[Thread.currentThread().getStackTrace().length - 1].getClassName()) + message);
    }

    public static void debug(String message) {
        if (killSwitch) return;
        System.out.println(getPrefix(PrefixTypes.DEBUG).replace("{CLASSNAME}", Thread.currentThread().getStackTrace()[Thread.currentThread().getStackTrace().length - 1].getClassName()) + message);
    }

    public static void warning(String message) {
        if (killSwitch) return;
        System.out.println(getPrefix(PrefixTypes.WARNING).replace("{CLASSNAME}", Thread.currentThread().getStackTrace()[Thread.currentThread().getStackTrace().length - 1].getClassName()) + message);
    }

    //Log4JSupport
    public static boolean isTraceEnabled() {
        return true;
    }
}
