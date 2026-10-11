package me.whereareiam.statistica.metric;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.LongAdder;

/**
 * Counts events between two submissions, such as logins handled.
 * <p>
 * Counting is a lock-free increment, cheap enough for a hot path. Each backend sends what was
 * counted since its own last successful submission; a submission that fails is carried over to
 * the next one, and no backend can reset what another has not sent yet.
 *
 * <pre>{@code
 * Counter logins = new Counter();
 * metrics.number("logins", logins);
 *
 * // in the login listener
 * logins.increment();
 * }</pre>
 */
public final class Counter implements MetricSource<Long> {
	private final LongAdder total = new LongAdder();

	/**
	 * Counts one event.
	 */
	public void increment() {
		total.increment();
	}

	/**
	 * Counts several events at once.
	 *
	 * @param amount number of events
	 * @throws IllegalArgumentException if {@code amount} is negative
	 */
	public void add(long amount) {
		if (amount < 0) throw new IllegalArgumentException("A counter only counts up: " + amount);

		total.add(amount);
	}

	/**
	 * Returns every event counted since the counter was created.
	 *
	 * @return the total
	 */
	public long total() {
		return total.sum();
	}

	@Override
	public @NotNull Reader<Long> reader() {
		return new CounterReader();
	}

	private final class CounterReader implements Reader<Long> {
		private long committed;

		@Override
		public synchronized @NotNull Reading<Long> read() {
			long current = total.sum();

			return Reading.of(current - committed, () -> commit(current));
		}

		private synchronized void commit(long value) {
			committed = Math.max(committed, value);
		}
	}
}
