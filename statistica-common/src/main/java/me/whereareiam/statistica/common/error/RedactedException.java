package me.whereareiam.statistica.common.error;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stands in for an error whose message held a value that must not leave the server.
 * <p>
 * A Java exception's message cannot be changed, so the report carries this copy instead: its
 * message starts with the original type, followed by the original message with the values hidden,
 * and it has the original stack trace.
 */
public final class RedactedException extends RuntimeException {
	/**
	 * Creates the stand-in of one error of a chain.
	 *
	 * @param type    name of the original error's type
	 * @param message original message with the values hidden, or {@code null} when it had none
	 * @param trace   original stack trace
	 * @param cause   stand-in or original of the original cause
	 */
	public RedactedException(
			@NotNull String type,
			@Nullable String message,
			@NotNull StackTraceElement[] trace,
			@Nullable Throwable cause
	) {
		super(message == null ? type : type + ": " + message, cause, false, true);
		setStackTrace(trace);
	}
}
