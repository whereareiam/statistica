package me.whereareiam.statistica.adapter.redis;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("redis-container")
class RedisElectionTest {
	private static final String KEY = "test:statistica:lease";
	private static final RedisContainer REDIS = new RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag("7"));
	private static JedisPooled redis;

	@BeforeAll
	static void start() {
		REDIS.start();
		redis = new JedisPooled(REDIS.getHost(), REDIS.getFirstMappedPort());
	}

	@AfterAll
	static void stop() {
		redis.close();
		REDIS.stop();
	}

	@BeforeEach
	void clear() {
		redis.del(KEY);
	}

	@Test
	void electsOneInstanceAtATime() {
		RedisElection first = RedisElection.of(redis, KEY);
		RedisElection second = RedisElection.of(redis, KEY);

		assertTrue(first.elected());
		assertFalse(second.elected());
		assertTrue(first.elected());
	}

	@Test
	void handsTheLeaseOverOnRelease() {
		RedisElection first = RedisElection.of(redis, KEY);
		RedisElection second = RedisElection.of(redis, KEY);

		first.elected();
		first.release();

		assertTrue(second.elected());
		assertFalse(first.elected());
	}

	@Test
	void takesAnExpiredLeaseOver() throws InterruptedException {
		RedisElection first = RedisElection.of(redis, KEY, Duration.ofMillis(50));
		RedisElection second = RedisElection.of(redis, KEY);

		first.elected();
		Thread.sleep(150);

		assertTrue(second.elected());
	}
}
