package me.whereareiam.statistica.error;

import org.jetbrains.annotations.NotNull;

/**
 * Reports errors a plugin caught, so they can be fixed without a server owner sending logs.
 * <p>
 * Report where an exception is caught and handled, such as a failed step of a login or a listener
 * that threw. Errors that reach no handler are collected on their own when the plugin's
 * {@link ErrorPolicy} captures them.
 *
 * <pre>{@code
 * try {
 *     step.run(connection);
 * } catch (RuntimeException exception) {
 *     errors.report(exception, ErrorContext.builder()
 *             .attribute("step", step.id())
 *             .redact(connection.username())
 *             .build());
 * }
 * }</pre>
 */
@FunctionalInterface
public interface ErrorReporter {
	/**
	 * Reports an error with the circumstances it happened in.
	 * <p>
	 * Never blocks and never throws: the report is queued and sent with the backend's next
	 * submission, or dropped when it is ignored, not the plugin's own, or over the limit.
	 *
	 * @param error   the error
	 * @param context attributes that help to reproduce it, and values to hide from it
	 */
	void report(@NotNull Throwable error, @NotNull ErrorContext context);

	/**
	 * Reports an error without further circumstances.
	 *
	 * @param error the error
	 */
	default void report(@NotNull Throwable error) {
		report(error, ErrorContext.empty());
	}

	/**
	 * Returns a reporter that drops everything.
	 *
	 * @return a reporter that does nothing
	 */
	static @NotNull ErrorReporter none() {
		return (error, context) -> {
		};
	}
}
