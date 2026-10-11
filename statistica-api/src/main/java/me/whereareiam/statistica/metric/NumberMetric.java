package me.whereareiam.statistica.metric;

import org.jetbrains.annotations.NotNull;

/**
 * A single number, such as accounts stored or logins handled. bStats shows it as a single line
 * chart.
 */
public final class NumberMetric extends Metric<Long> {
	NumberMetric(@NotNull String id, @NotNull MetricScope scope, @NotNull MetricSource<Long> source) {
		super(id, scope, source);
	}

	@Override
	public <R> R accept(@NotNull MetricVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public @NotNull NumberMetric withSource(@NotNull MetricSource<Long> source) {
		return new NumberMetric(getId(), getScope(), source);
	}
}
