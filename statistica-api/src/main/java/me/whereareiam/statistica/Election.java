package me.whereareiam.statistica;

/**
 * Decides which of several instances sends the metrics they share.
 * <p>
 * Instances of a plugin that share their data, such as proxies on one database, would otherwise
 * each send the same totals, and the backends would add them up. Only the elected instance sends
 * {@link me.whereareiam.statistica.metric.MetricScope#SHARED shared} metrics; every instance still
 * sends its own.
 * <p>
 * Use {@link #standalone()} when an instance shares nothing, and an adapter such as the JDBC or
 * Redis election when instances share the store the totals come from.
 */
@FunctionalInterface
public interface Election {
	/**
	 * Takes or renews this instance's claim and tells whether it holds it.
	 * <p>
	 * Called from the backends' own threads at most once per collection. A failure is treated as
	 * not elected, so shared metrics are left out rather than counted twice.
	 *
	 * @return whether this instance sends the shared metrics
	 * @throws Exception when the store holding the claim cannot be reached
	 */
	boolean elected() throws Exception;

	/**
	 * Gives up this instance's claim, so another instance can take it over at once.
	 *
	 * @throws Exception when the store holding the claim cannot be reached
	 */
	default void release() throws Exception {
	}

	/**
	 * Returns the election of an instance that shares nothing: it is always elected.
	 *
	 * @return an election that always elects this instance
	 */
	static Election standalone() {
		return () -> true;
	}
}
