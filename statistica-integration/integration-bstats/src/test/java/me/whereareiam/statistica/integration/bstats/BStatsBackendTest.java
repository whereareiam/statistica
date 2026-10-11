package me.whereareiam.statistica.integration.bstats;

import me.whereareiam.statistica.metric.Counter;
import me.whereareiam.statistica.metric.MetricSet;
import org.bstats.charts.CustomChart;
import org.bstats.json.JsonObjectBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BStatsBackendTest {
	@Test
	void mapsEveryKindToItsChart() {
		Counter logins = new Counter();
		logins.add(4);
		Map<String, Map<String, Integer>> modules = Map.of("official", Map.of("premium", 1));

		Map<String, String> charts = charts(MetricSet.builder()
				.string("storage", () -> "H2")
				.stringList("providers", () -> List.of("credential", "premium"))
				.number("logins", logins)
				.numberMap("accounts", () -> Map.of("credential", 3))
				.groupedMap("modules", () -> modules)
				.build());

		assertEquals("{\"chartId\":\"storage\",\"data\":{\"value\":\"H2\"}}", charts.get("storage"));
		assertEquals(
				"{\"chartId\":\"providers\",\"data\":{\"values\":{\"credential\":1,\"premium\":1}}}",
				charts.get("providers")
		);
		assertEquals("{\"chartId\":\"logins\",\"data\":{\"value\":4}}", charts.get("logins"));
		assertEquals("{\"chartId\":\"accounts\",\"data\":{\"values\":{\"credential\":3}}}", charts.get("accounts"));
		assertEquals(
				"{\"chartId\":\"modules\",\"data\":{\"values\":{\"official\":{\"premium\":1}}}}",
				charts.get("modules")
		);
	}

	@Test
	void sendsACounterOnce() {
		Counter logins = new Counter();
		logins.add(4);
		List<CustomChart> charts = new ArrayList<>();
		BStatsBackend backend = new BStatsBackend(
				MetricSet.builder().number("logins", logins).build(),
				() -> new BStatsBackend.Connection(charts::add, () -> {
				})
		);
		backend.start();

		CustomChart chart = charts.get(0);
		chart.getRequestJsonObject((message, error) -> {
		}, false);

		assertNull(chart.getRequestJsonObject((message, error) -> {
		}, false));
	}

	@Test
	void shutsTheConnectionDownOnStop() {
		AtomicBoolean shutdown = new AtomicBoolean();
		BStatsBackend backend = new BStatsBackend(MetricSet.empty(), () -> new BStatsBackend.Connection(chart -> {
		}, () -> shutdown.set(true)));

		backend.start();
		backend.stop();

		assertTrue(shutdown.get());
	}

	private static Map<String, String> charts(MetricSet metrics) {
		Map<String, String> charts = new LinkedHashMap<>();
		new BStatsBackend(metrics, () -> new BStatsBackend.Connection(chart -> {
			JsonObjectBuilder.JsonObject json = chart.getRequestJsonObject((message, error) -> {
				throw new AssertionError(message, error);
			}, true);
			charts.put(json.toString().replaceAll(".*\"chartId\":\"([a-z_]+)\".*", "$1"), json.toString());
		}, () -> {
		})).start();

		return charts;
	}
}
