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
            new Thread(() -> {
                try {
                    while (true) {
                        String inputMessage = in.readUTF();
                        if (inputMessage.startsWith("/auth ")) {
                            String[] tokens = inputMessage.split("\\s+");
                            if (tokens.length > 2) {
                                sendMessage("SERVER: Username cannot contain spaces");
                                continue;
                            }
                            if (tokens.length == 1) {
                                sendMessage("SERVER: Please enter Username");
                                continue;
                            }
                            if (server.checkIfUsernameIsUsed(tokens[1])) {
                                sendMessage("SERVER: Username is already in use");
                                continue;
                            }
                            username = tokens[1];
                            sendMessage("/authok " + username);
                            server.subscribe(this);
                            break;
                        } else {
                            sendMessage("SERVER: Please Authorize before continue");
                        }
                    }
                    while (true) {
                        String inputMessage = in.readUTF();
                        if (inputMessage.startsWith("/")) {
                            continue;
                        }
                        server.broadcastMessage(username + ": " + inputMessage);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
