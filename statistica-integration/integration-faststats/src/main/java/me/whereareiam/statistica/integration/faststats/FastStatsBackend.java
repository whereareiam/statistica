package me.whereareiam.statistica.integration.faststats;

import dev.faststats.FastStatsContext;
import dev.faststats.SimpleContext;
import me.whereareiam.statistica.common.Backend;
import me.whereareiam.statistica.error.ErrorPolicy;
import me.whereareiam.statistica.error.ErrorReporter;
import me.whereareiam.statistica.metric.Metric;
import me.whereareiam.statistica.metric.MetricSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Sends metrics and errors to FastStats.
 * <p>
 * Each platform has its own FastStats context; the platform module hands over its factory with the
 * token set, and this backend adds the metrics and the error tracker. FastStats creates nothing on
 * a server's first start, when it only writes its opt-out file, and nothing a server owner turned
 * off; this backend then sends nothing and keeps no error reports.
 */
public final class FastStatsBackend implements Backend {
	private final @NotNull MetricSet metrics;
	private final @NotNull Supplier<SimpleContext.Factory<?, ?>> factory;
	private final @NotNull FastStatsErrors errors;
	private @Nullable FastStatsContext context;

	/**
	 * Creates the backend.
	 *
	 * @param metrics metrics to send
	 * @param errors  which errors to report
	 * @param plugin  class loader of the plugin, whose code's errors are reported
	 * @param factory creates the platform's context factory, with the token set
	 */
	public FastStatsBackend(
			@NotNull MetricSet metrics,
			@NotNull ErrorPolicy errors,
			@NotNull ClassLoader plugin,
			@NotNull Supplier<SimpleContext.Factory<?, ?>> factory
	) {
		this.metrics = metrics;
		this.factory = factory;
		this.errors = new FastStatsErrors(errors, plugin);
	}

	@Override
	public synchronized void start() {
		if (context != null) return;

		FastStatsContext context = create(factory.get());
		if (context.errorTrackerService().isPresent()) errors.activate();
		context.ready();

		this.context = context;
	}

	@Override
	public synchronized void stop() {
		if (context == null) return;

		errors.deactivate();
		context.shutdown();
		context = null;
	}

	@Override
	public @NotNull ErrorReporter errors() {
		return errors;
	}

	@Override
	public String toString() {
		return "FastStats";
	}

	private <C extends SimpleContext, F extends SimpleContext.Factory<C, F>> @NotNull C create(
			@NotNull SimpleContext.Factory<C, F> factory
	) {
		FastStatsMetrics mapping = new FastStatsMetrics();

		return factory
				.metrics(builder -> {
					for (Metric<?> metric : metrics.getMetrics())
						builder.addMetric(metric.accept(mapping));

					return builder.onFlush(mapping::flush).create();
				})
				.errorTrackerService(errors.tracker())
				.create();
	}
}
