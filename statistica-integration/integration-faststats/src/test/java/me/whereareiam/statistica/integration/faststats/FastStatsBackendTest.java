package me.whereareiam.statistica.integration.faststats;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.config.SimpleConfig;
import dev.faststats.internal.LoggerFactory;
import dev.faststats.internal.PlatformLoggerFactory;
import me.whereareiam.statistica.error.ErrorContext;
import me.whereareiam.statistica.error.ErrorPolicy;
import me.whereareiam.statistica.metric.Counter;
import me.whereareiam.statistica.metric.MetricSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the backend against FastStats' own submission code and a local server standing in for
 * FastStats, so the test sees exactly what would leave the server.
 */
class FastStatsBackendTest {
	private static final String TOKEN = "0123456789abcdef0123456789abcdef";

	private final Map<String, String> received = new ConcurrentHashMap<>();
	private HttpServer server;

	@TempDir
	Path directory;

	@BeforeEach
	void start() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			try (InputStream body = new GZIPInputStream(exchange.getRequestBody())) {
				String content = new String(body.readAllBytes(), StandardCharsets.UTF_8);
				received.merge(exchange.getRequestURI().getPath(), content, String::concat);
			}
			byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, response.length);
			exchange.getResponseBody().write(response);
			exchange.close();
		});
		server.start();

		String url = "http://127.0.0.1:" + server.getAddress().getPort();
		System.setProperty("faststats.metrics-server", url);
		System.setProperty("faststats.error-tracker-server", url);
	}

	@AfterEach
	void stop() {
		server.stop(0);
		System.clearProperty("faststats.metrics-server");
		System.clearProperty("faststats.error-tracker-server");
	}

	@Test
	void sendsMetricsAndErrorsWithoutWhatWasHidden() throws Exception {
		Counter logins = new Counter();
		logins.add(2);
		FastStatsBackend backend = backend(MetricSet.builder()
				.string("storage", () -> "H2")
				.number("logins", logins)
				.build());

		backend.start();
		backend.errors().report(
				new IllegalStateException("Registration of Alice failed", new IllegalArgumentException(
						"arguments:{positional:{0:alice}} for 0b7c2a1e-3f4d-4c5b-9a8e-1f2d3c4b5a69"
				)),
				ErrorContext.builder().attribute("message", "Registration of %s failed").redact("Alice").build()
		);
		backend.stop();

		String metrics = received.get("/v1/collect");
		String errors = received.get("/v1/error");
		assertTrue(metrics.contains("\"storage\":\"H2\""), metrics);
		assertTrue(metrics.contains("\"logins\":2"), metrics);
		assertTrue(errors.contains("Registration of [value hidden] failed"), errors);
		assertTrue(errors.contains("\"message\":\"Registration of %s failed\""), errors);
		assertTrue(errors.contains("[uuid hidden]"), errors);
		assertFalse(errors.toLowerCase().contains("alice"), errors);
		assertFalse(errors.contains("0b7c2a1e"), errors);
	}

	@Test
	void keepsNothingWhenErrorTrackingIsOff() throws Exception {
		writeConfig(false);
		FastStatsBackend backend = backend(MetricSet.empty());

		backend.start();
		backend.errors().report(new IllegalStateException("broken"), ErrorContext.empty());
		backend.stop();

		assertFalse(received.containsKey("/v1/error"));
	}

	private FastStatsBackend backend(MetricSet metrics) throws IOException {
		Path config = config();
		if (!Files.exists(config)) writeConfig(true);

		return new FastStatsBackend(
				metrics,
				ErrorPolicy.builder().captureUncaught(false).build(),
				getClass().getClassLoader(),
				() -> new TestContext.Factory(config).token(TOKEN)
		);
	}

	private void writeConfig(boolean errors) throws IOException {
		Files.writeString(config(), """
				enabled=true
				submitMetrics=true
				submitAdditionalMetrics=true
				submitErrors=%s
				serverId=11111111-2222-3333-4444-555555555555
				debug=false
				configVersion=3
				""".formatted(errors));
	}

	private Path config() {
		return directory.resolve("config.properties");
	}

	/**
	 * A FastStats context of a platform that has nothing to add and no scheduler: everything is
	 * sent when it shuts down.
	 */
	static final class TestContext extends SimpleContext {
		private TestContext(Factory factory, LoggerFactory loggers, Path config, String token) {
			super(factory, loggers, SimpleConfig.read(config, loggers), "test", token);
			initializeServices(factory);
		}

		@Override
		protected boolean preSubmissionStart() {
			return true;
		}

		@Override
		public String getProjectName() {
			return "statistica-test";
		}

		@Override
		protected Metrics.Factory metricsFactory() {
			return new SimpleMetrics.Factory(this) {
				@Override
				public Metrics create() {
					return new SimpleMetrics(this) {
						@Override
						protected void appendDefaultData(JsonObject metrics) {
						}
					};
				}
			};
		}

		@Override
		protected void scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
		}

		static final class Factory extends SimpleContext.Factory<TestContext, Factory> {
			private final Path config;
			private String token;

			Factory(Path config) {
				this.config = config;
			}

			Factory token(String token) {
				this.token = token;
				return this;
			}

			@Override
			public TestContext create() {
				return new TestContext(this, new PlatformLoggerFactory((level, error, message) -> {
				}), config, token);
			}
		}
	}
}
