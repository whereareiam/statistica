package me.whereareiam.statistica.metric;

import org.jetbrains.annotations.NotNull;

/**
 * Handles each kind of {@link Metric}, so a backend maps every kind it is given.
 *
 * @param <R> result of the visit
 */
public interface MetricVisitor<R> {
	/**
	 * Handles a single text value.
	 *
	 * @param metric the metric
	 * @return the result
	 */
	R visit(@NotNull StringMetric metric);

	/**
	 * Handles a set of text values.
	 *
	 * @param metric the metric
	 * @return the result
	 */
	R visit(@NotNull StringListMetric metric);

	/**
	 * Handles a single number.
	 *
	 * @param metric the metric
	 * @return the result
	 */
	R visit(@NotNull NumberMetric metric);

	/**
	 * Handles numbers by key.
	 *
	 * @param metric the metric
	 * @return the result
	 */
	R visit(@NotNull NumberMapMetric metric);

	/**
	 * Handles numbers by key, grouped.
	 *
	 * @param metric the metric
	 * @return the result
	 */
	R visit(@NotNull GroupedMetric metric);
}
