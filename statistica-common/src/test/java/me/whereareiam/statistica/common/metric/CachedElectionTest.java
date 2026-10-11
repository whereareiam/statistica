package me.whereareiam.statistica.common.metric;

import me.whereareiam.statistica.Election;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CachedElectionTest {
	@Test
	void asksOncePerInterval() {
		AtomicInteger asked = new AtomicInteger();
		CachedElection election = new CachedElection(() -> asked.incrementAndGet() > 0, Duration.ofHours(1));

		assertTrue(election.elected());
		assertTrue(election.elected());
		assertEquals(1, asked.get());
	}

	@Test
	void treatsAFailureAsNotElected() {
		CachedElection election = new CachedElection(() -> {
			throw new IllegalStateException("unreachable");
		});

		assertFalse(election.elected());
	}

	@Test
	void asksAgainAfterARelease() {
		AtomicInteger asked = new AtomicInteger();
		AtomicInteger released = new AtomicInteger();
		CachedElection election = new CachedElection(new Election() {
			@Override
			public boolean elected() {
				return asked.incrementAndGet() > 0;
			}

			@Override
			public void release() {
				released.incrementAndGet();
			}
		}, Duration.ofHours(1));

		election.elected();
		election.release();
		election.elected();

		assertEquals(2, asked.get());
		assertEquals(1, released.get());
	}
}
