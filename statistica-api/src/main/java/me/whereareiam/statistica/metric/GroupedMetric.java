package me.whereareiam.statistica.metric;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Numbers by key, grouped, such as modules by their author. bStats shows it as a drilldown pie;
 * FastStats receives the entries as one map with {@code group:key} keys.
 */
public final class GroupedMetric extends Metric<Map<String, Map<String, Long>>> {
	GroupedMetric(
			@NotNull String id,
			@NotNull MetricScope scope,
			@NotNull MetricSource<Map<String, Map<String, Long>>> source
	) {
		super(id, scope, source);
	}

	@Override
	public <R> R accept(@NotNull MetricVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public @NotNull GroupedMetric withSource(@NotNull MetricSource<Map<String, Map<String, Long>>> source) {
		return new GroupedMetric(getId(), getScope(), source);
	}
}
