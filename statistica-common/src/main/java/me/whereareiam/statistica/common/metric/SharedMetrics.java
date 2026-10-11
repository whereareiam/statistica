package me.whereareiam.statistica.common.metric;

import me.whereareiam.statistica.metric.Metric;
import me.whereareiam.statistica.metric.MetricScope;
import me.whereareiam.statistica.metric.MetricSet;
import me.whereareiam.statistica.metric.MetricSource;
import org.jetbrains.annotations.NotNull;

/**
 * Makes {@link MetricScope#SHARED shared} metrics answer only on the elected instance.
 */
public final class SharedMetrics {
	private SharedMetrics() {
	}

	/**
	 * Returns the set with every shared metric reading nothing unless this instance is elected.
	 *
	 * @param metrics  the declared metrics
	 * @param election decides whether this instance is elected
	 * @return metrics ready to hand to the backends
	 */
	public static @NotNull MetricSet elect(@NotNull MetricSet metrics, @NotNull CachedElection election) {
		return metrics.map(metric -> metric.getScope() == MetricScope.SHARED ? gate(metric, election) : metric);
	}

	private static <T> @NotNull Metric<T> gate(@NotNull Metric<T> metric, @NotNull CachedElection election) {
		MetricSource<T> source = metric.getSource();

		return metric.withSource(() -> {
			MetricSource.Reader<T> reader = source.reader();
			return () -> election.elected() ? reader.read() : null;
		});
	}
}
