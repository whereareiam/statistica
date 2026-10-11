package me.whereareiam.statistica.metric;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Numbers by key, such as logins by outcome. bStats shows it as an advanced pie.
 */
public final class NumberMapMetric extends Metric<Map<String, Long>> {
	NumberMapMetric(
			@NotNull String id,
			@NotNull MetricScope scope,
			@NotNull MetricSource<Map<String, Long>> source
	) {
		super(id, scope, source);
	}

	@Override
	public <R> R accept(@NotNull MetricVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public @NotNull NumberMapMetric withSource(@NotNull MetricSource<Map<String, Long>> source) {
		return new NumberMapMetric(getId(), getScope(), source);
	}
}
