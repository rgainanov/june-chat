package ru.geekbrains.june.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private List<ClientHandler> clients;

    // конструктор сервера
    public Server() {
        try {
            // обявляем списко клиентов
            this.clients = new ArrayList<>();

            // открываем порт для подключения клиентов
            ServerSocket serverSocket = new ServerSocket(8189);
            System.out.println("Server Started. Awaiting clients...");

            //в этом цикле мы ожидаем подключения клиентов
            // в случае нового подключения мы вызываем обработчика клиентов в новом потоке
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New Client Connected");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //методы ниже выполняют служебные функции
    // оповещают пользователей чата о подключении новых клиентов а также об отключении уже существующих
    // добовляют/удаляют килентов из списка акивных пользователей
    public synchronized void subscribe(ClientHandler c) {
        broadcastMessage("\nUser " + c.getUsername() + " connected to the chat\n");
        clients.add(c);
        c.sendHelpMessage();
        broadcastClientsList();
    }

    public synchronized void unsubscribe(ClientHandler c) {
        clients.remove(c);
        broadcastMessage("\nUser " + c.getUsername() + " left chat\n");
        broadcastClientsList();
    }

    // метод позволяет отправить сообщение всем пользователям
    // используется цикл foreach для перебора имен пользователей
    public synchronized void broadcastMessage(String message) {
        for (ClientHandler c : clients) {
            c.sendMessage(message);
        }
    }

    // метод для рассылки списка клиентов
    // в нем мы пробегаемся по списку клиентов строим строку и отправляем это сообщение всем активным пользователям
    public synchronized void broadcastClientsList() {
        StringBuilder builder = new StringBuilder(clients.size() * 10);
        builder.append("/clients_list ");

        for (ClientHandler c : clients) {
            builder.append(c.getUsername()).append(" ");
        }

        String clientsListStr = builder.toString();
        broadcastMessage(clientsListStr);
    }

    // метода выполняет проверку имени пользователя
    // метод вызывается во время авторизации
    public synchronized boolean checkIfUsernameIsUsed(String username) {
        for (ClientHandler c : clients) {
            if (username.equalsIgnoreCase(c.getUsername())) {
                return true;
            }
        }
        return false;
    }

    // метод для отправки персональных сообшений
    public synchronized void sendPrivateMessage(ClientHandler sender, String receiver, String message) {
        if (sender.getUsername().equalsIgnoreCase(receiver)) {
            sender.sendMessage("\nSERVER: Personal messages are not allowed\n");
            return;
        }
        for (ClientHandler c : clients) {
            if (c.getUsername().equalsIgnoreCase(receiver)) {
                c.sendMessage("from user " + sender.getUsername() + ": " + message);
                sender.sendMessage("to user " + receiver + ": " + message);
                return;
            }
        }
        sender.sendMessage("\nSERVER: User " + receiver + " is not available\n");
    }
}
