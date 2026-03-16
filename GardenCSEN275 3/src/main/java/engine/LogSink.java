package engine;

/**
 * LogSink
 *
 * This interface acts as an abstraction for logging.
 *
 * Why use an interface instead of writing directly to a file here?
 * - It decouples the engine from any specific logging implementation.
 * - Your logging teammate can later create a FileLogSink or LogManager
 *   without changing the engine code much.
 */
public interface LogSink {

    /**
     * Record one simulation event.
     *
     * Suggested format from the project:
     * DAY, EVENT, EVENT_VALUE, PLANTS_ALIVE
     *
     * The "details" field gives you room for extra readable information.
     */
    void event(int day, String event, String value, int plantsAlive, String details);

    /**
     * Default safe logger that does nothing.
     *
     * Useful during early development so the engine can run even if
     * a real logger is not connected yet.
     */
    static LogSink noop() {
        return (day, event, value, alive, details) -> {
            // intentionally blank
        };
    }
}
