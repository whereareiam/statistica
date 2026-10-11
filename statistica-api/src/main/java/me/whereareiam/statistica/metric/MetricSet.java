package me.whereareiam.statistica.metric;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.UnaryOperator;

/**
 * The metrics a plugin sends, in declaration order.
 * <p>
 * Values are read on the backends' own threads when they collect, so a value must be safe to
 * compute from any thread and should be cheap: a field, a cached count, a counter. Returning
 * {@code null}, a blank text or an empty collection sends nothing for that collection.
 *
 * <pre>{@code
 * MetricSet metrics = MetricSet.builder()
 *         .string("storage", () -> settings.getStorage().name())
 *         .stringList("modules", modules::enabledIds)
 *         .numberMap("logins", logins)                 // a CounterMap
 *         .total("accounts", accounts::cachedCount)    // sent by the elected instance only
 *         .build();
 * }</pre>
 * <p>
 * Every value leaves the server. Send fixed values the plugin controls, such as enum names and
 * the plugin's own ids, never names, addresses or other data a server owner or player entered.
 */
@Getter
public final class MetricSet {
	private static final MetricSet EMPTY = new MetricSet(List.of());

	/** The metrics, in declaration order. */
	private final @NotNull List<Metric<?>> metrics;

	private MetricSet(@NotNull List<Metric<?>> metrics) {
		this.metrics = List.copyOf(metrics);
	}

	/**
	 * Returns a set without metrics, for a plugin that only reports errors.
	 *
	 * @return the empty set
	 */
	public static @NotNull MetricSet empty() {
		return EMPTY;
	}

	/**
	 * Returns a set with every metric replaced by what {@code mapper} returns for it, such as the
	 * same metric reading from another source.
	 *
	 * @param mapper replaces one metric
	 * @return the new set
	 */
	public @NotNull MetricSet map(@NotNull UnaryOperator<Metric<?>> mapper) {
		return new MetricSet(metrics.stream().<Metric<?>>map(mapper).toList());
	}

	/**
	 * Starts the declaration of a set.
	 *
	 * @return a new builder
	 */
	public static @NotNull Builder builder() {
		return new Builder();
	}

	/**
	 * Declares the metrics of a set. Each id may be used once.
	 */
	public static final class Builder {
		private final Map<String, Metric<?>> metrics = new LinkedHashMap<>();

		private Builder() {
		}

		/**
		 * Declares a single text value.
		 *
		 * @param id    metric id
		 * @param value computes the value
		 * @return this builder
		 */
		public @NotNull Builder string(@NotNull String id, @NotNull Callable<@Nullable String> value) {
			return add(new StringMetric(id, MetricScope.INSTANCE, MetricSource.of(() -> text(value.call()))));
		}

		/**
		 * Declares a set of text values. Blank and repeated values are left out.
		 *
		 * @param id     metric id
		 * @param values computes the values
		 * @return this builder
		 */
		public @NotNull Builder stringList(
				@NotNull String id,
				@NotNull Callable<? extends @Nullable Collection<String>> values
		) {
			return add(new StringListMetric(id, MetricScope.INSTANCE, MetricSource.of(() -> texts(values.call()))));
		}

		/**
		 * Declares a single number this instance computes.
		 *
		 * @param id    metric id
		 * @param value computes the value
		 * @return this builder
		 */
		public @NotNull Builder number(@NotNull String id, @NotNull Callable<? extends @Nullable Number> value) {
			return add(new NumberMetric(id, MetricScope.INSTANCE, MetricSource.of(() -> number(value.call()))));
		}

		/**
		 * Declares the events a counter counted since each backend's last submission.
		 *
		 * @param id      metric id
		 * @param counter counts the events
		 * @return this builder
		 */
		public @NotNull Builder number(@NotNull String id, @NotNull Counter counter) {
			return add(new NumberMetric(id, MetricScope.INSTANCE, counter));
		}

