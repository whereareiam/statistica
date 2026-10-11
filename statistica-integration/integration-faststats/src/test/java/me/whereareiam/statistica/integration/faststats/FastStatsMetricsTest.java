package me.whereareiam.statistica.integration.faststats;

import com.google.gson.JsonElement;
import dev.faststats.data.Metric;
import me.whereareiam.statistica.metric.Counter;
import me.whereareiam.statistica.metric.MetricSet;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FastStatsMetricsTest {
	@Test
	void mapsEveryKind() throws Exception {
		FastStatsMetrics mapping = new FastStatsMetrics();
		MetricSet metrics = MetricSet.builder()
				.string("storage", () -> "H2")
				.stringList("providers", () -> List.of("credential", "premium"))
				.number("accounts", () -> 3)
				.numberMap("logins", () -> Map.of("completed", 2))
				.groupedMap("modules", () -> Map.of("official", Map.of("premium", 1)))
				.build();

		List<Metric<?>> mapped = metrics.getMetrics().stream().<Metric<?>>map(metric -> metric.accept(mapping)).toList();

		assertEquals("\"H2\"", data(mapped.get(0)));
		assertEquals("[\"credential\",\"premium\"]", data(mapped.get(1)));
		assertEquals("3", data(mapped.get(2)));
		assertEquals("{\"completed\":2}", data(mapped.get(3)));
		assertEquals("{\"official:premium\":1}", data(mapped.get(4)));
	}

	@Test
	void commitsWhatTheFlushingThreadRead() throws Exception {
		FastStatsMetrics mapping = new FastStatsMetrics();
		Counter logins = new Counter();
		Metric<?> metric = MetricSet.builder().number("logins", logins).build().getMetrics().get(0).accept(mapping);

		logins.add(3);
		assertEquals("3", data(metric));
		mapping.flush();
		logins.add(2);

		assertEquals("2", data(metric));
	}

	@Test
	void ignoresReadingsOfOtherThreads() throws Exception {
		FastStatsMetrics mapping = new FastStatsMetrics();
		Counter logins = new Counter();
		Metric<?> metric = MetricSet.builder().number("logins", logins).build().getMetrics().get(0).accept(mapping);

		logins.add(3);
		CompletableFuture.runAsync(() -> {
			try {
				data(metric);
			} catch (Exception failure) {
				throw new IllegalStateException(failure);
			}
		}).join();
		mapping.flush();

		assertEquals("3", data(metric));
	}

	private static String data(Metric<?> metric) throws Exception {
		return metric.getData().map(JsonElement::toString).orElse(null);
	}
}
