package ru.geekbrains.june.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private String username;
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

    public String getUsername() {
        return username;
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
        server.broadcastMessage(username + ": " + message);
        return true;
    }

    // метод реализует логу сообщений для авторизации, а также выполняет проверку на корректнось имени пользователя
    // здесь я также добавил проверку на имена начинающиеся со служебного символа
    private boolean authMessageLogic(String message) {
        if (message.startsWith("/auth ")) {
            String[] tokens = message.split("\\s+");
            if (tokens.length == 1) {
                sendMessage("\nSERVER: Please enter Username\n");
                return false;
            }
            if (tokens.length > 2) {
                sendMessage("\nSERVER: Username cannot contain spaces\n");
                return false;
            }
            if (tokens[1].indexOf('/') != -1) {
                sendMessage("\nSERVER: Username cannot be used\n");
                return false;
            }
            if (server.checkIfUsernameIsUsed(tokens[1])) {
                sendMessage("\nSERVER: Username is already in use\n");
                return false;
            }
            username = tokens[1];
            // если имя пользователя прошло все проверки то отправляем клиенту сообщение об успешной авторизации
            sendMessage("/authok " + username);
            // ваполняем метод подписки
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
                "/w - is used for sending personal messages\n\t\t" +
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

