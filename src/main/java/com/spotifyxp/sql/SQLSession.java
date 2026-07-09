package com.spotifyxp.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class SQLSession {
    private static SQLSession itself;
    private static Connection connection;
    private static String sqlBaseURL = "";
    private static String database = "";
    private static String username = "";
    private static String password = "";
    private static final ArrayList<SQLElement> elements = new ArrayList<>();
    /**
     * Guards connect()/disconnect()/the shared connection field and every SQLTable operation
     * (via SQLSessionPrivate.connect()/disconnect()) - the connection is process-wide static state
     * and callers (track-change events, HotList's background recommendation reads, the history UI's
     * own background thread) can call into it concurrently.
     */
    static final ReentrantLock lock = new ReentrantLock();

    public SQLSession(String username, String password, String database) {
        SQLSession.database = database;
        SQLSession.username = username;
        SQLSession.password = password;
        itself = this;
    }

    public SQLSession(String database) {
        SQLSession.database = database;
        itself = this;
    }

    public void initSQLElement(SQLElement element) {
        elements.add(element);
        element.provideSession(new SQLSessionPrivate());
    }

    public boolean isConnected() throws SQLException {
        return connection != null && !connection.isClosed();
    }

    public boolean tryIsConnected() {
        try {
            return isConnected();
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean connect() throws SQLException {
        lock.lock();
        try {
            if (connection != null && !connection.isClosed()) {
                return true;
            }
            if (username.isEmpty() || password.isEmpty()) {
                connection = DriverManager.getConnection(sqlBaseURL);
            } else {
                connection = DriverManager.getConnection(sqlBaseURL, username, password);
            }
            for (SQLElement element : elements) {
                element.provideSession(new SQLSessionPrivate());
            }
            return isConnected();
        } finally {
            lock.unlock();
        }
    }

    public boolean tryConnect() {
        try {
            return connect();
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean loadDriver(String classPath, String name, String sqlType) {
        try {
            Class.forName(classPath);
            sqlBaseURL += name;
            sqlBaseURL += ":" + sqlType + ":" + database;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void disconnect() throws SQLException {
        lock.lock();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } finally {
            lock.unlock();
        }
    }

    public void tryDisconnect() {
        try {
            disconnect();
        } catch (SQLException ignored) {
        }
    }

    public static class SQLSessionPrivate {
        public void initSQLElement(SQLElement element) {
            element.provideSession(this);
        }

        public boolean isConnected() throws SQLException {
            return itself.isConnected();
        }

        public boolean tryIsConnected() {
            return itself.tryConnect();
        }

        public boolean connect() throws SQLException {
            return itself.connect();
        }

        public void disconnect() throws SQLException {
            itself.disconnect();
        }

        public void tryDisconnect() {
            try {
                itself.disconnect();
            } catch (SQLException ignored) {
            }
        }

        public boolean tryConnect() {
            return itself.tryConnect();
        }

        public boolean loadDriver(String classPath, String name, String sqlType) {
            return itself.loadDriver(classPath, name, sqlType);
        }

        public Connection getConnection() {
            return connection;
        }

        /**
         * Acquires the session-wide lock. Callers must always release it via unlock() in a
         * finally block, and should hold it across an entire logical operation (not just the
         * connect() call) so another thread's connect()/disconnect() can't swap out the
         * connection mid-statement.
         */
        public void lock() {
            SQLSession.lock.lock();
        }

        public void unlock() {
            SQLSession.lock.unlock();
        }
    }
}
