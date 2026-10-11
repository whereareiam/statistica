package me.whereareiam.statistica.common;

import me.whereareiam.statistica.Statistica;
import me.whereareiam.statistica.common.metric.CachedElection;
import me.whereareiam.statistica.error.ErrorContext;
import me.whereareiam.statistica.error.ErrorReporter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Starts, stops and reports to every backend a plugin configured.
 * <p>
 * A backend that fails to start or stop is logged and skipped, so one unreachable service never
 * keeps the others, or the plugin, from working.
 */
public final class DefaultStatistica implements Statistica {
	private static final System.Logger LOGGER = System.getLogger("Statistica");

	private final @NotNull List<Backend> backends;
	private final @NotNull CachedElection election;
	private final @NotNull ErrorReporter errors;
	private boolean started;

	/**
	 * Combines backends.
	 *
	 * @param backends the backends, in the order they start
	 * @param election decides whether this instance sends shared metrics
	 */
	public DefaultStatistica(@NotNull List<Backend> backends, @NotNull CachedElection election) {
		this.backends = List.copyOf(backends);
		this.election = election;
		this.errors = this::report;
	}

	@Override
	public synchronized void start() {
		if (started) return;

		started = true;
		for (Backend backend : backends) {
			try {
				backend.start();
			} catch (Exception | LinkageError failure) {
				LOGGER.log(System.Logger.Level.WARNING, "Failed to start " + backend, failure);
			}
		}
	}

	@Override
	public synchronized void stop() {
		if (!started) return;

		started = false;
		for (Backend backend : backends) {
			try {
				backend.stop();
			} catch (Exception | LinkageError failure) {
				LOGGER.log(System.Logger.Level.WARNING, "Failed to stop " + backend, failure);
			}
		}
		election.release();
	}

	@Override
	public @NotNull ErrorReporter errors() {
		return errors;
	}

	private void report(@NotNull Throwable error, @NotNull ErrorContext context) {
		for (Backend backend : backends) {
			try {
				backend.errors().report(error, context);
			} catch (RuntimeException | LinkageError failure) {
				LOGGER.log(System.Logger.Level.DEBUG, "Failed to report an error to " + backend, failure);
			}
		}
	}
}
