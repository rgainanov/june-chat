package ru.geekbrains.june.chat.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
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

    @FXML
    ListView<String> clientsListView;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public void setAuthorized(boolean authorized) {
        msgPanel.setVisible(authorized);
        msgPanel.setManaged(authorized);
        authPanel.setVisible(!authorized);
        authPanel.setManaged(!authorized);
        clientsListView.setVisible(authorized);
        clientsListView.setManaged(authorized);
    }

    public void sendMessage() {
        try {
            out.writeUTF(messageField.getText().trim());
            messageField.clear();
            messageField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCloseRequest() {
        try {
            if (out != null) {
                out.writeUTF("/exit");
            }
        } catch (IOException e) {
            showError("Unable to send request to the server");
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

            new Thread(() -> mainClientLogic()).start();

        } catch (IOException e) {
            System.out.println("Unable to connect to the Server");
            System.exit(0);
        }
    }

    private void mainClientLogic() {
        try {
            while (true) {
                String inputMessage = in.readUTF();
                if (inputMessage.startsWith("/exit ")) {
                    closeConnection();
                }
                if (inputMessage.startsWith("/authok ")) {
                    setAuthorized(true);
                    break;
                }
                chatArea.appendText(inputMessage + "\n");
            }
            while (true) {
                String inputMessage = in.readUTF();
                if (inputMessage.startsWith("/")) {
                    if (inputMessage.equals("/exit")) {
                        break;
                    }
                    if (inputMessage.startsWith("/clients_list ")) {
                        Platform.runLater(() -> {
                            String[] tokens = inputMessage.split("\\s+");
                            clientsListView.getItems().clear();
                            for (int i = 1; i < tokens.length; i++) {
                                clientsListView.getItems().add(tokens[i]);
                            }
                        });
                    }
                    continue;
                }
                chatArea.appendText(inputMessage + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private void closeConnection() {
        setAuthorized(false);
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

    public void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }

    public void clientsListDoubleClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            String selectedUser = clientsListView.getSelectionModel().getSelectedItem();
            messageField.setText("/w " + selectedUser + " ");
            messageField.requestFocus();
            messageField.selectEnd();
        }
    }
}
