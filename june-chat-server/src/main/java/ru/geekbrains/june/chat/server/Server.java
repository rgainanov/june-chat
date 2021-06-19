package ru.geekbrains.june.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private List<ClientHandler> clients;

    public Server() {
        try {
            this.clients = new ArrayList<>();

            ServerSocket serverSocket = new ServerSocket(8189);
            System.out.println("Server Started. Awaiting clients...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New Client Connected");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void subscribe(ClientHandler c) {
        broadcastMessage("User " + c.getUsername() + " connected to the chat");
        clients.add(c);
        broadcastClientsList();
    }

    public synchronized void unsubscribe(ClientHandler c) {
        clients.remove(c);
        broadcastMessage("User " + c.getUsername() + " left chat");
        broadcastClientsList();
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler c : clients) {
            c.sendMessage(message);
        }
    }

    public synchronized void broadcastClientsList() {
        StringBuilder builder = new StringBuilder(clients.size() * 10);
        builder.append("/clients_list ");

        for (ClientHandler c : clients) {
            builder.append(c.getUsername()).append(" ");
        }

        String clientsListStr = builder.toString();
        broadcastMessage(clientsListStr);
    }

    public synchronized boolean checkIfUsernameIsUsed(String username) {
        for (ClientHandler c : clients) {
            if (username.equalsIgnoreCase(c.getUsername())) {
                return true;
            }
        }
        return false;
    }

    public synchronized void sendPrivateMessage(ClientHandler sender, String receiver, String message) {
        if (sender.getUsername().equalsIgnoreCase(receiver)) {
            sender.sendMessage("SERVER: Personal messages are not allowed");
            return;
        }
        for (ClientHandler c : clients) {
            if (c.getUsername().equalsIgnoreCase(receiver)) {
                c.sendMessage("from user " + sender.getUsername() + ": " + message);
                sender.sendMessage("to user " + receiver + ": " + message);
                return;
            }
        }
        sender.sendMessage("User " + receiver + " is not available");
    }
}
