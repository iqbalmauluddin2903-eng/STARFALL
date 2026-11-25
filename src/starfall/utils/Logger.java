package starfall.utils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class Logger {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Logger() {}

    public static void info(String message) {
        log("INFO", message);
    }

    public static void warn(String message) {
        log("WARN", message);
    }

    public static void error(String message, Throwable t) {
        log("ERROR", message + (t != null ? " -> " + t.getMessage() : ""));
        if (t != null) {
            t.printStackTrace(System.err);
        }
    }

    private static void log(String level, String message) {
        String time = LocalTime.now().format(TIME_FMT);
        System.out.printf("[%s] [%s] [%s] %s%n", time, level, Thread.currentThread().getName(), message);
    }
}