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

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
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

    private boolean regularMessageLogic(String message) {
        if (message.startsWith("/")) {
            if (message.equals("/exit")) {
                sendMessage("/exit");
                return false;
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
        server.broadcastMessage(username + ": " + message);
        return true;
    }

    private boolean authMessageLogic(String message) {
        if (message.startsWith("/auth ")) {
            String[] tokens = message.split("\\s+");
            if (tokens.length == 1) {
                sendMessage("SERVER: Please enter Username");
                return false;
            }
            if (tokens.length > 2) {
                sendMessage("SERVER: Username cannot contain spaces");
                return false;
            }
            if (tokens[1].indexOf('/') != -1) {
                sendMessage("SERVER: Username cannot be used");
                return false;
            }
            if (server.checkIfUsernameIsUsed(tokens[1])) {
                sendMessage("SERVER: Username is already in use");
                return false;
            }
            username = tokens[1];
            sendMessage("/authok " + username);
            server.subscribe(this);
            return true;
        } else {
            sendMessage("SERVER: Please Authorize before continue");
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
}

