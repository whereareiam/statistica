package me.whereareiam.statistica.platform.bungeecord;

import me.whereareiam.statistica.Statistica;
import me.whereareiam.statistica.common.Backend;
import me.whereareiam.statistica.common.StatisticaBuilder;
import me.whereareiam.statistica.error.ErrorPolicy;
import me.whereareiam.statistica.metric.MetricSet;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Creates the {@link Statistica} of a BungeeCord or Waterfall plugin.
 *
 * <pre>{@code
 * @Override
 * public void onEnable() {
 *     statistica = BungeeCordStatistica.builder(this)
 *             .bstats(12345)
 *             .faststats("0123456789abcdef0123456789abcdef")
 *             .metrics(metrics -> metrics.string("storage", () -> storage.name()))
 *             .build();
 *     statistica.start();
 * }
 *
 * @Override
 * public void onDisable() {
 *     statistica.stop();
 * }
 * }</pre>
 */
public final class BungeeCordStatistica {
	private BungeeCordStatistica() {
	}

	/**
	 * Starts the configuration of a plugin's statistics.
	 *
	 * @param plugin the plugin
	 * @return a new builder
	 */
	public static @NotNull Builder builder(@NotNull Plugin plugin) {
		return new Builder(Objects.requireNonNull(plugin, "plugin"));
	}

	/**
	 * Configures the statistics of a BungeeCord plugin.
	 */
	public static final class Builder extends StatisticaBuilder<Builder> {
		private final @NotNull Plugin plugin;

		private Builder(@NotNull Plugin plugin) {
			this.plugin = plugin;
		}

		/**
		 * Sends to bStats. Requires the {@code bstats} capability of this module.
		 *
		 * @param serviceId id of the plugin on bStats
		 * @return this builder
		 * @throws IllegalArgumentException if {@code serviceId} is not positive
		 */
		public @NotNull Builder bstats(int serviceId) {
			return bstatsService(serviceId);
		}

		@Override
		protected @NotNull Backend createBStats(int serviceId, @NotNull MetricSet metrics) {
			return BungeeCordBStats.backend(plugin, serviceId, metrics);
		}

		@Override
		protected @NotNull Backend createFastStats(
				@NotNull String token,
				@NotNull MetricSet metrics,
				@NotNull ErrorPolicy errors
		) {
			return BungeeCordFastStats.backend(plugin, token, metrics, errors);
		}
	}
}
