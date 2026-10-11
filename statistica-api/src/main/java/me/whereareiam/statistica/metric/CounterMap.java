package me.whereareiam.statistica.metric;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Counts events by kind between two submissions, such as logins by their outcome.
 * <p>
 * It behaves like a {@link Counter} per key. Keys should come from a small fixed set, such as the
 * constants of an enum: every key becomes an entry in the backends' charts, and a key made of user
 * data would send that data.
 *
 * <pre>{@code
 * CounterMap logins = new CounterMap();
 * metrics.numberMap("logins", logins);
 *
 * // in the login listener
 * logins.increment(result);   // an enum constant, sent as its name in lower case
 * }</pre>
 */
public final class CounterMap implements MetricSource<Map<String, Long>> {
	private final Map<String, LongAdder> totals = new ConcurrentHashMap<>();

	/**
	 * Counts one event of a kind.
	 *
	 * @param key kind of the event
	 */
	public void increment(@NotNull String key) {
		add(key, 1);
	}

	/**
	 * Counts one event of a kind named by an enum constant, sent as the constant's name in lower case.
	 *
	 * @param key kind of the event
	 */
	public void increment(@NotNull Enum<?> key) {
		add(key, 1);
	}

	/**
	 * Counts several events of a kind named by an enum constant.
	 *
	 * @param key    kind of the events
	 * @param amount number of events
	 * @throws IllegalArgumentException if {@code amount} is negative
	 */
	public void add(@NotNull Enum<?> key, long amount) {
		add(key.name().toLowerCase(Locale.ROOT), amount);
	}

	/**
	 * Counts several events of a kind.
	 *
	 * @param key    kind of the events
	 * @param amount number of events
	 * @throws IllegalArgumentException if {@code key} is blank or {@code amount} is negative
	 */
	public void add(@NotNull String key, long amount) {
		Objects.requireNonNull(key, "key");
		if (key.isBlank()) throw new IllegalArgumentException("A counter key must not be blank");
		if (amount < 0) throw new IllegalArgumentException("A counter only counts up: " + amount);

		totals.computeIfAbsent(key, ignored -> new LongAdder()).add(amount);
	}

	/**
	 * Returns every event counted since the map was created, by kind.
	 *
	 * @return totals by kind
	 */
	public @NotNull Map<String, Long> totals() {
		Map<String, Long> snapshot = new LinkedHashMap<>();
		totals.forEach((key, total) -> snapshot.put(key, total.sum()));

		return snapshot;
	}

	@Override
	public @NotNull Reader<Map<String, Long>> reader() {
		return new CounterMapReader();
	}

	private final class CounterMapReader implements Reader<Map<String, Long>> {
		private final Map<String, Long> committed = new HashMap<>();

		@Override
		public synchronized @NotNull Reading<Map<String, Long>> read() {
			Map<String, Long> current = totals();
			Map<String, Long> delta = new LinkedHashMap<>();
			current.forEach((key, total) -> {
				long counted = total - committed.getOrDefault(key, 0L);
				if (counted > 0) delta.put(key, counted);
			});

			return Reading.of(delta, () -> commit(current));
		}

		private synchronized void commit(@NotNull Map<String, Long> values) {
			values.forEach((key, total) -> committed.merge(key, total, Math::max));
		}
	}
}
