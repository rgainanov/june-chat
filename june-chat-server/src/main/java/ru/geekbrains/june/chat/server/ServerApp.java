package ru.geekbrains.june.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApp {
    public static void main(String[] args) {
        try {
            // открываем сокет на порту 8189
            ServerSocket serverSocket = new ServerSocket(8189);
            System.out.println("Сервер запущен. Ожидаем подключения клиентов.");

            while (true) {
                // ожидаем подключения клиента
                Socket client = serverSocket.accept();
                // создаём отдельный поток для клиента
                Thread clientThread = new Thread(() -> {
                    try {
                        // входной поток клиента
                        DataInputStream in = new DataInputStream(client.getInputStream());
                        // выходной поток клиента
                        DataOutputStream out = new DataOutputStream(client.getOutputStream());
                        System.out.println("Клиент подключился.");
                        while (true) {
                            // ожидаем сообщение от клиента на входном потоке
                            String inputMessage = in.readUTF();
                            // отправляем полученное собщение в исходящий поток
                            out.writeUTF("ECHO: " + inputMessage);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
