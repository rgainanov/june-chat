package ru.geekbrains.june.chat.client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LoggingHandler {
    String filename;
    BufferedWriter writer;
    BufferedReader reader;

    public LoggingHandler(String filename) {
        this.filename = filename;
        try {
            this.writer = new BufferedWriter(new FileWriter(this.filename + ".txt", true));
            this.reader = new BufferedReader(new FileReader(this.filename + ".txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BufferedWriter getWriter() {
        return writer;
    }

    public List<String> getLastOneHundredMessages() {
        List<String> lines = new ArrayList<>();
        List<String> linesToReturn = new ArrayList<>();
        try {
            lines = Files.readAllLines(Paths.get(filename + ".txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (lines.size() < 100) {
            return lines;
        } else {
            for (int i = lines.size() - 100; i < lines.size(); i++) {
                linesToReturn.add(lines.get(i));
            }
        }
        return linesToReturn;
    }

    public List<String> getChatHistoryLastNRows(int n) {
        List<String> list = new LinkedList<>();
        String str;
        try {
            while ((str = reader.readLine()) != null) {
                list.add(str);
                if (list.size() > n) {
                    list.remove(0);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return list;
    }

    public void writeLogs(String string) {
        try {
            writer.write(string + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

