package me.whereareiam.statistica.common.metric;

import me.whereareiam.statistica.Election;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Asks an {@link Election} at most once per interval, so backends collecting one after another
 * reach the shared store once.
 * <p>
 * A failed election counts as not elected until the next attempt: shared metrics are left out
 * rather than sent by two instances.
 */
public final class CachedElection {
	/** How long a result is reused, by default. */
	public static final Duration DEFAULT_INTERVAL = Duration.ofMinutes(5);

	private static final System.Logger LOGGER = System.getLogger("Statistica");

	private final @NotNull Election election;
	private final long intervalNanos;
	private long checkedAt;
	private boolean checked;
	private boolean elected;

	/**
	 * Caches an election for {@link #DEFAULT_INTERVAL}.
	 *
	 * @param election the election
	 */
	public CachedElection(@NotNull Election election) {
		this(election, DEFAULT_INTERVAL);
	}

	/**
	 * Caches an election.
	 *
	 * @param election the election
	 * @param interval how long a result is reused
	 */
	public CachedElection(@NotNull Election election, @NotNull Duration interval) {
		this.election = election;
		this.intervalNanos = interval.toNanos();
	}

	/**
	 * Tells whether this instance sends the shared metrics, asking the election when the last
	 * answer is older than the interval.
	 *
	 * @return whether this instance is elected
	 */
	public synchronized boolean elected() {
		long now = System.nanoTime();
		if (checked && now - checkedAt < intervalNanos) return elected;

		elected = ask();
		checked = true;
		checkedAt = now;

		return elected;
	}

	/**
	 * Gives up this instance's claim. The next {@link #elected()} asks the election again.
	 */
	public synchronized void release() {
		checked = false;
		elected = false;
		try {
			election.release();
		} catch (Exception failure) {
			LOGGER.log(System.Logger.Level.WARNING, "Failed to release the election of shared metrics", failure);
		}
	}

	private boolean ask() {
		try {
			return election.elected();
		} catch (Exception failure) {
			LOGGER.log(System.Logger.Level.WARNING, "Failed to elect the instance sending shared metrics", failure);
			return false;
		}
	}
}
