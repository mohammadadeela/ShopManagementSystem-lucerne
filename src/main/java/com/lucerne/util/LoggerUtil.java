package com.lucerne.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.*;

/** Central application logging. Passwords, hashes and database credentials must never be logged. */
public final class LoggerUtil {
    private static final Logger LOGGER = Logger.getLogger("com.lucerne");
    private static volatile boolean configured;

    private LoggerUtil() { }

    public static synchronized void configure() {
        if (configured) return;
        try {
            Path directory = Path.of("logs");
            Files.createDirectories(directory);
            LOGGER.setUseParentHandlers(false);
            LOGGER.setLevel(Level.INFO);
            FileHandler file = new FileHandler(directory.resolve("lucerne-%g.log").toString(), 1_000_000, 5, true);
            file.setEncoding("UTF-8");
            file.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(file);
            ConsoleHandler console = new ConsoleHandler();
            console.setLevel(Level.WARNING);
            console.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(console);
            Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
                    LOGGER.log(Level.SEVERE, "Uncaught exception on " + thread.getName(), error));
            configured = true;
        } catch (IOException exception) {
            System.err.println("Lucerne logging could not be initialized: " + exception.getMessage());
        }
    }

    public static Logger get(Class<?> type) {
        configure();
        return Logger.getLogger(type.getName());
    }

    public static void info(Class<?> type, String message) { get(type).info(message); }
    public static void warning(Class<?> type, String message, Throwable error) {
        get(type).log(Level.WARNING, message, error);
    }
    public static void severe(Class<?> type, String message, Throwable error) {
        get(type).log(Level.SEVERE, message, error);
    }
}
