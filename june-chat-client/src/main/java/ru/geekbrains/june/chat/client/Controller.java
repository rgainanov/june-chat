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
    TextField messageField, nicknameField, loginField, passwordField, signInLoginField, signInPasswordField, signInNickname;

    @FXML
    HBox msgPanel, authPanel, usernameHbox, signInPanel;

    @FXML
    ListView<String> clientsListView;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private LoggingHandler loggingHandler;

    /*
        вспомогательный метод, отвечает за скрытие/открытие строки ввода/вывода имени пользователя
    */
    public void setAuthorized(boolean authorized) {
        msgPanel.setVisible(authorized);
        msgPanel.setManaged(authorized);
        authPanel.setVisible(!authorized);
        authPanel.setManaged(!authorized);
        clientsListView.setVisible(authorized);
        clientsListView.setManaged(authorized);
        usernameHbox.setVisible(authorized);
        usernameHbox.setManaged(authorized);

        signInLoginField.clear();
        signInPasswordField.clear();
        signInNickname.clear();

        loginField.clear();
        passwordField.clear();
    }

    public void getSignInMenu(boolean signin) {
        authPanel.setVisible(!signin);
        authPanel.setManaged(!signin);
        signInPanel.setManaged(signin);
        signInPanel.setVisible(signin);
    }

    public void sendMessage() {
        try {
            if (messageField.getText().trim().length() > 0) {
                out.writeUTF(messageField.getText().trim());
                messageField.clear();
                messageField.requestFocus();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
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
            showError("\nUnable to send request to the server\n");
        }
    }

    public void tryToAuth() {
        connect();
        try {
            String[] loginTokens = loginField.getText().trim().split("\\s+");
            String[] passwordTokens = passwordField.getText().trim().split("\\s+");
            if (loginTokens.length > 1 || passwordTokens.length > 1) {
                out.writeUTF("/auth error_spaces");
            } else {
                out.writeUTF("/auth " + loginField.getText().trim() + " " + passwordField.getText().trim());
            }
        } catch (IOException e) {
            showError("\nUnable to send request to the server\n");
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
            new Thread(this::mainClientLogic).start();

        } catch (IOException e) {
            showError("\nUnable to connect to the Server\n");
            System.exit(0);
        }
    }

    private void showNickname(String username) {
        nicknameField.setText("NOTIFICATION: Your username is - " + username);
    }

    private void mainClientLogic() {
//        chatArea.textProperty().addListener((observable, oldValue, newValue) -> {
//                    System.out.println("chat area listener");
//                    chatArea.setScrollTop(Double.MAX_VALUE);
//                }
//        );
        try {
            while (true) {
                String inputMessage = in.readUTF();
                if (inputMessage.startsWith("/exit ")) {
                    closeConnection();
                }
                if (inputMessage.startsWith("/signin_required ")) {
                    getSignInMenu(true);
                    signInLoginField.setText(loginField.getText());
                    signInPasswordField.setText(passwordField.getText());
                    continue;
                }
                if (inputMessage.startsWith("/authok ")) {
                    String username = inputMessage.split("\\s+")[1];
                    loggingHandler = new LoggingHandler(username);
                    signInPanel.setManaged(false);
                    signInPanel.setVisible(false);
                    chatArea.clear();
                    setAuthorized(true);
                    showNickname(username);
                    Platform.runLater(() -> {
                        for (String msg : loggingHandler.getChatHistoryLastNRows(100)) {
                            chatArea.appendText(msg + "\n");
                        }
                        chatArea.appendText("\nNOTIFICATION: History Loaded \n\n");
                        chatArea.selectEnd();
                    });
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

                    if (inputMessage.startsWith("/update_nickname ")) {
                        showNickname(inputMessage);
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

                    if (inputMessage.startsWith("/info_message ")) {
                        String[] tokens = inputMessage.split("\\s+", 2);
                        chatArea.appendText(tokens[1]);
                    }

                    continue;
                }
                chatArea.appendText(inputMessage + "\n");
                loggingHandler.writeLogs(inputMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private void closeConnection() {
        setAuthorized(false);
        loggingHandler.close();
        chatArea.clear();
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

    public void logOut() {
        sendMessage("/exit");
    }

    public void signIn() {
        try {
            String[] loginTokens = signInLoginField.getText().trim().split("\\s+");
            String[] passwordTokens = signInPasswordField.getText().trim().split("\\s+");
            String[] nicknameTokens = signInNickname.getText().trim().split("\\s+");

            if (loginTokens.length > 1 || passwordTokens.length > 1 || nicknameTokens.length > 1) {
                out.writeUTF("/signin error_spaces");
            } else {
                out.writeUTF("/signin " +
                        signInLoginField.getText().trim() +
                        " " +
                        signInPasswordField.getText().trim() +
                        " " +
                        signInNickname.getText().trim());
            }
        } catch (IOException e) {
            showError("\nUnable to send request to the server\n");
        }
    }
}
