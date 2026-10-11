package me.whereareiam.statistica.integration.faststats;

import dev.faststats.data.Metric;
import me.whereareiam.statistica.metric.GroupedMetric;
import me.whereareiam.statistica.metric.MetricSource;
import me.whereareiam.statistica.metric.MetricVisitor;
import me.whereareiam.statistica.metric.NumberMapMetric;
import me.whereareiam.statistica.metric.NumberMetric;
import me.whereareiam.statistica.metric.StringListMetric;
import me.whereareiam.statistica.metric.StringMetric;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns metrics into FastStats metrics, and commits what was read once FastStats accepted it.
 * <p>
 * FastStats reads every metric when it submits metrics, and again on another thread when it submits
 * errors, whose reports carry the metrics as context. Only the metrics submission confirms
 * delivery, through its flush callback on the same thread. The latest reading of each metric is
 * therefore kept per thread, and {@link #flush()} commits those of the thread that calls it: an
 * error submission in between never moves a counter on.
 */
final class FastStatsMetrics implements MetricVisitor<Metric<?>> {
	private final ThreadLocal<Map<MetricSource.Reader<?>, MetricSource.Reading<?>>> pending =
			ThreadLocal.withInitial(IdentityHashMap::new);

	@Override
	public @NotNull Metric<?> visit(@NotNull StringMetric metric) {
		MetricSource.Reader<String> reader = metric.getSource().reader();

		return Metric.string(metric.getId(), () -> read(reader));
	}

	@Override
	public @NotNull Metric<?> visit(@NotNull StringListMetric metric) {
		MetricSource.Reader<List<String>> reader = metric.getSource().reader();

		return Metric.stringArray(metric.getId(), () -> {
			List<String> values = read(reader);
			return values == null ? null : values.toArray(String[]::new);
		});
	}

	@Override
	public @NotNull Metric<?> visit(@NotNull NumberMetric metric) {
		MetricSource.Reader<Long> reader = metric.getSource().reader();

		return Metric.number(metric.getId(), () -> read(reader));
	}

	@Override
	public @NotNull Metric<?> visit(@NotNull NumberMapMetric metric) {
		MetricSource.Reader<Map<String, Long>> reader = metric.getSource().reader();

		return Metric.numberMap(metric.getId(), () -> {
			Map<String, Long> values = read(reader);
			return values == null || values.isEmpty() ? null : values;
		});
	}

	@Override
	public @NotNull Metric<?> visit(@NotNull GroupedMetric metric) {
		MetricSource.Reader<Map<String, Map<String, Long>>> reader = metric.getSource().reader();

		return Metric.numberMap(metric.getId(), () -> {
			Map<String, Map<String, Long>> groups = read(reader);
			if (groups == null) return null;

			Map<String, Long> entries = new LinkedHashMap<>();
			groups.forEach((group, values) -> values.forEach((key, value) -> entries.put(group + ":" + key, value)));

			return entries.isEmpty() ? null : entries;
		});
	}

	/**
	 * Commits the latest reading of each metric the calling thread read since its last flush.
	 */
	void flush() {
		Map<MetricSource.Reader<?>, MetricSource.Reading<?>> readings = pending.get();
		readings.values().forEach(MetricSource.Reading::commit);
		readings.clear();
	}

	private <T> @Nullable T read(@NotNull MetricSource.Reader<T> reader) throws Exception {
		MetricSource.Reading<T> reading = reader.read();
		if (reading == null) return null;

		pending.get().put(reader, reading);
		return reading.value();
	}
}
