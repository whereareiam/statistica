package me.whereareiam.statistica.integration.faststats;

import dev.faststats.Attributes;
import dev.faststats.ErrorTracker;
import me.whereareiam.statistica.common.error.ErrorFilter;
import me.whereareiam.statistica.common.error.ErrorRedaction;
import me.whereareiam.statistica.error.ErrorContext;
import me.whereareiam.statistica.error.ErrorPolicy;
import me.whereareiam.statistica.error.ErrorReporter;
import me.whereareiam.statistica.error.Scrub;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reports errors to FastStats.
 * <p>
 * The tracker holds reports in memory until FastStats submits them, so reports are only accepted
 * while error tracking is on: a server owner who turned it off, or the first start that only
 * creates the opt-out file, keeps nothing.
 */
final class FastStatsErrors implements ErrorReporter {
	private final @NotNull ErrorTracker tracker = ErrorTracker.contextUnaware();
	private final @NotNull ErrorPolicy policy;
	private final @NotNull ErrorFilter filter;
	private final @NotNull ClassLoader plugin;
	private volatile boolean active;

	FastStatsErrors(@NotNull ErrorPolicy policy, @NotNull ClassLoader plugin) {
		this.policy = policy;
		this.filter = new ErrorFilter(policy, plugin);
		this.plugin = plugin;

		policy.getIgnoredTypes().forEach(tracker::ignoreError);
		policy.getIgnoredMessages().forEach(pattern -> tracker.ignoreError((Pattern) pattern));
		for (Scrub scrub : policy.getScrubs()) tracker.anonymize(scrub.getPattern(), scrub.getReplacement());
		put(tracker.getAttributes(), policy.getAttributes());
	}

	@NotNull ErrorTracker tracker() {
		return tracker;
	}

	/**
	 * Accepts reports from now on, and errors of the plugin's code that no handler caught if the
	 * policy asks for them.
	 */
	void activate() {
		active = true;
		if (policy.isCaptureUncaught() && !tracker.isContextAttached()) tracker.attachErrorContext(plugin);
	}

	/**
	 * Stops accepting reports and stops capturing uncaught errors.
	 */
	void deactivate() {
		active = false;
		tracker.detachErrorContext();
	}

	@Override
	public void report(@NotNull Throwable error, @NotNull ErrorContext context) {
		if (!active || !filter.accepts(error)) return;

		Throwable redacted = ErrorRedaction.redact(error, context, policy.getScrubs());
		Attributes attributes = Attributes.empty();
		put(attributes, context.getAttributes());

		tracker.trackError(redacted).attributes(attributes);
	}

	private static void put(@NotNull Attributes target, @NotNull Map<String, Object> attributes) {
		attributes.forEach((key, value) -> {
			if (value instanceof Number number) target.put(key, number);
			else if (value instanceof Boolean bool) target.put(key, bool);
			else target.put(key, value.toString());
		});
	}
}
