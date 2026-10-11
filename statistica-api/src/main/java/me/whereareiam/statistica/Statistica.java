package me.whereareiam.statistica;

import me.whereareiam.statistica.error.ErrorReporter;
import org.jetbrains.annotations.NotNull;

/**
 * Usage statistics and error reports of one plugin, sent to every backend the plugin configured.
 * <p>
 * A platform module builds it, for example {@code VelocityStatistica} on Velocity:
 *
 * <pre>{@code
 * Statistica statistica = builder
 *         .bstats(12345)
 *         .faststats("0123456789abcdef0123456789abcdef")
 *         .metrics(metrics -> metrics.string("storage", () -> storage.name()))
 *         .build();
 *
 * statistica.start();
 * statistica.errors().report(exception);
 * statistica.stop();
 * }</pre>
 */
public interface Statistica {
	/**
	 * Starts sending to every configured backend. Further calls do nothing.
	 * <p>
	 * Each backend keeps its own schedule and honours the opt-out file its server owners use.
	 */
	void start();

	/**
	 * Sends what is pending where a backend supports it, stops every backend and gives up the
	 * election of shared metrics. Further calls, and a call before {@link #start()}, do nothing.
	 */
	void stop();

	/**
	 * Returns where the plugin reports its errors.
	 * <p>
	 * Reports go to the backends that track errors and are dropped when none does, so callers never
	 * need to check which backends are configured.
	 *
	 * @return the error reporter of this plugin
	 */
	@NotNull ErrorReporter errors();
}
