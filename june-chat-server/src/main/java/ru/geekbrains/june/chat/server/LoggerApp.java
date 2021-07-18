package ru.geekbrains.june.chat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggerApp {

    private static final Logger LOGGER = LogManager.getLogger(LoggerApp.class);

    public static void main(String[] args) {
        LOGGER.trace("trace test");
        LOGGER.debug("Debug");
        LOGGER.info("Info");
        LOGGER.warn("Warn");
        LOGGER.error("Error");
        LOGGER.fatal("Fatal");
        LOGGER.info("String: {}.", "Hello, World");
    }

}
