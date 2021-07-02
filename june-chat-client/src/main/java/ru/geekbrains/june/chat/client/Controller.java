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

    public void setSignInMenu(boolean signin) {
        authPanel.setVisible(!signin);
        authPanel.setManaged(!signin);
        signInPanel.setManaged(signin);
        signInPanel.setVisible(signin);
    }

    /*
        метод отвечает за отправку сообщений
    */
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

    /*
    метод отвечает за коректный выход из чата
    */
    public void sendCloseRequest() {
        try {
            // перед тем как отправть команду выхода, проверям открыт ли исходящий поток
            if (out != null) {
                out.writeUTF("/exit");
            }
        } catch (IOException e) {
            showError("\nUnable to send request to the server\n");
        }
    }

    /*
        метод ваполняет авториззацию
    */
    public void tryToAuth() {
        // вызываем метод который осуществляет подклячение к серверу
        connect();
        try {
            // отправляем команду авторизации на сервер вместе с введённым именем пользователя
            String[] loginTokens = loginField.getText().trim().split("\\s+");
            String[] passwordTokens = passwordField.getText().trim().split("\\s+");
            if (loginTokens.length > 1 || passwordTokens.length > 1) {
                out.writeUTF("/auth error_spaces");
            } else {
                out.writeUTF("/auth " + loginField.getText().trim() + " " + passwordField.getText().trim());
//                loginField.clear();
//                passwordField.clear();
            }
        } catch (IOException e) {
            showError("\nUnable to send request to the server\n");
        }
    }

    /*
        метод выполняет подключение к серверу
    */
    public void connect() {
        // выполняем проверку открыт ли сокет перед тем как начать соеденение с сервером,
        // если сокет открыт и не Null то выходим из метода
        if (socket != null && !socket.isClosed()) {
            return;
        }

        try {
            // подключаемся к серверу
            socket = new Socket("localhost", 8189);
            //открываем входящие и исходящие потоки
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            // создаём и запускаем новый поток с основной логикой клиента
            new Thread(() -> mainClientLogic()).start();

        } catch (IOException e) {
            showError("\nUnable to connect to the Server\n");
            System.exit(0);
        }
    }

    private void showNickname(String message){
        String username = message.split("\\s+")[1];
        nicknameField.setText("NOTIFICATION: Your username is - " + username);
    }

    private void mainClientLogic() {
        try {
            /*
            цикл отвечает за авторизацию, пока мы не получим от сервера команду /authok
            мы будем находится тут, если сообщение не содержит команду /authok то это сообщение будет
            выведенно на экран, но пока авторизация не будет успешна мы не попадё в следующий цикл который
            отвечает за получение сообщений от других пользователей
             */
            while (true) {
                String inputMessage = in.readUTF();
                if (inputMessage.startsWith("/exit ")) {
                    closeConnection();
                }
                if (inputMessage.startsWith("/signin_required ")) {
                    setSignInMenu(true);
                    signInLoginField.setText(loginField.getText());
                    signInPasswordField.setText(passwordField.getText());
                    continue;
                }
                if (inputMessage.startsWith("/authok ")) {
                    signInPanel.setManaged(false);
                    signInPanel.setVisible(false);
                    chatArea.clear();
                    setAuthorized(true);
                    showNickname(inputMessage);
                    break;
                }
                chatArea.appendText(inputMessage + "\n");
            }

            /*
            цикл отвечает за:
              1. получение сообщений от пользователей.
              2. получение списка пользователей
              а также отслеживает системную команду /exit которая завершает цыкл и корректно завершает программу
             */
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
                        // для того чтобы изменять список пользователей в потоке JavaFX оборачиваем заполнение списка
                        // в поток JavaFX
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

    /*
    метод реализует обработчик мыши и позволяет отслеживать нажатия мыши на елементы списка
     */
    public void clientsListDoubleClick(MouseEvent mouseEvent) {
        // если двойной клик по мыши
        if (mouseEvent.getClickCount() == 2) {
            // находим елемент по которому кликнули
            String selectedUser = clientsListView.getSelectionModel().getSelectedItem();
            //в строку сообщения добовляем команду для рассылки личных сообшений а имя выбранного пользователя
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
//                signInLoginField.clear();
//                signInPasswordField.clear();
//                signInNickname.clear();
            }
        } catch (IOException e) {
            showError("\nUnable to send request to the server\n");
        }
    }
}
