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
        clients.add(c);
    }

    public synchronized void unsubscribe(ClientHandler c) {
        clients.remove(c);
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler c : clients) {
            c.sendMessage(message);
        }
    }

    public synchronized boolean checkIfUsernameIsUsed(String user) {
        for (ClientHandler c : clients) {
            if (user.equalsIgnoreCase(c.getUsername())) {
                return true;
            }
        }
        return false;
    }
}
