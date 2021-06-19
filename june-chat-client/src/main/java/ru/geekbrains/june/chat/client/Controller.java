package ru.geekbrains.june.chat.client;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Controller {
    @FXML
    TextArea chatArea;

    @FXML
    TextField messageField, usernameField;

    @FXML
    HBox msgPanel, authPanel;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;


    public void sendMessage() {
        try {
            out.writeUTF(messageField.getText().trim());
            messageField.clear();
            messageField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void tryToAuth() {
        connect();
        try {
            out.writeUTF("/auth " + usernameField.getText().trim());
            usernameField.clear();
        } catch (IOException e) {
            showError("Unable to send request to the server");
        }
    }

    public void connect() {
        if (socket != null && !socket.isClosed()) {
            return;
        }

        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            Thread readThread = new Thread(() -> {
                try {
                    while (true) {
                        String inputMessage = in.readUTF();
                        if (inputMessage.startsWith("/authok ")) {
                            msgPanel.setVisible(true);
                            msgPanel.setManaged(true);
                            authPanel.setVisible(false);
                            authPanel.setManaged(false);
                            break;
                        }
                        chatArea.appendText(inputMessage + "\n");
                    }
                    while (true) {
                        String inputMessage = in.readUTF();
                        chatArea.appendText(inputMessage + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            readThread.start();
        } catch (IOException e) {
            System.out.println("Unable to connect to the Server");
            System.exit(0);
        }
    }

    public void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }
}
