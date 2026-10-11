package me.whereareiam.statistica.platform.velocity;

import me.whereareiam.statistica.common.Backend;
import me.whereareiam.statistica.integration.bstats.BStatsBackend;
import me.whereareiam.statistica.metric.MetricSet;
import org.bstats.velocity.Metrics;
import org.jetbrains.annotations.NotNull;

/**
 * Reaches bStats' Velocity library, which is only loaded when a plugin sends to bStats.
 */
final class VelocityBStats {
	private VelocityBStats() {
	}

	static @NotNull Backend backend(
			@NotNull Metrics.Factory factory,
			@NotNull Object plugin,
			int serviceId,
			@NotNull MetricSet metrics
	) {
		return new BStatsBackend(metrics, () -> {
			Metrics bstats = factory.make(plugin, serviceId);
			return new BStatsBackend.Connection(bstats::addCustomChart, bstats::shutdown);
		});
	}
}
