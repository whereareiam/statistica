package me.whereareiam.statistica.platform.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.faststats.velocity.VelocityContext;
import me.whereareiam.statistica.common.Backend;
import me.whereareiam.statistica.error.ErrorPolicy;
import me.whereareiam.statistica.integration.faststats.FastStatsBackend;
import me.whereareiam.statistica.metric.MetricSet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Reaches FastStats' Velocity library, which is only loaded when a plugin sends to FastStats.
 */
final class VelocityFastStats {
	private VelocityFastStats() {
	}

	static @NotNull Backend backend(
			@NotNull PluginContainer container,
			@NotNull ProxyServer server,
			@NotNull Logger logger,
			@NotNull Path dataDirectory,
			@NotNull String token,
			@NotNull MetricSet metrics,
			@NotNull ErrorPolicy errors,
			@NotNull Object plugin
	) {
		return new FastStatsBackend(
				metrics,
				errors,
				plugin.getClass().getClassLoader(),
				() -> new VelocityContext.Factory(container, server, logger, dataDirectory).token(token)
		);
	}
}
