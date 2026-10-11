package me.whereareiam.statistica.adapter.redis;

import me.whereareiam.statistica.Election;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.params.SetParams;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Elects the instance that sends shared metrics through a lease in a Redis every instance uses.
 * <p>
 * Use it when the shared metrics come from Redis. When they come from a database, elect through
 * that database instead, because instances may share the database without sharing a Redis.
 * <p>
 * The lease is one key holding the holder's id, which expires on its own. An instance takes it
 * when it is free and renews it while it holds it; when the holder stops, it deletes the key.
 *
 * <pre>{@code
 * RedisElection election = RedisElection.of(jedis, "myplugin:statistica:lease");
 * }</pre>
 */
public final class RedisElection implements Election {
	/** How long a lease lasts without renewal, by default. Longer than a backend's interval of 30 minutes. */
	public static final Duration DEFAULT_LEASE = Duration.ofHours(1);

	private static final String RENEW = """
			if redis.call('get', KEYS[1]) == ARGV[1] then
				return redis.call('pexpire', KEYS[1], ARGV[2])
			end
			return 0""";

	private static final String RELEASE = """
			if redis.call('get', KEYS[1]) == ARGV[1] then
				return redis.call('del', KEYS[1])
			end
			return 0""";

	private final @NotNull UnifiedJedis redis;
	private final @NotNull String key;
	private final long leaseMillis;
	private final @NotNull String holder = UUID.randomUUID().toString();

	private RedisElection(@NotNull UnifiedJedis redis, @NotNull String key, @NotNull Duration lease) {
		Objects.requireNonNull(key, "key");
		if (key.isBlank()) throw new IllegalArgumentException("The key must not be blank");
		if (lease.isNegative() || lease.isZero()) throw new IllegalArgumentException("The lease must be positive: " + lease);

		this.redis = Objects.requireNonNull(redis, "redis");
		this.key = key;
		this.leaseMillis = lease.toMillis();
	}

	/**
	 * Returns an election with leases of {@link #DEFAULT_LEASE}.
	 *
	 * @param redis the Redis every instance uses, such as a {@code JedisPooled}
	 * @param key   key of the lease, unique to the application
	 * @return the election
	 */
	public static @NotNull RedisElection of(@NotNull UnifiedJedis redis, @NotNull String key) {
		return new RedisElection(redis, key, DEFAULT_LEASE);
	}

	/**
	 * Returns an election.
	 *
	 * @param redis the Redis every instance uses
	 * @param key   key of the lease, unique to the application
	 * @param lease how long a lease lasts without renewal; longer than the backends' interval
	 * @return the election
	 */
	public static @NotNull RedisElection of(@NotNull UnifiedJedis redis, @NotNull String key, @NotNull Duration lease) {
		return new RedisElection(redis, key, lease);
	}

	@Override
	public boolean elected() {
		String taken = redis.set(key, holder, SetParams.setParams().nx().px(leaseMillis));
		if ("OK".equals(taken)) return true;

		Object renewed = redis.eval(RENEW, List.of(key), List.of(holder, Long.toString(leaseMillis)));
		return renewed instanceof Long count && count == 1L;
	}

	@Override
	public void release() {
		redis.eval(RELEASE, List.of(key), List.of(holder));
	}
}
