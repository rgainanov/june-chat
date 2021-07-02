package ru.geekbrains.june.chat.server;

import java.sql.SQLException;
import java.util.Arrays;

public class DbTest {
    public static void main(String[] args) {
        DbHandler db = new DbHandler();

        try {
            db.connect();
            db.createUsersTable();
//            db.addUserRecord("gainanov", "123");
//            db.updateUserRecord("gainanov", "password", "myNewPassword");
//            System.out.println(Arrays.toString(db.getUserByLogin("gainanov")));
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.disconnect();
        }
    }
}
