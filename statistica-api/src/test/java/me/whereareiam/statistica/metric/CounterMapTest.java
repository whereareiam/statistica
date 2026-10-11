package me.whereareiam.statistica.metric;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CounterMapTest {
	private enum Outcome {
		COMPLETED,
		FAILED
	}

	@Test
	void namesEnumKeysInLowerCase() throws Exception {
		CounterMap counter = new CounterMap();

		counter.increment(Outcome.COMPLETED);
		counter.add(Outcome.FAILED, 2);

		assertEquals(Map.of("completed", 1L, "failed", 2L), counter.reader().read().value());
	}

	@Test
	void leavesOutKeysWithoutNewEvents() throws Exception {
		CounterMap counter = new CounterMap();
		MetricSource.Reader<Map<String, Long>> reader = counter.reader();

		counter.increment("completed");
		reader.read().commit();
		counter.increment("failed");

		assertEquals(Map.of("failed", 1L), reader.read().value());
	}

	@Test
	void keepsReadersApart() throws Exception {
		CounterMap counter = new CounterMap();
		MetricSource.Reader<Map<String, Long>> bstats = counter.reader();
		MetricSource.Reader<Map<String, Long>> faststats = counter.reader();

		counter.increment("completed");
		bstats.read().commit();

		assertEquals(Map.of(), bstats.read().value());
		assertEquals(Map.of("completed", 1L), faststats.read().value());
	}

	@Test
	void refusesBlankKeys() {
		assertThrows(IllegalArgumentException.class, () -> new CounterMap().increment(" "));
	}
}
