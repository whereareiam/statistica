package me.whereareiam.statistica.common.error;

import me.whereareiam.statistica.error.ErrorContext;
import me.whereareiam.statistica.error.Scrub;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class ErrorRedactionTest {
	@Test
	void keepsAnErrorWithoutTheValues() {
		IllegalStateException error = new IllegalStateException("Step failed");
		ErrorContext context = ErrorContext.builder().redact("Alice").build();

		assertSame(error, ErrorRedaction.redact(error, context, Scrub.defaults()));
	}

	@Test
	void hidesValuesInEveryMessageOfTheChain() {
		SQLException cause = new SQLException("Duplicate entry 'alice' for key 'username'");
		IllegalStateException error = new IllegalStateException("Registration of Alice failed", cause);
		ErrorContext context = ErrorContext.builder().redact("Alice").build();

		Throwable redacted = ErrorRedaction.redact(error, context, Scrub.defaults());

		assertInstanceOf(RedactedException.class, redacted);
		assertEquals("java.lang.IllegalStateException: Registration of [value hidden] failed", redacted.getMessage());
		assertEquals(
				"java.sql.SQLException: Duplicate entry '[value hidden]' for key 'username'",
				redacted.getCause().getMessage()
		);
		assertArrayEquals(error.getStackTrace(), redacted.getStackTrace());
	}

	@Test
	void skipsValuesTooShortToHide() {
		ErrorContext context = ErrorContext.builder().redact("Al").redact(null).build();

		assertEquals(0, context.getRedactions().size());
	}

	@Test
	void scrubsDatabaseValues() {
		String message = "Key (username)=(alice) already exists. Failing row contains (1, alice).";

		String scrubbed = message;
		for (Scrub scrub : Scrub.defaults()) scrubbed = scrub.apply(scrubbed);

		assertEquals("Key (username)=([value hidden]) already exists. Failing row contains ([values hidden]).", scrubbed);
	}

	@Test
	void scrubsStatementArgumentsAndIds() {
		String message = "Unique violation [statement:\"INSERT INTO accounts VALUES (?)\", "
				+ "arguments:{positional:{0:alice}, named:{}, finder:[]}] for 0b7c2a1e-3f4d-4c5b-9a8e-1f2d3c4b5a69";

		String scrubbed = message;
		for (Scrub scrub : List.of(Scrub.STATEMENT_ARGUMENTS, Scrub.UNIQUE_IDS)) scrubbed = scrub.apply(scrubbed);

		assertEquals(
				"Unique violation [statement:\"INSERT INTO accounts VALUES (?)\", arguments:{[values hidden]}] for [uuid hidden]",
				scrubbed
		);
	}
}
