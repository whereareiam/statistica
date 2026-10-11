package me.whereareiam.statistica.error;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Hides part of an error message before it leaves the server.
 * <p>
 * The replacement may refer to groups of the pattern, as in
 * {@link java.util.regex.Matcher#replaceAll(String)}. Backends hide addresses, home directories and
 * database passwords on their own; these rules add what a plugin's own errors may contain.
 */
@Getter
public final class Scrub {
	/** Unique ids, such as player and account ids. */
	public static final Scrub UNIQUE_IDS = of(
			"(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b",
			"[uuid hidden]"
	);

	/** E-mail addresses. */
	public static final Scrub EMAIL_ADDRESSES = of(
			"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}",
			"[email hidden]"
	);

	/** Values Jdbi lists after a failed statement. */
	public static final Scrub STATEMENT_ARGUMENTS = of("(?s)arguments:\\{.*}", "arguments:{[values hidden]}");

	/** The value MySQL and MariaDB name in a duplicate key error. */
	public static final Scrub DUPLICATE_ENTRIES = of(
			"Duplicate entry '.*?' for key",
			"Duplicate entry '[value hidden]' for key"
	);

	/** The values PostgreSQL names in constraint errors. */
	public static final Scrub CONSTRAINT_VALUES = of(
			"Key \\(([^)]*)\\)=\\(.*?\\)( already exists| is not present|)",
			"Key ($1)=([value hidden])$2"
	);

	/** The row PostgreSQL shows when a row breaks a constraint. */
	public static final Scrub FAILING_ROWS = of("Failing row contains \\(.*?\\)", "Failing row contains ([values hidden])");

	/** The values H2 names in constraint errors. */
	public static final Scrub CONSTRAINT_ROWS = of("(?s)VALUES \\(.*?\\)\"", "VALUES ([values hidden])\"");

	private static final List<Scrub> DEFAULTS = List.of(
			UNIQUE_IDS,
			EMAIL_ADDRESSES,
			STATEMENT_ARGUMENTS,
			DUPLICATE_ENTRIES,
			CONSTRAINT_VALUES,
			FAILING_ROWS,
			CONSTRAINT_ROWS
	);

	/** What to hide. */
	private final @NotNull Pattern pattern;

	/** What to put in its place. */
	private final @NotNull String replacement;

	private Scrub(@NotNull Pattern pattern, @NotNull String replacement) {
		this.pattern = Objects.requireNonNull(pattern, "pattern");
		this.replacement = Objects.requireNonNull(replacement, "replacement");
	}

	/**
	 * Returns a rule.
	 *
	 * @param pattern     what to hide
	 * @param replacement what to put in its place
	 * @return the rule
	 */
	public static @NotNull Scrub of(@NotNull Pattern pattern, @NotNull String replacement) {
		return new Scrub(pattern, replacement);
	}

	/**
	 * Returns a rule.
	 *
	 * @param regex       what to hide
	 * @param replacement what to put in its place
	 * @return the rule
	 */
	public static @NotNull Scrub of(@NotNull String regex, @NotNull String replacement) {
		return new Scrub(Pattern.compile(regex), replacement);
	}

	/**
	 * Returns the rules every {@link ErrorPolicy} starts with: unique ids, e-mail addresses, and the
	 * values databases and Jdbi put into their error messages.
	 *
	 * @return the default rules
	 */
	public static @NotNull List<Scrub> defaults() {
		return DEFAULTS;
	}

	/**
	 * Applies this rule to a text.
	 *
	 * @param text the text
	 * @return the text with every match replaced
	 */
	public @NotNull String apply(@NotNull String text) {
		return pattern.matcher(text).replaceAll(replacement);
	}
}
