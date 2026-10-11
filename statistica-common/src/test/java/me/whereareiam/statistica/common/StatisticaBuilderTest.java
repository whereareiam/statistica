package me.whereareiam.statistica.common;

import me.whereareiam.statistica.Statistica;
import me.whereareiam.statistica.error.ErrorContext;
import me.whereareiam.statistica.error.ErrorPolicy;
import me.whereareiam.statistica.error.ErrorReporter;
import me.whereareiam.statistica.metric.MetricSet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatisticaBuilderTest {
	private final List<String> events = new ArrayList<>();

	@Test
	void refusesToBuildWithoutABackend() {
		assertThrows(IllegalStateException.class, () -> new TestBuilder().build());
	}

	@Test
	void refusesMalformedTokens() {
		assertThrows(IllegalArgumentException.class, () -> new TestBuilder().faststats("not-a-token"));
		assertThrows(IllegalArgumentException.class, () -> new TestBuilder().bstats(0));
	}

	@Test
	void startsAndStopsEveryBackendOnce() {
		Statistica statistica = new TestBuilder()
				.bstats(1)
				.faststats("0123456789abcdef0123456789abcdef")
				.build();

		statistica.stop();
		statistica.start();
		statistica.start();
		statistica.stop();
		statistica.stop();

		assertEquals(List.of("start bstats", "start faststats", "stop bstats", "stop faststats"), events);
	}

	@Test
	void keepsStartingWhenABackendFails() {
		Statistica statistica = new TestBuilder()
				.failing()
				.bstats(1)
				.faststats("0123456789abcdef0123456789abcdef")
				.build();

		statistica.start();

		assertEquals(List.of("start faststats"), events);
	}

	@Test
	void reportsErrorsToEveryBackend() {
		Statistica statistica = new TestBuilder()
				.bstats(1)
				.faststats("0123456789abcdef0123456789abcdef")
				.build();

		statistica.errors().report(new IllegalStateException(), ErrorContext.empty());

		assertEquals(List.of("error bstats", "error faststats"), events);
	}

	private final class TestBuilder extends StatisticaBuilder<TestBuilder> {
		private boolean failing;

		TestBuilder bstats(int serviceId) {
			return bstatsService(serviceId);
		}

		TestBuilder failing() {
			failing = true;
			return this;
		}

		@Override
		protected @NotNull Backend createBStats(int serviceId, @NotNull MetricSet metrics) {
			return new RecordingBackend("bstats", failing);
		}

		@Override
		protected @NotNull Backend createFastStats(
				@NotNull String token,
				@NotNull MetricSet metrics,
				@NotNull ErrorPolicy errors
		) {
			return new RecordingBackend("faststats", false);
		}
	}

	private final class RecordingBackend implements Backend {
		private final String name;
		private final boolean failing;

		private RecordingBackend(String name, boolean failing) {
			this.name = name;
			this.failing = failing;
		}

		@Override
		public void start() {
			if (failing) throw new IllegalStateException("unreachable");

			events.add("start " + name);
		}

		@Override
		public void stop() {
			events.add("stop " + name);
		}

		@Override
		public @NotNull ErrorReporter errors() {
			return (error, context) -> events.add("error " + name);
		}
	}
}
