package me.whereareiam.statistica.common.error;

import me.whereareiam.statistica.error.ErrorContext;
import me.whereareiam.statistica.error.Scrub;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hides the values of an {@link ErrorContext} from an error before it is reported.
 */
public final class ErrorRedaction {
	/** What a hidden value is replaced with. */
	public static final String HIDDEN = "[value hidden]";

	private ErrorRedaction() {
	}

	/**
	 * Returns the error unchanged when none of its messages contains a value to hide, or else a chain
	 * of {@link RedactedException stand-ins} with the values hidden and the scrubs applied.
	 *
	 * @param error   the error
	 * @param context the values to hide
	 * @param scrubs  rules applied to the messages of a stand-in
	 * @return the error to report
	 */
	public static @NotNull Throwable redact(
			@NotNull Throwable error,
			@NotNull ErrorContext context,
			@NotNull List<Scrub> scrubs
	) {
		Set<String> values = context.getRedactions();
		if (values.isEmpty() || !contains(error, values)) return error;

		return copy(error, values, scrubs, Collections.newSetFromMap(new IdentityHashMap<>()));
	}

	/**
	 * Hides values in a text, ignoring case.
	 *
	 * @param text   the text
	 * @param values the values to hide
	 * @return the text with every occurrence hidden
	 */
	public static @NotNull String hide(@NotNull String text, @NotNull Collection<String> values) {
		String hidden = text;
		for (String value : values) {
			Pattern pattern = Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			hidden = pattern.matcher(hidden).replaceAll(Matcher.quoteReplacement(HIDDEN));
		}

		return hidden;
	}

	private static boolean contains(@NotNull Throwable error, @NotNull Set<String> values) {
		Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Throwable current = error; current != null && visited.add(current); current = current.getCause()) {
			String message = current.getMessage();
			if (message == null) continue;

			String lower = message.toLowerCase(Locale.ROOT);
			for (String value : values)
				if (lower.contains(value.toLowerCase(Locale.ROOT))) return true;
		}

		return false;
	}

	private static @Nullable Throwable copy(
			@Nullable Throwable error,
			@NotNull Set<String> values,
			@NotNull List<Scrub> scrubs,
			@NotNull Set<Throwable> visited
	) {
		if (error == null || !visited.add(error)) return null;

		Throwable cause = copy(error.getCause(), values, scrubs, visited);
		String message = error.getMessage();
		if (message != null) {
			message = hide(message, values);
			for (Scrub scrub : scrubs) message = scrub.apply(message);
		}

		return new RedactedException(error.getClass().getName(), message, error.getStackTrace(), cause);
	}
}
