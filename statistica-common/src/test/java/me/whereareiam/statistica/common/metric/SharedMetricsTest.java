package me.whereareiam.statistica.common.metric;

import me.whereareiam.statistica.metric.Metric;
import me.whereareiam.statistica.metric.MetricSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SharedMetricsTest {
	private static final MetricSet METRICS = MetricSet.builder()
			.number("logins", () -> 2)
			.total("accounts", () -> 7)
			.build();

	@Test
	void sendsSharedMetricsOnTheElectedInstance() throws Exception {
		MetricSet elected = SharedMetrics.elect(METRICS, new CachedElection(() -> true));

		assertEquals(7L, elected.getMetrics().get(1).getSource().reader().read().value());
	}

	@Test
	void sendsOnlyItsOwnMetricsOnAnotherInstance() throws Exception {
		MetricSet elected = SharedMetrics.elect(METRICS, new CachedElection(() -> false));
		Metric<?> logins = elected.getMetrics().get(0);
		Metric<?> accounts = elected.getMetrics().get(1);

		assertNotNull(logins.getSource().reader().read());
		assertNull(accounts.getSource().reader().read());
	}
}
