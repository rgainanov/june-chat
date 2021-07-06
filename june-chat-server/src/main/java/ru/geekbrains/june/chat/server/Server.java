package ru.geekbrains.june.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Server {
    private AuthenticationProvider authenticationProvider;
    private List<ClientHandler> clients;

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public Server() {
        try {
            this.authenticationProvider = new DbAuthenticationProvider();
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

    public synchronized void refreshClientList(ClientHandler c, String oldNickname) {
        for (int i = 0; i < clients.size(); i++) {
            if (!clients.get(i).getLogin().equals(c.getLogin())) {
                clients.get(i).sendMessage("SERVER: User " + oldNickname + " changed Nickname to -> " + c.getNickname());
            }
        }
        broadcastClientsList();
    }

    public synchronized void subscribe(ClientHandler c) {
        broadcastMessage("\nUser " + c.getNickname() + " connected to the chat\n");
        clients.add(c);
        c.sendHelpMessage();
        broadcastClientsList();
    }

    public synchronized void unsubscribe(ClientHandler c) {
        clients.remove(c);
        broadcastMessage("\nUser " + c.getNickname() + " left chat\n");
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
            builder.append(c.getNickname()).append(" ");
        }

        String clientsListStr = builder.toString();
        broadcastMessage(clientsListStr);
    }

    public synchronized boolean checkIfUsernameIsUsed(String username) {
        for (ClientHandler c : clients) {
            if (username.equalsIgnoreCase(c.getLogin())) {
                return true;
            }
        }
        return false;
    }

    public synchronized void sendPrivateMessage(ClientHandler sender, String receiver, String message) {
        if (sender.getNickname().equalsIgnoreCase(receiver)) {
            sender.sendMessage("\nSERVER: Personal messages are not allowed\n");
            return;
        }
        for (ClientHandler c : clients) {
            if (c.getNickname().equalsIgnoreCase(receiver)) {
                c.sendMessage("from user " + sender.getNickname() + ": " + message);
                sender.sendMessage("to user " + receiver + ": " + message);
                return;
            }
        }
        sender.sendMessage("\nSERVER: User " + receiver + " is not available\n");
    }
}
