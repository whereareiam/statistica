package me.whereareiam.statistica.error;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Which errors a plugin reports, and what is hidden from them.
 * <p>
 * Only errors of the plugin's own code are reported: those thrown from a class one of the owning
 * class loaders defined. The plugin's class loader always owns; add the loaders of modules the
 * plugin ships separately with {@link Builder#owner(ClassLoader)}, or {@link Builder#owners(Supplier)}
 * for modules it reloads. An error thrown from someone
 * else's code, such as a third-party extension, is not the plugin's to send.
 *
 * <pre>{@code
 * ErrorPolicy policy = ErrorPolicy.builder()
 *         .ignore(ConnectException.class)          // an unreachable database is a setup problem, not a bug
 *         .attribute("storage", storage.name())
 *         .build();
 * }</pre>
 */
@Getter
public final class ErrorPolicy {
	/** Reports kept between two submissions at most, by default. */
	public static final int DEFAULT_LIMIT = 100;

	/** Error types that are never reported, also when they are the cause of another error. */
	private final @NotNull Set<Class<? extends Throwable>> ignoredTypes;

	/** Errors whose message, or one of whose causes' messages, matches are never reported. */
	private final @NotNull List<Pattern> ignoredMessages;

	/** Rules applied to every message before it leaves the server, in order. */
	private final @NotNull List<Scrub> scrubs;

	/** Class loaders whose classes are the plugin's own code, besides the plugin's. */
	private final @NotNull List<ClassLoader> owners;

	/** Asked on every report for further owning class loaders, such as those of reloadable modules. */
	private final @NotNull Supplier<? extends Collection<ClassLoader>> ownerSource;

	/** Attributes sent with every error, each a text, a number or a boolean. */
	private final @NotNull Map<String, Object> attributes;

	/** Whether errors of the plugin's code that no handler caught are reported as well. */
	private final boolean captureUncaught;

	/** Reports kept between two submissions at most; further ones are dropped. */
	private final int limit;

	private ErrorPolicy(@NotNull Builder builder) {
		this.ignoredTypes = Set.copyOf(builder.ignoredTypes);
		this.ignoredMessages = List.copyOf(builder.ignoredMessages);
		this.scrubs = List.copyOf(builder.scrubs);
		this.owners = List.copyOf(builder.owners);
		this.ownerSource = builder.ownerSource;
		this.attributes = Map.copyOf(builder.attributes);
		this.captureUncaught = builder.captureUncaught;
		this.limit = builder.limit;
	}

	/**
	 * Returns the policy used when a plugin declares none: the default scrubs, uncaught errors
	 * captured, at most {@value #DEFAULT_LIMIT} reports between two submissions.
	 *
	 * @return the default policy
	 */
	public static @NotNull ErrorPolicy defaults() {
		return builder().build();
	}

	/**
	 * Starts the description of a policy, with the {@link Scrub#defaults() default scrubs}.
	 *
	 * @return a new builder
	 */
	public static @NotNull Builder builder() {
		return new Builder();
	}

	/**
	 * Describes a policy.
	 */
	public static final class Builder {
		private final Set<Class<? extends Throwable>> ignoredTypes = new LinkedHashSet<>();
		private final List<Pattern> ignoredMessages = new ArrayList<>();
		private final List<Scrub> scrubs = new ArrayList<>(Scrub.defaults());
		private final List<ClassLoader> owners = new ArrayList<>();
		private Supplier<? extends Collection<ClassLoader>> ownerSource = List::of;
		private final Map<String, Object> attributes = new LinkedHashMap<>();
		private boolean captureUncaught = true;
		private int limit = DEFAULT_LIMIT;

		private Builder() {
		}

		/**
		 * Never reports errors of a type, such as those a server owner's setup causes.
		 *
		 * @param type the error type, matched exactly
		 * @return this builder
		 */
		public @NotNull Builder ignore(@NotNull Class<? extends Throwable> type) {
			ignoredTypes.add(Objects.requireNonNull(type, "type"));
			return this;
		}

		/**
		 * Never reports errors whose message contains a match.
		 *
		 * @param message what to look for in the message
		 * @return this builder
		 */
		public @NotNull Builder ignore(@NotNull Pattern message) {
			ignoredMessages.add(Objects.requireNonNull(message, "message"));
			return this;
		}

		/**
		 * Adds a rule that hides part of every message.
		 *
		 * @param scrub the rule
		 * @return this builder
		 */
		public @NotNull Builder scrub(@NotNull Scrub scrub) {
			scrubs.add(Objects.requireNonNull(scrub, "scrub"));
			return this;
		}

		/**
		 * Drops every rule added so far, including the default ones.
		 *
		 * @return this builder
		 */
		public @NotNull Builder clearScrubs() {
			scrubs.clear();
			return this;
		}

		/**
		 * Counts the classes of a class loader as the plugin's own code, such as a module the plugin
		 * ships and loads separately.
		 *
		 * @param loader the class loader
		 * @return this builder
		 */
		public @NotNull Builder owner(@NotNull ClassLoader loader) {
			owners.add(Objects.requireNonNull(loader, "loader"));
			return this;
		}

		/**
		 * Asks for further owning class loaders on every report, for modules the plugin loads and
		 * reloads at runtime. Replaces an earlier source.
		 *
		 * @param owners returns the current class loaders of such modules; called from any thread
		 * @return this builder
		 */
		public @NotNull Builder owners(@NotNull Supplier<? extends Collection<ClassLoader>> owners) {
			this.ownerSource = Objects.requireNonNull(owners, "owners");
			return this;
		}

		/**
		 * Sends a text attribute with every error.
		 *
		 * @param key   attribute name
		 * @param value attribute value
		 * @return this builder
		 */
		public @NotNull Builder attribute(@NotNull String key, @NotNull String value) {
			attributes.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
			return this;
		}

		/**
		 * Sends a number attribute with every error.
		 *
		 * @param key   attribute name
		 * @param value attribute value
		 * @return this builder
		 */
		public @NotNull Builder attribute(@NotNull String key, @NotNull Number value) {
			attributes.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
			return this;
		}

		/**
		 * Sends a boolean attribute with every error.
		 *
		 * @param key   attribute name
		 * @param value attribute value
		 * @return this builder
		 */
		public @NotNull Builder attribute(@NotNull String key, boolean value) {
			attributes.put(Objects.requireNonNull(key, "key"), value);
			return this;
		}

		/**
		 * Decides whether errors of the plugin's code that no handler caught are reported.
		 *
		 * @param captureUncaught {@code true} to report them, the default
		 * @return this builder
		 */
		public @NotNull Builder captureUncaught(boolean captureUncaught) {
			this.captureUncaught = captureUncaught;
			return this;
		}

		/**
		 * Limits how many reports are kept between two submissions, so a failure that repeats on
		 * every login cannot fill the memory.
		 *
		 * @param limit the most reports kept
		 * @return this builder
		 * @throws IllegalArgumentException if {@code limit} is not positive
		 */
		public @NotNull Builder limit(int limit) {
			if (limit <= 0) throw new IllegalArgumentException("The limit must be positive: " + limit);

			this.limit = limit;
			return this;
		}

		/**
		 * Finishes the policy.
		 *
		 * @return the policy
		 */
		public @NotNull ErrorPolicy build() {
			return new ErrorPolicy(this);
		}
	}
}
