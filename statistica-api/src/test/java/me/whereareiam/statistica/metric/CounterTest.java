package me.whereareiam.statistica.metric;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CounterTest {
	@Test
	void readsWhatWasCountedSinceTheLastCommit() throws Exception {
		Counter counter = new Counter();
		MetricSource.Reader<Long> reader = counter.reader();

		counter.add(3);
		MetricSource.Reading<Long> first = reader.read();
		first.commit();
		counter.increment();

		assertEquals(3L, first.value());
		assertEquals(1L, reader.read().value());
	}

	@Test
	void carriesAnUncommittedReadingOver() throws Exception {
		Counter counter = new Counter();
		MetricSource.Reader<Long> reader = counter.reader();

		counter.add(2);
		reader.read();
		counter.add(5);

		assertEquals(7L, reader.read().value());
	}

	@Test
	void keepsEventsCountedAfterTheCommittedReading() throws Exception {
		Counter counter = new Counter();
		MetricSource.Reader<Long> reader = counter.reader();

		counter.add(2);
		MetricSource.Reading<Long> sent = reader.read();
		counter.add(4);
		sent.commit();

		assertEquals(4L, reader.read().value());
	}

	@Test
	void keepsReadersApart() throws Exception {
		Counter counter = new Counter();
		MetricSource.Reader<Long> bstats = counter.reader();
		MetricSource.Reader<Long> faststats = counter.reader();

		counter.add(6);
		bstats.read().commit();

		assertEquals(0L, bstats.read().value());
		assertEquals(6L, faststats.read().value());
	}

	@Test
	void ignoresAnOlderCommitAfterANewerOne() throws Exception {
		Counter counter = new Counter();
		MetricSource.Reader<Long> reader = counter.reader();

		counter.add(1);
		MetricSource.Reading<Long> older = reader.read();
		counter.add(1);
		reader.read().commit();
		older.commit();

		assertEquals(0L, reader.read().value());
	}

	@Test
	void refusesToCountDown() {
		assertThrows(IllegalArgumentException.class, () -> new Counter().add(-1));
	}
}
