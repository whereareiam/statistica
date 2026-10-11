package me.whereareiam.statistica.integration.bstats;

import me.whereareiam.statistica.metric.GroupedMetric;
import me.whereareiam.statistica.metric.Metric;
import me.whereareiam.statistica.metric.MetricSource;
import me.whereareiam.statistica.metric.MetricVisitor;
import me.whereareiam.statistica.metric.NumberMapMetric;
import me.whereareiam.statistica.metric.NumberMetric;
import me.whereareiam.statistica.metric.StringListMetric;
import me.whereareiam.statistica.metric.StringMetric;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.CustomChart;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns metrics into bStats charts.
 * <p>
 * bStats does not tell whether a submission arrived, so a value counts as sent as soon as bStats
 * collects it: a counter's events of a failed submission are lost to bStats, never sent twice.
 */
final class BStatsCharts implements MetricVisitor<CustomChart> {
	@Override
	public @NotNull CustomChart visit(@NotNull StringMetric metric) {
		MetricSource.Reader<String> reader = reader(metric);

		return new SimplePie(metric.getId(), () -> collect(reader));
	}

	@Override
	public @NotNull CustomChart visit(@NotNull StringListMetric metric) {
		MetricSource.Reader<List<String>> reader = reader(metric);

		return new AdvancedPie(metric.getId(), () -> {
			List<String> values = collect(reader);
			if (values == null) return null;

			Map<String, Integer> entries = new LinkedHashMap<>();
			values.forEach(value -> entries.put(value, 1));

			return entries;
		});
	}

	@Override
	public @NotNull CustomChart visit(@NotNull NumberMetric metric) {
		MetricSource.Reader<Long> reader = reader(metric);

		return new SingleLineChart(metric.getId(), () -> {
			Long value = collect(reader);
			return value == null ? 0 : clamp(value);
		});
	}

	@Override
	public @NotNull CustomChart visit(@NotNull NumberMapMetric metric) {
		MetricSource.Reader<Map<String, Long>> reader = reader(metric);

		return new AdvancedPie(metric.getId(), () -> {
			Map<String, Long> values = collect(reader);
			return values == null ? null : clamp(values);
		});
	}

	@Override
	public @NotNull CustomChart visit(@NotNull GroupedMetric metric) {
		MetricSource.Reader<Map<String, Map<String, Long>>> reader = reader(metric);

		return new DrilldownPie(metric.getId(), () -> {
			Map<String, Map<String, Long>> groups = collect(reader);
			if (groups == null) return null;

			Map<String, Map<String, Integer>> charts = new LinkedHashMap<>();
			groups.forEach((group, values) -> charts.put(group, clamp(values)));

			return charts;
		});
	}

	private static <T> @NotNull MetricSource.Reader<T> reader(@NotNull Metric<T> metric) {
		return metric.getSource().reader();
	}

	private static <T> @Nullable T collect(@NotNull MetricSource.Reader<T> reader) throws Exception {
		MetricSource.Reading<T> reading = reader.read();
		if (reading == null) return null;

		reading.commit();
		return reading.value();
	}

	private static @NotNull Map<String, Integer> clamp(@NotNull Map<String, Long> values) {
		Map<String, Integer> clamped = new LinkedHashMap<>();
		values.forEach((key, value) -> clamped.put(key, clamp(value)));

		return clamped;
	}

	private static int clamp(long value) {
		return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
	}
}
