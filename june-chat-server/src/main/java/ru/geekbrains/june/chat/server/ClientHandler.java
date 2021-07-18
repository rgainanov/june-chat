package ru.geekbrains.june.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientHandler {
    private static final Logger LOGGER = LogManager.getLogger(ClientHandler.class);
    private Server server;
    private Socket socket;
    private String login;
    private String password;
    private String nickname;
    private DataInputStream in;
    private DataOutputStream out;
    private ExecutorService cachedService;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;

            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            cachedService = Executors.newCachedThreadPool();
            cachedService.execute(() -> {
                logic();
            });
//            new Thread(this::logic).start();
        } catch (IOException e) {
//            e.printStackTrace();
            LOGGER.error("ClientHandler INIT error - " + e);
        }
    }

    public String getLogin() {
        return login;
    }

    public String getNickname() {
        return nickname;
    }

    private void logic() {
        try {
            server.getAuthenticationProvider().start();
            while (!authMessageLogic(in.readUTF())) ;
            while (regularMessageLogic(in.readUTF())) ;
        } catch (IOException e) {
//            e.printStackTrace();
            LOGGER.error("ClientHandler Input Stream error - " + e);
        } finally {
            server.unsubscribe(this);
            closeConnection();
            server.getAuthenticationProvider().stop();
        }
    }

    private boolean regularMessageLogic(String message) {
        if (message.startsWith("/")) {
            if (message.equals("/exit")) {
                sendMessage("/exit");
                return false;
            }
            if (message.equals("/help")) {
                sendHelpMessage();
                return true;
            }
            if (message.startsWith("/update_nickname ")) {
                String[] tokens = message.split("\\s+");
                if (tokens.length > 2) {
                    sendMessage("SERVER: Nickname cannot contain spaces\n");
                    return true;
                }
                if (server.getAuthenticationProvider().getUserByField("nickname", tokens[1]) != null) {
                    sendMessage("\nSERVER: This Nickname is already in use\n");
                    return true;
                }
                String oldNickname = nickname;
                nickname = tokens[1];
                boolean nicknameUpdateResponse = server.getAuthenticationProvider().updateUserRecord(login, "nickname", nickname);
                if (nicknameUpdateResponse) {
                    sendMessage("/update_nickname " + nickname);
                    sendMessage("SERVER: Nickname has been updated\n");
                    server.refreshClientList(this, oldNickname);
                    return true;
                }

            }

            if (message.startsWith("/update_password ")) {
                String[] tokens = message.split("\\s+");
                if (tokens.length > 2) {
                    sendMessage("SERVER: Password cannot contain spaces\n");
                    return true;
                }
                boolean passUpdateResponse = server.getAuthenticationProvider().updateUserRecord(login, "password", tokens[1]);
                if (passUpdateResponse) {
                    sendMessage("SERVER: Password has been updated\n");
                    return true;
                }

            }
            if (message.startsWith("/w ")) {
                String[] tokens = message.split("\\s+", 3);
                if (tokens.length == 2) {
                    sendMessage("SERVER: Please add message\n\t command is /w <recipient> <message>");
                    return true;
                }
                server.sendPrivateMessage(this, tokens[1], tokens[2]);
            }
            return true;
        }
        server.broadcastMessage(login + ": " + message);
        return true;
    }

    private boolean authMessageLogic(String message) {
        if (message.startsWith("/auth ")) {
            String[] tokens = message.split("\\s+");
            if (tokens[1].equals("error_spaces")) {
                sendMessage("\nSERVER: Login and Password cannot contain spaces\n");
                return false;
            }
            if (tokens.length >= 1 && tokens.length < 3) {
                sendMessage("\nSERVER: Please enter Login and Password\n");
                return false;
            }
            if (server.checkIfUsernameIsUsed(tokens[1])) {
                sendMessage(
                        "\nSERVER: You are already logged in from another device\n" +
                                "\tPlease log out and log back again\n");
                return false;
            }
            login = tokens[1];
            password = tokens[2];
            String[] selectedUser = server.getAuthenticationProvider().getUserByField("login", login);

            if (selectedUser == null) {
                sendMessage("\nSERVER: Login not found, please Sing Up before continue\n");
                sendMessage("/signin_required ");
                return false;
            }
            if (selectedUser[1].equals(password)) {
                nickname = selectedUser[2];
                LOGGER.info("ClientHandler User - " + nickname + " Successfully Authenticated");
                sendMessage("/authok " + nickname);

                server.subscribe(this);
                return true;
            }

            sendMessage("\nSERVER: Password is Incorrect\n");
            return false;

        } else if (message.startsWith("/signin ")) {
            String[] tokens = message.split("\\s+");
            if (tokens[1].equals("error_spaces")) {
                sendMessage("\nSERVER: Login, Password and Nickname cannot contain spaces\n");
                return false;
            }
            if (tokens.length >= 1 && tokens.length < 4) {
                sendMessage("\nSERVER: Please enter Login, Password and Nickname\n");
                return false;
            }

            login = tokens[1];
            password = tokens[2];
            nickname = tokens[3];

            if (server.getAuthenticationProvider().getUserByField("login", login) != null) {
                sendMessage("\nSERVER: This Login is already in use\n");
                return false;
            }
            if (server.getAuthenticationProvider().getUserByField("nickname", nickname) != null) {
                sendMessage("\nSERVER: This Nickname is already in use\n");
                return false;
            }

            boolean userAddResponse = server.getAuthenticationProvider().addUserRecord(login, password, nickname);
            if (userAddResponse) {
                LOGGER.info("ClientHandler User - " + nickname + " Successfully signed in");
                sendMessage("\nSERVER: Successfully signed in\n");
                sendMessage("/authok " + nickname);
                server.subscribe(this);
                return true;
            } else {
                sendMessage("\nSERVER: Error Occurred, please try again\n");
                LOGGER.warn("ClientHandler User - " + nickname + " Error Occurred during Sign in");
            }
        }
        sendMessage("\nSERVER: Please Authorize before continue\n");
        return false;


    }

    public void sendMessage(String message) {
        try {
            LOGGER.trace(login + " - " + message);
            out.writeUTF(message);
        } catch (IOException e) {
//            e.printStackTrace();
            LOGGER.error("SEND MESSAGE ERROR - " + e);
        }
    }

    private void closeConnection() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
//            e.printStackTrace();
            LOGGER.error("ClientHandler Input Stream close error - " + e);

        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
//            e.printStackTrace();
            LOGGER.error("ClientHandler Output Stream close error - " + e);
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
//            e.printStackTrace();
            LOGGER.error("ClientHandler Socket close error - " + e);
        }
        cachedService.shutdown();
    }

    public void sendHelpMessage() {
        String message = "/info_message \nWelcome to the June Chat\n\tBellow command are available for you:\n\t" +
                "/exit - is used for when you want to leave chat\n\t" +
                "/update_nickname <new nickname> - is used to update your nickname\n\t" +
                "/update_password <new password> - is used to update your password\n\t" +
                "/w <recipient> <message> - is used for sending personal messages\n\t\t" +
                "Please remember that you can also double click on a username in a list\n\n" +
                "You can get this help using /help command\n" +
                "Have a nice time,\n" +
                "SERVER\n\n";
        try {
            out.writeUTF(message);
        } catch (IOException e) {
//            e.printStackTrace();
            LOGGER.error("ClientHandler Help Message send error - " + e);
        }
    }
}

