package me.whereareiam.statistica.common;

import me.whereareiam.statistica.error.ErrorReporter;
import org.jetbrains.annotations.NotNull;

/**
 * One service a plugin sends to, such as bStats or FastStats.
 * <p>
 * A backend connects to its service in {@link #start()}, not before, so building a
 * {@link me.whereareiam.statistica.Statistica} sends nothing.
 */
public interface Backend {
	/**
	 * Starts collecting and sending on the backend's own schedule.
	 *
	 * @throws Exception when the backend cannot start; the other backends start regardless
	 */
	void start() throws Exception;

	/**
	 * Sends what is pending where the service supports it and stops.
	 *
	 * @throws Exception when the backend cannot stop cleanly; the other backends stop regardless
	 */
	void stop() throws Exception;

	/**
	 * Returns where this backend takes error reports.
	 *
	 * @return the reporter, or {@link ErrorReporter#none()} when the service does not track errors
	 */
	default @NotNull ErrorReporter errors() {
		return ErrorReporter.none();
	}
}
