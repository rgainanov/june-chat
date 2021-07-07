package ru.geekbrains.june.chat.client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LoggingHandler {
    String filename;
    BufferedWriter writer;

    public LoggingHandler(String filename) {
        this.filename = filename;
        try {
            this.writer = new BufferedWriter(new FileWriter(this.filename + ".txt", true));
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

