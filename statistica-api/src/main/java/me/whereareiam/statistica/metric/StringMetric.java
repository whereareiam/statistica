package me.whereareiam.statistica.metric;

import org.jetbrains.annotations.NotNull;

/**
 * A single text value, such as the kind of storage in use. bStats shows it as a simple pie.
 */
public final class StringMetric extends Metric<String> {
	StringMetric(@NotNull String id, @NotNull MetricScope scope, @NotNull MetricSource<String> source) {
		super(id, scope, source);
	}

	@Override
	public <R> R accept(@NotNull MetricVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public @NotNull StringMetric withSource(@NotNull MetricSource<String> source) {
		return new StringMetric(getId(), getScope(), source);
	}
}
