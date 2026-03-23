package com.spotifyxp.logging;

import org.slf4j.Logger;
import org.slf4j.Marker;

import java.util.HashMap;
import java.util.Map;

public class Slf4JLogger implements Logger {
    /*
    Log levels:
    - TRACE => 0
    - DEBUG => 10
    - INFO => 20
    - WARN => 30
    - ERROR => 40
     */
    private static final Map<String, Integer> LOGLEVELFILTER = new HashMap<String, Integer>() {{
        put("org.freedesktop.dbus", 30);
        put("uk.co.caprica.vlcj", 30);
        put("org.sqlite.core.NativeDB", 30);
        put("org.mpris", 30);
    }};

    private final String name;

    public Slf4JLogger(String name) {
        this.name = name;
    }

    private static Integer resolveLevel(String loggerName) {
        String matchedKey = null;

        for (String key : LOGLEVELFILTER.keySet()) {
            if (loggerName.equals(key) || loggerName.startsWith(key + ".")) {
                if (matchedKey == null || key.length() > matchedKey.length()) {
                    matchedKey = key;
                }
            }
        }

        return matchedKey != null ? LOGLEVELFILTER.get(matchedKey) : null;
    }

    private static boolean isEnabled(String loggerName, int requestedLevel) {
        Integer configured = resolveLevel(loggerName);
        if (configured == null) return true;
        return requestedLevel >= configured;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isTraceEnabled() {
        return isEnabled(name, 0);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        if (isTraceEnabled()) {
            ConsoleLoggingModules.debug(msg);
        }
    }

    @Override
    public void trace(String format, Object arg) {
        if (isTraceEnabled()) {
            ConsoleLoggingModules.debug(format, arg);
        }
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            ConsoleLoggingModules.debug(format, arg1, arg2);
        }
    }

    @Override
    public void trace(String format, Object... arguments) {
        if (isTraceEnabled()) {
            ConsoleLoggingModules.debug(format, arguments);
        }
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (isTraceEnabled()) {
            ConsoleLoggingModules.debug(msg);
            ConsoleLoggingModules.Throwable(t);
        }
    }

    @Override
    public void trace(Marker marker, String msg) {}

    @Override
    public void trace(Marker marker, String format, Object arg) {}

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {}

    @Override
    public void trace(Marker marker, String format, Object... argArray) {}

    @Override
    public void trace(Marker marker, String msg, Throwable t) {}

    @Override
    public boolean isDebugEnabled() {
        return isEnabled(name, 10);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        if (isDebugEnabled()) {
            ConsoleLoggingModules.debug(msg);
        }
    }

    @Override
    public void debug(String format, Object arg) {
        if (isDebugEnabled()) {
            ConsoleLoggingModules.debug(format, arg);
        }
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            ConsoleLoggingModules.debug(format, arg1, arg2);
        }
    }

    @Override
    public void debug(String format, Object... arguments) {
        if (isDebugEnabled()) {
            ConsoleLoggingModules.debug(format, arguments);
        }
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (isDebugEnabled()) {
            ConsoleLoggingModules.debug(msg);
            ConsoleLoggingModules.Throwable(t);
        }
    }

    @Override
    public void debug(Marker marker, String msg) {}

    @Override
    public void debug(Marker marker, String format, Object arg) {}

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {}

    @Override
    public void debug(Marker marker, String format, Object... arguments) {}

    @Override
    public void debug(Marker marker, String msg, Throwable t) {}

    @Override
    public boolean isInfoEnabled() {
        return isEnabled(name, 20);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        if (isInfoEnabled()) {
            ConsoleLoggingModules.info(msg);
        }
    }

    @Override
    public void info(String format, Object arg) {
        if (isInfoEnabled()) {
            ConsoleLoggingModules.info(format, arg);
        }
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            ConsoleLoggingModules.info(format, arg1, arg2);
        }
    }

    @Override
    public void info(String format, Object... arguments) {
        if (isInfoEnabled()) {
            ConsoleLoggingModules.info(format, arguments);
        }
    }

    @Override
    public void info(String msg, Throwable t) {
        if (isInfoEnabled()) {
            ConsoleLoggingModules.info(msg);
            ConsoleLoggingModules.Throwable(t);
        }
    }

    @Override
    public void info(Marker marker, String msg) {}

    @Override
    public void info(Marker marker, String format, Object arg) {}

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {}

    @Override
    public void info(Marker marker, String format, Object... arguments) {}

    @Override
    public void info(Marker marker, String msg, Throwable t) {}

    @Override
    public boolean isWarnEnabled() {
        return isEnabled(name, 30);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        if (isWarnEnabled()) {
            ConsoleLoggingModules.warning(msg);
        }
    }

    @Override
    public void warn(String format, Object arg) {
        if (isWarnEnabled()) {
            ConsoleLoggingModules.warning(format, arg);
        }
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            ConsoleLoggingModules.warning(format, arg1, arg2);
        }
    }

    @Override
    public void warn(String format, Object... arguments) {
        if (isWarnEnabled()) {
            ConsoleLoggingModules.warning(format, arguments);
        }
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (isWarnEnabled()) {
            ConsoleLoggingModules.warning(msg);
            ConsoleLoggingModules.Throwable(t);
        }
    }

    @Override
    public void warn(Marker marker, String msg) {}

    @Override
    public void warn(Marker marker, String format, Object arg) {}

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {}

    @Override
    public void warn(Marker marker, String format, Object... arguments) {}

    @Override
    public void warn(Marker marker, String msg, Throwable t) {}

    @Override
    public boolean isErrorEnabled() {
        return isEnabled(name, 40);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        if (isErrorEnabled()) {
            ConsoleLoggingModules.error(msg);
        }
    }

    @Override
    public void error(String format, Object arg) {
        if (isErrorEnabled()) {
            ConsoleLoggingModules.error(format, arg);
        }
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            ConsoleLoggingModules.error(format, arg1, arg2);
        }
    }

    @Override
    public void error(String format, Object... arguments) {
        if (isErrorEnabled()) {
            ConsoleLoggingModules.error(format, arguments);
        }
    }

    @Override
    public void error(String msg, Throwable t) {
        if (isErrorEnabled()) {
            ConsoleLoggingModules.error(msg);
            ConsoleLoggingModules.Throwable(t);
        }
    }

    @Override
    public void error(Marker marker, String msg) {}

    @Override
    public void error(Marker marker, String format, Object arg) {}

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {}

    @Override
    public void error(Marker marker, String format, Object... arguments) {}

    @Override
    public void error(Marker marker, String msg, Throwable t) {}
}