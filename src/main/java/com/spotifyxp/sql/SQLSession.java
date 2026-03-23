package com.spotifyxp.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

public class SQLSession {
    private static SQLSession itself;
    private static Connection connection;
    private static String sqlBaseURL = "";
    private static String database = "";
    private static String username = "";
    private static String password = "";
    private static final ArrayList<SQLElement> elements = new ArrayList<>();

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
        return !connection.isClosed();
    }

    public boolean tryIsConnected() {
        try {
            return !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean connect() throws SQLException {
        if (username.isEmpty() || password.isEmpty()) {
            connection = DriverManager.getConnection(sqlBaseURL);
        } else {
            connection = DriverManager.getConnection(sqlBaseURL, username, password);
        }
        for (SQLElement element : elements) {
            element.provideSession(new SQLSessionPrivate());
        }
        return isConnected();
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
        connection.close();
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
    }
}
