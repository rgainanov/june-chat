package ru.geekbrains.june.chat.server;

import java.sql.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DbAuthenticationProvider implements AuthenticationProvider {
    private static final Logger LOGGER = LogManager.getLogger(DbAuthenticationProvider.class);
    private Connection conn;
    private Statement stmt;

    @Override
    public void start() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:javadb.db");
            LOGGER.info("DBHandler Connected to DB");
            stmt = conn.createStatement();
            LOGGER.info("DBHandler Statement created");
            stmt.executeUpdate(
                    "create table if not exists users (" +
                            "id integer primary key autoincrement," +
                            "login text," +
                            "password text," +
                            "nickname text)"
            );

        } catch (SQLException e) {
//            e.printStackTrace();
            LOGGER.error("DBHandler error connecting to db - " + e);
        }
    }

    @Override
    public void stop() {
        try {
            if (stmt != null) {
                stmt.close();
            }
            LOGGER.info("DBHandler Statement closed");
        } catch (SQLException e) {
//            e.printStackTrace();
            LOGGER.error("DBHandler error closing Statement - " + e);
        }
        try {
            if (conn != null) {
                conn.close();
            }
            LOGGER.info("DBHandler Connection closed");
        } catch (SQLException e) {
//            e.printStackTrace();
            LOGGER.error("DBHandler error closing Connection - " + e);
        }
        LOGGER.info("DBHandler Disconnected from DB");
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
            LOGGER.info("DBHandler getUserByField executed");
            if (rs.next()) {
                String[] results = new String[3];
                results[0] = rs.getString("login");
                results[1] = rs.getString("password");
                results[2] = rs.getString("nickname");
                return results;
            }
        } catch (SQLException e) {
//            e.printStackTrace();
            LOGGER.error("DBHandler error in getUserByField - " + e);
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
            LOGGER.info("DBHandler addUserRecord executed");
            if (rowsAffected > 0) {
                return true;
            }
        } catch (SQLException e) {
//            e.printStackTrace();
            LOGGER.error("DBHandler error in addUserRecord - " + e);
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
            LOGGER.info("DBHandler updateUserRecord executed");
            if (rowsAffected > 0) {
                return true;
            }
        } catch (SQLException e) {
//            e.printStackTrace();
            LOGGER.error("DBHandler error in updateUserRecord - " + e);
        }
        return false;
    }
}
