package me.whereareiam.statistica.platform.bungeecord;

import dev.faststats.bungee.BungeeContext;
import me.whereareiam.statistica.common.Backend;
import me.whereareiam.statistica.error.ErrorPolicy;
import me.whereareiam.statistica.integration.faststats.FastStatsBackend;
import me.whereareiam.statistica.metric.MetricSet;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Reaches FastStats' BungeeCord library, which is only loaded when a plugin sends to FastStats.
 */
final class BungeeCordFastStats {
	private BungeeCordFastStats() {
	}

	static @NotNull Backend backend(
			@NotNull Plugin plugin,
			@NotNull String token,
			@NotNull MetricSet metrics,
			@NotNull ErrorPolicy errors
	) {
		return new FastStatsBackend(
				metrics,
				errors,
				plugin.getClass().getClassLoader(),
				() -> new BungeeContext.Factory(plugin, token)
		);
	}
}
