package me.whereareiam.statistica.platform.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import me.whereareiam.statistica.Statistica;
import me.whereareiam.statistica.common.Backend;
import me.whereareiam.statistica.common.StatisticaBuilder;
import me.whereareiam.statistica.error.ErrorPolicy;
import me.whereareiam.statistica.metric.MetricSet;
import org.bstats.velocity.Metrics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Creates the {@link Statistica} of a Velocity plugin.
 *
 * <pre>{@code
 * @Inject
 * public MyPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory bstats) {
 *     this.statistica = VelocityStatistica.builder(this, server, logger, dataDirectory)
 *             .bstats(bstats, 12345)
 *             .faststats("0123456789abcdef0123456789abcdef")
 *             .metrics(metrics -> metrics.string("storage", () -> storage.name()))
 *             .build();
 * }
 *
 * @Subscribe
 * public void onInitialize(ProxyInitializeEvent event) {
 *     statistica.start();
 * }
 *
 * @Subscribe
 * public void onShutdown(ProxyShutdownEvent event) {
 *     statistica.stop();
 * }
 * }</pre>
 */
public final class VelocityStatistica {
	private VelocityStatistica() {
	}

	/**
	 * Starts the configuration of a plugin's statistics.
	 *
	 * @param plugin        the plugin's main instance
	 * @param server        the proxy
	 * @param logger        the plugin's logger
	 * @param dataDirectory the plugin's data directory; the opt-out files of both backends sit
	 *                      beside it
	 * @return a new builder
	 * @throws IllegalArgumentException if {@code plugin} is not a loaded plugin's main instance
	 */
	public static @NotNull Builder builder(
			@NotNull Object plugin,
			@NotNull ProxyServer server,
			@NotNull Logger logger,
			@NotNull Path dataDirectory
	) {
		PluginContainer container = server.getPluginManager().fromInstance(plugin)
				.orElseThrow(() -> new IllegalArgumentException("Not a plugin's main instance: " + plugin));

		return new Builder(plugin, container, server, logger, dataDirectory);
	}

	/**
	 * Configures the statistics of a Velocity plugin.
	 */
	public static final class Builder extends StatisticaBuilder<Builder> {
		private final @NotNull Object plugin;
		private final @NotNull PluginContainer container;
		private final @NotNull ProxyServer server;
		private final @NotNull Logger logger;
		private final @NotNull Path dataDirectory;
		private @Nullable Metrics.Factory bstatsFactory;

		private Builder(
				@NotNull Object plugin,
				@NotNull PluginContainer container,
				@NotNull ProxyServer server,
				@NotNull Logger logger,
				@NotNull Path dataDirectory
		) {
			this.plugin = plugin;
			this.container = container;
			this.server = server;
			this.logger = logger;
			this.dataDirectory = dataDirectory;
		}

		/**
		 * Sends to bStats. Requires the {@code bstats} capability of this module.
		 *
		 * @param factory   bStats' factory, which only Velocity's injector can create; inject it into
		 *                  the plugin's constructor
		 * @param serviceId id of the plugin on bStats
		 * @return this builder
		 * @throws IllegalArgumentException if {@code serviceId} is not positive
		 */
		public @NotNull Builder bstats(@NotNull Metrics.Factory factory, int serviceId) {
			this.bstatsFactory = Objects.requireNonNull(factory, "factory");
			return bstatsService(serviceId);
		}

		@Override
		protected @NotNull Backend createBStats(int serviceId, @NotNull MetricSet metrics) {
			return VelocityBStats.backend(Objects.requireNonNull(bstatsFactory), plugin, serviceId, metrics);
		}

		@Override
		protected @NotNull Backend createFastStats(
				@NotNull String token,
				@NotNull MetricSet metrics,
				@NotNull ErrorPolicy errors
		) {
			return VelocityFastStats.backend(container, server, logger, dataDirectory, token, metrics, errors, plugin);
		}
	}
}
