package me.whereareiam.statistica.platform.bungeecord;

import me.whereareiam.statistica.common.Backend;
import me.whereareiam.statistica.integration.bstats.BStatsBackend;
import me.whereareiam.statistica.metric.MetricSet;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;
import org.jetbrains.annotations.NotNull;

/**
 * Reaches bStats' BungeeCord library, which is only loaded when a plugin sends to bStats.
 */
final class BungeeCordBStats {
	private BungeeCordBStats() {
	}

	static @NotNull Backend backend(@NotNull Plugin plugin, int serviceId, @NotNull MetricSet metrics) {
		return new BStatsBackend(metrics, () -> {
			Metrics bstats = new Metrics(plugin, serviceId);
			return new BStatsBackend.Connection(bstats::addCustomChart, bstats::shutdown);
		});
	}
}
