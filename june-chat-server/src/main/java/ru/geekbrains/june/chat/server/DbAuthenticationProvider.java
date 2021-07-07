package ru.geekbrains.june.chat.server;

import java.sql.*;
import java.util.Arrays;

public class DbAuthenticationProvider implements AuthenticationProvider {
    private Connection conn;
    private Statement stmt;

    @Override
    public void start() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:javadb.db");
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

    @Override
    public void stop() {
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

    @Override
    public String[] getUserByField(String field, String login) {
        try (PreparedStatement ps =
                     conn.prepareStatement(
                             "SELECT * " +
                                     "FROM users " +
                                     "WHERE " + field + " = ?"
                     );
        ) {
            ps.setString(1, login);
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

    @Override
    public boolean addUserRecord(String login, String password, String nickname) {
        try (PreparedStatement ps =
                     conn.prepareStatement(
                             "insert into users (login, password, nickname) " +
                                     "values (?, ?, ?)"
                     );
        ) {
            ps.setString(1, login);
            ps.setString(2, password);
            ps.setString(3, nickname);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean updateUserRecord(String login, String fieldToUpdate, String newValue) {
        try (PreparedStatement ps =
                     conn.prepareStatement(
                             "update users set " +
                                     fieldToUpdate +
                                     " = ? where login = ?;"
                     );
        ) {
            ps.setString(1, newValue);
            ps.setString(2, login);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
