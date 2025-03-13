package com.spotifyxp.logging;

import com.spotifyxp.utils.GraphicalMessage;

public class ConsoleLogging {
    private static final boolean killSwitch = false;

    //All colors available in the console
    private static class ColorMap {
        // Reset
        public static final String RESET = "\033[0m";  // Text Reset
        // Regular Colors
        public static final String BLACK = "\033[0;30m";   // BLACK
        public static final String RED = "\033[0;31m";     // RED
        public static final String GREEN = "\033[0;32m";   // GREEN
        public static final String YELLOW = "\033[0;33m";  // YELLOW
        public static final String BLUE = "\033[0;34m";    // BLUE
        public static final String PURPLE = "\033[0;35m";  // PURPLE
        public static final String CYAN = "\033[0;36m";    // CYAN
        public static final String WHITE = RESET;   // WHITE
        // Bold
        public static final String BLACK_BOLD = "\033[1;30m";  // BLACK
        public static final String RED_BOLD = "\033[1;31m";    // RED
        public static final String GREEN_BOLD = "\033[1;32m";  // GREEN
        public static final String YELLOW_BOLD = "\033[1;33m"; // YELLOW
        public static final String BLUE_BOLD = "\033[1;34m";   // BLUE
        public static final String PURPLE_BOLD = "\033[1;35m"; // PURPLE
        public static final String CYAN_BOLD = "\033[1;36m";   // CYAN
        public static final String WHITE_BOLD = "\033[1;37m";  // WHITE
        // Underline
        public static final String BLACK_UNDERLINED = "\033[4;30m";  // BLACK
        public static final String RED_UNDERLINED = "\033[4;31m";    // RED
        public static final String GREEN_UNDERLINED = "\033[4;32m";  // GREEN
        public static final String YELLOW_UNDERLINED = "\033[4;33m"; // YELLOW
        public static final String BLUE_UNDERLINED = "\033[4;34m";   // BLUE
        public static final String PURPLE_UNDERLINED = "\033[4;35m"; // PURPLE
        public static final String CYAN_UNDERLINED = "\033[4;36m";   // CYAN
        public static final String WHITE_UNDERLINED = "\033[4;37m";  // WHITE
        // Background
        public static final String BLACK_BACKGROUND = "\033[40m";  // BLACK
        public static final String RED_BACKGROUND = "\033[41m";    // RED
        public static final String GREEN_BACKGROUND = "\033[42m";  // GREEN
        public static final String YELLOW_BACKGROUND = "\033[43m"; // YELLOW
        public static final String BLUE_BACKGROUND = "\033[44m";   // BLUE
        public static final String PURPLE_BACKGROUND = "\033[45m"; // PURPLE
        public static final String CYAN_BACKGROUND = "\033[46m";   // CYAN
        public static final String WHITE_BACKGROUND = "\033[47m";  // WHITE
        // High Intensity
        public static final String BLACK_BRIGHT = "\033[0;90m";  // BLACK
        public static final String RED_BRIGHT = "\033[0;91m";    // RED
        public static final String GREEN_BRIGHT = "\033[0;92m";  // GREEN
        public static final String YELLOW_BRIGHT = "\033[0;93m"; // YELLOW
        public static final String BLUE_BRIGHT = "\033[0;94m";   // BLUE
        public static final String PURPLE_BRIGHT = "\033[0;95m"; // PURPLE
        public static final String CYAN_BRIGHT = "\033[0;96m";   // CYAN
        public static final String WHITE_BRIGHT = "\033[0;97m";  // WHITE
        // Bold High Intensity
        public static final String BLACK_BOLD_BRIGHT = "\033[1;90m"; // BLACK
        public static final String RED_BOLD_BRIGHT = "\033[1;91m";   // RED
        public static final String GREEN_BOLD_BRIGHT = "\033[1;92m"; // GREEN
        public static final String YELLOW_BOLD_BRIGHT = "\033[1;93m";// YELLOW
        public static final String BLUE_BOLD_BRIGHT = "\033[1;94m";  // BLUE
        public static final String PURPLE_BOLD_BRIGHT = "\033[1;95m";// PURPLE
        public static final String CYAN_BOLD_BRIGHT = "\033[1;96m";  // CYAN
        public static final String WHITE_BOLD_BRIGHT = "\033[1;97m"; // WHITE
        // High Intensity backgrounds
        public static final String BLACK_BACKGROUND_BRIGHT = "\033[0;100m";// BLACK
        public static final String RED_BACKGROUND_BRIGHT = "\033[0;101m";// RED
        public static final String GREEN_BACKGROUND_BRIGHT = "\033[0;102m";// GREEN
        public static final String YELLOW_BACKGROUND_BRIGHT = "\033[0;103m";// YELLOW
        public static final String BLUE_BACKGROUND_BRIGHT = "\033[0;104m";// BLUE
        public static final String PURPLE_BACKGROUND_BRIGHT = "\033[0;105m"; // PURPLE
        public static final String CYAN_BACKGROUND_BRIGHT = "\033[0;106m";  // CYAN
        public static final String WHITE_BACKGROUND_BRIGHT = "\033[0;107m";   // WHITE

        public static void reset() {
            System.out.print(RESET);
        }
    }
    //----------------------------------------

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
