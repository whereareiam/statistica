package me.whereareiam.statistica.common.error;

import me.whereareiam.statistica.error.ErrorPolicy;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.sql.SQLException;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorFilterTest {
	private static final ClassLoader PLUGIN = ErrorFilterTest.class.getClassLoader();

	@Test
	void acceptsErrorsThrownByThePlugin() {
		ErrorFilter filter = new ErrorFilter(ErrorPolicy.defaults(), PLUGIN);

		assertTrue(filter.accepts(new IllegalStateException("broken")));
	}

	@Test
	void passesOverPlatformFramesToThePluginsCode() {
		ErrorFilter filter = new ErrorFilter(ErrorPolicy.defaults(), PLUGIN);
		RuntimeException error = new RuntimeException();
		error.setStackTrace(new StackTraceElement[]{
				frame("java.util.Objects"),
				frame(ErrorFilterTest.class.getName())
		});

		assertTrue(filter.owned(error));
	}

	@Test
	void refusesErrorsThrownBySomeoneElsesCode() {
		ErrorFilter filter = new ErrorFilter(ErrorPolicy.defaults(), PLUGIN);
		RuntimeException error = new RuntimeException();
		error.setStackTrace(new StackTraceElement[]{
				frame("com.example.extension.Provider"),
				frame(ErrorFilterTest.class.getName())
		});

		assertFalse(filter.owned(error));
	}

	@Test
	void decidesByTheDeepestCause() {
		ErrorFilter filter = new ErrorFilter(ErrorPolicy.defaults(), PLUGIN);
		RuntimeException cause = new RuntimeException();
		cause.setStackTrace(new StackTraceElement[]{frame("com.example.extension.Provider")});

		assertFalse(filter.owned(new IllegalStateException("wrapped", cause)));
	}

	@Test
	void refusesErrorsWithOnlyPlatformFrames() {
		ErrorFilter filter = new ErrorFilter(ErrorPolicy.defaults(), PLUGIN);
		RuntimeException error = new RuntimeException();
		error.setStackTrace(new StackTraceElement[]{frame("java.lang.Thread")});

		assertFalse(filter.owned(error));
	}

	@Test
	void refusesIgnoredTypesAlsoAsCause() {
		ErrorFilter filter = new ErrorFilter(ErrorPolicy.builder().ignore(ConnectException.class).build(), PLUGIN);

		assertFalse(filter.accepts(new SQLException("no connection", new ConnectException("refused"))));
	}

	@Test
	void refusesIgnoredMessages() {
		ErrorPolicy policy = ErrorPolicy.builder().ignore(Pattern.compile("Access denied")).build();
		ErrorFilter filter = new ErrorFilter(policy, PLUGIN);

		assertFalse(filter.accepts(new SQLException("Access denied for user")));
	}

	@Test
	void stopsAtTheLimit() {
		ErrorFilter filter = new ErrorFilter(ErrorPolicy.builder().limit(2).build(), PLUGIN);

		assertTrue(filter.accepts(new IllegalStateException()));
		assertTrue(filter.accepts(new IllegalStateException()));
		assertFalse(filter.accepts(new IllegalStateException()));
	}

	private static StackTraceElement frame(String className) {
		return new StackTraceElement(className, "run", null, 1);
	}
}
