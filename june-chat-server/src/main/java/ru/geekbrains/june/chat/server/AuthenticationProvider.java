package ru.geekbrains.june.chat.server;

public interface AuthenticationProvider {
    void start();

    void stop();

    String[] getUserByField(String field, String login);

    boolean addUserRecord(String login, String password, String nickname);

    boolean updateUserRecord(String login, String fieldToUpdate, String newValue);
}
