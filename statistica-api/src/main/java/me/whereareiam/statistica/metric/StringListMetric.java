package me.whereareiam.statistica.metric;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A set of text values, such as the modules that are turned on. bStats shows it as an advanced
 * pie that counts each value once.
 */
public final class StringListMetric extends Metric<List<String>> {
	StringListMetric(
			@NotNull String id,
			@NotNull MetricScope scope,
			@NotNull MetricSource<List<String>> source
	) {
		super(id, scope, source);
	}

	@Override
	public <R> R accept(@NotNull MetricVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public @NotNull StringListMetric withSource(@NotNull MetricSource<List<String>> source) {
		return new StringListMetric(getId(), getScope(), source);
	}
}
