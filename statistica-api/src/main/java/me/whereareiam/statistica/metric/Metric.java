package me.whereareiam.statistica.metric;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * One value a plugin sends, under an id the backends' charts refer to.
 * <p>
 * Metrics are declared through {@link MetricSet.Builder}. Each kind maps to the closest chart or
 * value type of every backend, which a backend handles through a {@link MetricVisitor}.
 *
 * @param <T> type of the value
 */
@Getter
public abstract class Metric<T> {
	private static final Pattern ID = Pattern.compile("[a-z0-9_]+");

	/** Id of the chart or data source, made of lower-case letters, digits and underscores. */
	private final @NotNull String id;

	/** Which instances send the metric. */
	private final @NotNull MetricScope scope;

	/** Where the value comes from. */
	private final @NotNull MetricSource<T> source;

	Metric(@NotNull String id, @NotNull MetricScope scope, @NotNull MetricSource<T> source) {
		Objects.requireNonNull(id, "id");
		if (!ID.matcher(id).matches())
			throw new IllegalArgumentException("A metric id is made of a-z, 0-9 and _: " + id);

		this.id = id;
		this.scope = Objects.requireNonNull(scope, "scope");
		this.source = Objects.requireNonNull(source, "source");
	}

	/**
	 * Passes this metric to the visitor method of its kind.
	 *
	 * @param visitor handles each kind of metric
	 * @param <R>     result of the visitor
	 * @return what the visitor returned
	 */
	public abstract <R> R accept(@NotNull MetricVisitor<R> visitor);

	/**
	 * Returns the same metric reading from another source, such as one that only answers on the
	 * elected instance.
	 *
	 * @param source the new source
	 * @return a metric of the same kind, id and scope
	 */
	public abstract @NotNull Metric<T> withSource(@NotNull MetricSource<T> source);
}
