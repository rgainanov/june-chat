package ru.geekbrains.june.chat.server;

import java.sql.*;

public class DbHandler {
    private Connection conn;
    private Statement stmt;

    public void connect() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:javadb.db");
        stmt = conn.createStatement();
    }

    public void disconnect() {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createUsersTable() {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(
                    "create table if not exists users (" +
                            "id integer primary key autoincrement," +
                            "login text," +
                            "password text," +
                            "nickname text)"
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String[] getUserByField(String field, String value) {

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE " + field + " = ?");) {
            ps.setString(1, value);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String[] results = new String[3];
                results[0] = rs.getString("login");
                results[1] = rs.getString("password");
                results[2] = rs.getString("nickname");
                return results;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addUserRecord(String login, String password, String nickname) {
        try (PreparedStatement ps =
                     conn.prepareStatement("insert into users (login, password, nickname) values (?, ?, ?)");
        ) {
            ps.setString(1, login);
            ps.setString(2, password);
            ps.setString(3, nickname);
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateUserRecord(String login, String fieldToUpdate, String newValue) {
        try (PreparedStatement ps =
                     conn.prepareStatement("update users set " + fieldToUpdate + " = ? where login = ?;");
        ) {
            ps.setString(1, newValue);
            ps.setString(2, login);
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}

