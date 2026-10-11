package me.whereareiam.statistica.metric;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetricSetTest {
	@Test
	void keepsDeclarationOrderAndScopes() {
		MetricSet metrics = MetricSet.builder()
				.string("storage", () -> "H2")
				.total("accounts", () -> 5)
				.build();

		assertEquals(List.of("storage", "accounts"), metrics.getMetrics().stream().map(Metric::getId).toList());
		assertEquals(MetricScope.INSTANCE, metrics.getMetrics().get(0).getScope());
		assertEquals(MetricScope.SHARED, metrics.getMetrics().get(1).getScope());
	}

	@Test
	void refusesInvalidAndRepeatedIds() {
		assertThrows(IllegalArgumentException.class, () -> MetricSet.builder().string("Storage", () -> "H2"));
		assertThrows(IllegalArgumentException.class, () -> MetricSet.builder()
				.string("storage", () -> "H2")
				.number("storage", () -> 1));
	}

	@Test
	void sendsNothingForBlankText() throws Exception {
		Metric<?> metric = MetricSet.builder().string("storage", () -> "  ").build().getMetrics().get(0);

		assertNull(metric.getSource().reader().read());
	}

	@Test
	void dropsBlankAndRepeatedValuesFromLists() throws Exception {
		StringListMetric metric = (StringListMetric) MetricSet.builder()
				.stringList("modules", () -> Arrays.asList("a", " ", null, "a", "b"))
				.build()
				.getMetrics()
				.get(0);

		assertEquals(List.of("a", "b"), metric.getSource().reader().read().value());
	}

	@Test
	void dropsEmptyEntriesFromMaps() throws Exception {
		Map<String, Integer> values = new LinkedHashMap<>();
		values.put("credential", 3);
		values.put(" ", 1);
		values.put("premium", null);
		NumberMapMetric metric = (NumberMapMetric) MetricSet.builder()
				.numberMap("accounts", () -> values)
				.build()
				.getMetrics()
				.get(0);

		assertEquals(Map.of("credential", 3L), metric.getSource().reader().read().value());
	}
}
