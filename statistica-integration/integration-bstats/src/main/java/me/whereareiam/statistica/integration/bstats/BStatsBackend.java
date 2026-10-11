package me.whereareiam.statistica.integration.bstats;

import me.whereareiam.statistica.common.Backend;
import me.whereareiam.statistica.metric.Metric;
import me.whereareiam.statistica.metric.MetricSet;
import org.bstats.charts.CustomChart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Sends metrics to bStats.
 * <p>
 * Each platform has its own bStats class; the platform module hands over how to create it, and
 * this backend adds the charts. bStats starts collecting when that class is created, which happens
 * in {@link #start()}.
 */
public final class BStatsBackend implements Backend {
	private final @NotNull MetricSet metrics;
	private final @NotNull Connector connector;
	private @Nullable Connection connection;

	/**
	 * Creates the backend.
	 *
	 * @param metrics   metrics to send
	 * @param connector creates the platform's bStats instance
	 */
	public BStatsBackend(@NotNull MetricSet metrics, @NotNull Connector connector) {
		this.metrics = metrics;
		this.connector = connector;
	}

	@Override
	public synchronized void start() {
		if (connection != null) return;

		Connection connection = connector.connect();
		BStatsCharts charts = new BStatsCharts();
		for (Metric<?> metric : metrics.getMetrics())
			connection.charts().accept(metric.accept(charts));

		this.connection = connection;
	}

	@Override
	public synchronized void stop() {
		if (connection == null) return;

		connection.shutdown().run();
		connection = null;
	}

	@Override
	public String toString() {
		return "bStats";
	}

	/**
	 * Creates the platform's bStats instance.
	 */
	@FunctionalInterface
	public interface Connector {
		/**
		 * Creates the instance, which starts collecting.
		 *
		 * @return how to add charts to it and shut it down
		 */
		@NotNull Connection connect();
	}

	/**
	 * A running bStats instance of a platform.
	 *
	 * @param charts   adds a chart
	 * @param shutdown stops collecting
	 */
	public record Connection(@NotNull Consumer<CustomChart> charts, @NotNull Runnable shutdown) {
	}
}
