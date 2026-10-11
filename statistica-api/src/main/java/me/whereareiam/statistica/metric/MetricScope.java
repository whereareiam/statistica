package me.whereareiam.statistica.metric;

/**
 * Which instances send a metric.
 */
public enum MetricScope {
	/** Every instance sends its own value, such as the logins it handled. */
	INSTANCE,

	/**
	 * Only the {@link me.whereareiam.statistica.Election elected} instance sends the value, because
	 * every instance would read the same one, such as the number of accounts in a shared database.
	 */
	SHARED
}
