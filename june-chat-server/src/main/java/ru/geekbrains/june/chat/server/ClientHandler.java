package ru.geekbrains.june.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private String login;
    private String password;
    private String nickname;
    private DataInputStream in;
    private DataOutputStream out;

    // конструктор обработчика клиентов
    public ClientHandler(Server server, Socket socket) {
        try {
            // запоминаем сервер и сокет
            this.server = server;
            this.socket = socket;

            // создаём входаший и исходяший потоки
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            // запускаем основную логику обработчика клиентов в собственном потоке
            new Thread(() -> logic()).start();
        } catch (IOException e) {
            e.printStackTrace();
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
            while (!authMessageLogic(in.readUTF())) ;
            while (regularMessageLogic(in.readUTF())) ;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            server.unsubscribe(this);
            closeConnection();
        }
    }

    // метод который реализует логку сообшений
    private boolean regularMessageLogic(String message) {
        // проверяем начинается ли сообшение со служебного символа, если же сообщение не начинается со служебного символа
        // то это сообшение отправляется всем участникам чата
        if (message.startsWith("/")) {
            // если сообшение ровняется /exit то оправляем такое же сообшение обратно клиенту и возвращаем false
            // false в основной логике завершит цикл и соединение будет корректно закрыто как на сервере так и на клиенте
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
                String oldNickname = nickname;
                nickname = tokens[1];
                server.updateUserRecord(login, "nickname", nickname);
                sendMessage("/update_nickname " + nickname);
                sendMessage("SERVER: Nickname has been updated\n");
                server.refreshClientList(this, oldNickname);
                return true;
            }

            if (message.startsWith("/update_password ")) {
                String[] tokens = message.split("\\s+");
                if (tokens.length > 2) {
                    sendMessage("SERVER: Password cannot contain spaces\n");
                    return true;
                }
                server.updateUserRecord(login, "password", tokens[1]);
                sendMessage("SERVER: Password has been updated\n");
                return true;
            }
            // этот блок кода отвечает за персональные сообщения, если сообщение начинается со служебной команды /w
            // то оно делится на три части по пробелам, первая - сама команда, вторая получатель и третья само сообщение
            // я так же добавил проверку если пользователь не ввёл сообшение то сервер отпрвит подсказку как должна выглядеть команда
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

    // метод реализует логу сообщений для авторизации, а также выполняет проверку на корректнось имени пользователя
    // здесь я также добавил проверку на имена начинающиеся со служебного символа
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
            String[] userDetails = server.checkUserDetails("login", login);

            if (userDetails == null) {
                sendMessage("\nSERVER: Login not found, please Sing Up before continue\n");
                sendMessage("/signin_required ");
                return false;
            }
            if (userDetails[1].equals(password)) {
                nickname = userDetails[2];
                sendMessage("/authok " + nickname);
                // ваполняем метод подписки
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

            if (server.checkUserDetails("login", login) != null) {
                sendMessage("\nSERVER: This Login is already in use\n");
                return false;
            }
            if (server.checkUserDetails("nickname", nickname) != null) {
                sendMessage("\nSERVER: This Nickname is already in use\n");
                return false;
            }

            server.addUserRecord(login, password, nickname);
            sendMessage("/authok " + nickname);
            server.subscribe(this);
            return true;

        } else {
            sendMessage("\nSERVER: Please Authorize before continue\n");
            return false;
        }

    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeConnection() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendHelpMessage() {
        String message = "\nWelcome to the June Chat\n\tBellow command are available for you:\n\t" +
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
            e.printStackTrace();
        }
    }
}

