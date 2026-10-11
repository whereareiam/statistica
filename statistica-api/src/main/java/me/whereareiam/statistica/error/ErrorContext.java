package me.whereareiam.statistica.error;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The circumstances of one reported error.
 * <p>
 * Attributes say where the error happened, such as the step or stage that failed, so it can be
 * reproduced. Redacted values are hidden wherever they appear in the error's messages and causes:
 * pass the name and id of the player the error happened to, which no pattern could recognise.
 */
@Getter
public final class ErrorContext {
	private static final ErrorContext EMPTY = new ErrorContext(Map.of(), Set.of());

	/** The shortest value that is hidden; shorter ones would hide unrelated parts of a message. */
	public static final int MIN_REDACTED_LENGTH = 3;

	/** Attributes sent with the error, each a text, a number or a boolean. */
	private final @NotNull Map<String, Object> attributes;

	/** Values to hide from the error's messages. */
	private final @NotNull Set<String> redactions;

	private ErrorContext(@NotNull Map<String, Object> attributes, @NotNull Set<String> redactions) {
		this.attributes = Map.copyOf(attributes);
		this.redactions = Set.copyOf(redactions);
	}

	/**
	 * Returns a context without attributes or values to hide.
	 *
	 * @return the empty context
	 */
	public static @NotNull ErrorContext empty() {
		return EMPTY;
	}

	/**
	 * Returns a context with a single attribute.
	 *
	 * @param key   attribute name
	 * @param value attribute value
	 * @return the context
	 */
	public static @NotNull ErrorContext of(@NotNull String key, @NotNull String value) {
		return builder().attribute(key, value).build();
	}

	/**
	 * Starts the description of a context.
	 *
	 * @return a new builder
	 */
	public static @NotNull Builder builder() {
		return new Builder();
	}

	/**
	 * Describes the circumstances of one error.
	 */
	public static final class Builder {
		private final Map<String, Object> attributes = new LinkedHashMap<>();
		private final Set<String> redactions = new LinkedHashSet<>();

		private Builder() {
		}

		/**
		 * Adds a text attribute.
		 *
		 * @param key   attribute name
		 * @param value attribute value
		 * @return this builder
		 */
		public @NotNull Builder attribute(@NotNull String key, @NotNull String value) {
			return put(key, value);
		}

		/**
		 * Adds a number attribute.
		 *
		 * @param key   attribute name
		 * @param value attribute value
		 * @return this builder
		 */
		public @NotNull Builder attribute(@NotNull String key, @NotNull Number value) {
			return put(key, value);
		}

		/**
		 * Adds a boolean attribute.
		 *
		 * @param key   attribute name
		 * @param value attribute value
		 * @return this builder
		 */
		public @NotNull Builder attribute(@NotNull String key, boolean value) {
			return put(key, value);
		}

		/**
		 * Hides a value wherever it appears in the error's messages, ignoring case. A {@code null}
		 * value, and one shorter than {@value #MIN_REDACTED_LENGTH} characters, is skipped.
		 *
		 * @param value the value to hide, such as a player's name
		 * @return this builder
		 */
		public @NotNull Builder redact(@Nullable Object value) {
			if (value == null) return this;

			String text = value.toString().trim();
			if (text.length() >= MIN_REDACTED_LENGTH) redactions.add(text);

			return this;
		}

		/**
		 * Finishes the context.
		 *
		 * @return the context
		 */
		public @NotNull ErrorContext build() {
			return new ErrorContext(attributes, redactions);
		}

		private @NotNull Builder put(@NotNull String key, @NotNull Object value) {
			Objects.requireNonNull(key, "key");
			Objects.requireNonNull(value, "value");
			if (key.isBlank()) throw new IllegalArgumentException("An attribute name must not be blank");

			attributes.put(key, value);

			return this;
		}
	}
}
