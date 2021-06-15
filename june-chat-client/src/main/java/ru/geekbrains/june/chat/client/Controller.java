package ru.geekbrains.june.chat.client;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    TextArea chatArea;

    @FXML
    TextField messageField;

    @FXML
    Button btn;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        messageField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                btn.setDisable(newValue.trim().length() == 0);
            }
        });

        try {
            // окрываем сокет и подключаемся к localhost:8189
            socket = new Socket("localhost", 8189);
            // оборачиваем входящий поток сокет в DataInputStream для простоты обращения с байтами
            in = new DataInputStream(socket.getInputStream());
            // оборачиваем исходящий поток сокет в DataOutputStream для простоты обращения с байтами
            out = new DataOutputStream(socket.getOutputStream());

            // создаём новый поток в который обарачиваем ожидание сообщения от сервера
            Thread readThread = new Thread(() -> {
                try {
                    while (true) {
                        // ожидаем сообщение от сервера на входящем потоке
                        String inputMessage = in.readUTF();
                        // добовляем сообщение в текстовое поле
                        chatArea.appendText(inputMessage + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            // запускаем поток
            readThread.start();
        } catch (IOException e) {
            System.out.println("Не возможно подключится к серверу.");
            System.exit(0);
        }
    }

    public void sendMessage() {
        try {
            // отправляем в исходящий поток текс из текстовой строки
            out.writeUTF(messageField.getText().trim());
            //очищаем текстовую строку
            messageField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
