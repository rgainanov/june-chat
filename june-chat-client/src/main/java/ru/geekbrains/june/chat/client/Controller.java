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
    TextField messageField, usernameField, authField;

    @FXML
    HBox msgPanel, authPanel;

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
        usernameField.setVisible(authorized);
        usernameField.setManaged(authorized);
    }

    /*
        метод отвечает за отправку сообщений
    */
    public void sendMessage() {
        try {
            if (messageField.getText().trim().length() > 0){
                out.writeUTF(messageField.getText().trim());
                messageField.clear();
                messageField.requestFocus();
            }
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
            out.writeUTF("/auth " + authField.getText().trim());
            authField.clear();
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
                if (inputMessage.startsWith("/authok ")) {
                    setAuthorized(true);
                    String username = inputMessage.split("\\s+")[1];
                    usernameField.setText("NOTIFICATION: Your username is - " + username);
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
}
