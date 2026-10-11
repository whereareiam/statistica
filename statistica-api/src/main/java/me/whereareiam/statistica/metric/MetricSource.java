package me.whereareiam.statistica.metric;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Where a metric's value comes from.
 * <p>
 * Every backend opens its own {@link Reader}, because backends send at different times: a
 * {@link Counter} has to know, per backend, what that backend has already sent.
 *
 * @param <T> type of the value
 */
@FunctionalInterface
public interface MetricSource<T> {
	/**
	 * Opens a reader for one backend.
	 *
	 * @return a reader that only this backend uses
	 */
	@NotNull Reader<T> reader();

	/**
	 * Returns a source that reads a value on every collection and keeps no state.
	 *
	 * @param value computes the value; may return {@code null} to send nothing this time
	 * @param <T>   type of the value
	 * @return a source of the value
	 */
	static <T> @NotNull MetricSource<T> of(@NotNull Callable<? extends T> value) {
		Objects.requireNonNull(value, "value");

		return () -> () -> {
			T read = value.call();
			return read == null ? null : Reading.of(read);
		};
	}

	/**
	 * Reads a source for one backend.
	 *
	 * @param <T> type of the value
	 */
	@FunctionalInterface
	interface Reader<T> {
		/**
		 * Reads the value to send now.
		 *
		 * @return the value, or {@code null} to send nothing this time
		 * @throws Exception when the value cannot be computed
		 */
		@Nullable Reading<T> read() throws Exception;
	}

	/**
	 * A value read for one backend, which the backend commits once it is sure the value arrived.
	 *
	 * @param <T> type of the value
	 */
	final class Reading<T> {
		private static final Runnable NOTHING = () -> {
		};

		private final @NotNull T value;
		private final @NotNull Runnable commit;

		private Reading(@NotNull T value, @NotNull Runnable commit) {
			this.value = Objects.requireNonNull(value, "value");
			this.commit = Objects.requireNonNull(commit, "commit");
		}

		/**
		 * Returns a reading with nothing to commit.
		 *
		 * @param value the value
		 * @param <T>   type of the value
		 * @return the reading
		 */
		public static <T> @NotNull Reading<T> of(@NotNull T value) {
			return new Reading<>(value, NOTHING);
		}

		/**
		 * Returns a reading that runs {@code commit} once its value arrived.
		 *
		 * @param value  the value
		 * @param commit marks the value as sent
		 * @param <T>    type of the value
		 * @return the reading
		 */
		public static <T> @NotNull Reading<T> of(@NotNull T value, @NotNull Runnable commit) {
			return new Reading<>(value, commit);
		}

		/**
		 * Returns the value to send.
		 *
		 * @return the value
		 */
		public @NotNull T value() {
			return value;
		}

		/**
		 * Marks the value as sent, so the next reading of the same reader starts after it.
		 */
		public void commit() {
			commit.run();
		}
	}
}
