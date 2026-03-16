package logging;

import engine.LogSink;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * FileLogSink
 *
 * Concrete logging implementation that writes simulation events
 * to a log.txt file as required by the project specification.
 *
 * Log format: DAY, EVENT, EVENT_VALUE, PLANTS_ALIVE, DETAILS
 *
 * Features:
 * - Writes to log.txt in the working directory.
 * - Also maintains an in-memory list of log entries for the GUI.
 * - Each entry includes a real-world timestamp for traceability.
 * - Flushes after every write to ensure data is not lost on crash.
 */
public class FileLogSink implements LogSink {

    private static final String LOG_FILE = "log.txt";
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** In-memory log entries for the GUI to display. */
    private final List<String> entries = new ArrayList<>();

    /** The file writer, opened once and kept alive. */
    private PrintWriter writer;

    public FileLogSink() {
        try {
            File logFile = new File(LOG_FILE);
            writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile, false)));

            // Write header line.
            String header = "TIMESTAMP, DAY, EVENT, EVENT_VALUE, PLANTS_ALIVE, DETAILS";
            writer.println(header);
            writer.println("=".repeat(header.length()));
            writer.flush();
        } catch (IOException e) {
            System.err.println("WARNING: Could not open log file: " + e.getMessage());
            writer = null;
        }
    }

    /**
     * Records one simulation event to both file and memory.
     */
    @Override
    public void event(int day, String event, String value, int plantsAlive, String details) {
        String timestamp = LocalDateTime.now().format(TIME_FMT);
        String line = String.format("%s, %d, %s, %s, %d, %s",
                timestamp, day, event, value, plantsAlive, details);

        // Store in memory for GUI access.
        entries.add(line);

        // Write to file.
        if (writer != null) {
            writer.println(line);
            writer.flush();
        }

        // Also echo to console for development convenience.
        System.out.println("[LOG] " + line);
    }

    /**
     * Returns all log entries stored in memory.
     * Useful for the GUI log viewer.
     */
    public List<String> getEntries() {
        return new ArrayList<>(entries);
    }

    /**
     * Returns the most recent N entries.
     */
    public List<String> getRecentEntries(int count) {
        int start = Math.max(0, entries.size() - count);
        return new ArrayList<>(entries.subList(start, entries.size()));
    }

    /**
     * Cleanly closes the log file.
     * Should be called when the application shuts down.
     */
    public void close() {
        if (writer != null) {
            writer.flush();
            writer.close();
        }
    }
}