		/**
		 * Declares numbers by key this instance computes. Entries without a key or value are left out.
		 *
		 * @param id     metric id
		 * @param values computes the values
		 * @return this builder
		 */
		public @NotNull Builder numberMap(
				@NotNull String id,
				@NotNull Callable<? extends @Nullable Map<String, ? extends Number>> values
		) {
			return add(new NumberMapMetric(id, MetricScope.INSTANCE, MetricSource.of(() -> numbers(values.call()))));
		}

		/**
		 * Declares the events of each kind a counter map counted since each backend's last submission.
		 *
		 * @param id      metric id
		 * @param counter counts the events
		 * @return this builder
		 */
		public @NotNull Builder numberMap(@NotNull String id, @NotNull CounterMap counter) {
			return add(new NumberMapMetric(id, MetricScope.INSTANCE, counter));
		}

		/**
		 * Declares numbers by key, grouped. Empty groups are left out.
		 *
		 * @param id     metric id
		 * @param values computes the values by group
		 * @return this builder
		 */
		public @NotNull Builder groupedMap(
				@NotNull String id,
				@NotNull Callable<? extends @Nullable Map<String, ? extends Map<String, ? extends Number>>> values
		) {
			return add(new GroupedMetric(id, MetricScope.INSTANCE, MetricSource.of(() -> groups(values.call()))));
		}

		/**
		 * Declares a single number every instance would read the same way, such as the size of a
		 * shared database. Only the elected instance sends it.
		 *
		 * @param id    metric id
		 * @param value computes the value
		 * @return this builder
		 */
		public @NotNull Builder total(@NotNull String id, @NotNull Callable<? extends @Nullable Number> value) {
			return add(new NumberMetric(id, MetricScope.SHARED, MetricSource.of(() -> number(value.call()))));
		}

		/**
		 * Declares numbers by key every instance would read the same way. Only the elected instance
		 * sends them.
		 *
		 * @param id     metric id
		 * @param values computes the values
		 * @return this builder
		 */
		public @NotNull Builder totalMap(
				@NotNull String id,
				@NotNull Callable<? extends @Nullable Map<String, ? extends Number>> values
		) {
			return add(new NumberMapMetric(id, MetricScope.SHARED, MetricSource.of(() -> numbers(values.call()))));
		}

		/**
		 * Finishes the set.
		 *
		 * @return the declared metrics
		 */
		public @NotNull MetricSet build() {
			return new MetricSet(new ArrayList<>(metrics.values()));
		}

		private @NotNull Builder add(@NotNull Metric<?> metric) {
			if (metrics.putIfAbsent(metric.getId(), metric) != null)
				throw new IllegalArgumentException("Metric declared twice: " + metric.getId());

			return this;
		}

		private static @Nullable String text(@Nullable String value) {
			if (value == null || value.isBlank()) return null;

			return value.trim();
		}

		private static @Nullable List<String> texts(@Nullable Collection<String> values) {
			if (values == null) return null;

			Set<String> texts = new LinkedHashSet<>();
			for (String value : values) {
				String text = text(value);
				if (text != null) texts.add(text);
			}

			return texts.isEmpty() ? null : List.copyOf(texts);
		}

		private static @Nullable Long number(@Nullable Number value) {
			return value == null ? null : value.longValue();
		}

		private static @Nullable Map<String, Long> numbers(@Nullable Map<String, ? extends Number> values) {
			if (values == null) return null;

			Map<String, Long> numbers = new LinkedHashMap<>();
			values.forEach((key, value) -> {
				String text = text(key);
				if (text != null && value != null) numbers.merge(text, value.longValue(), Long::sum);
			});

			return numbers.isEmpty() ? null : numbers;
		}

		private static @Nullable Map<String, Map<String, Long>> groups(
				@Nullable Map<String, ? extends Map<String, ? extends Number>> values
		) {
			if (values == null) return null;

			Map<String, Map<String, Long>> groups = new LinkedHashMap<>();
			values.forEach((group, entries) -> {
				String name = text(group);
				Map<String, Long> numbers = numbers(entries);
				if (name != null && numbers != null) groups.put(name, numbers);
			});

			return groups.isEmpty() ? null : groups;
		}
	}
}
