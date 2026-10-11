package me.whereareiam.statistica.common.error;

import me.whereareiam.statistica.error.ErrorPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Decides which errors a plugin reports: its own, not ignored, and within the limit.
 * <p>
 * An error is the plugin's own when the code that threw it belongs to an owning class loader. Its
 * stack is read from the top, passing over frames of the JDK and the platform, which every plugin
 * calls into; the first frame left decides. A class no owner can see, such as another plugin's or a
 * third-party extension's, makes the error someone else's. The deepest cause is looked at first,
 * because that is where the failure started.
 */
public final class ErrorFilter {
	/** How long the limit of reports applies before it starts over. */
	public static final Duration WINDOW = Duration.ofMinutes(30);

	private static final int MAX_CACHED_CLASSES = 2048;

	private final @NotNull ErrorPolicy policy;
	private final @NotNull Set<ClassLoader> owners;
	private final @NotNull Map<String, Origin> origins = new ConcurrentHashMap<>();
	private long windowStart = System.nanoTime();
	private int reports;

	/**
	 * Creates the filter of a plugin.
	 *
	 * @param policy which errors to report
	 * @param plugin class loader of the plugin, which always owns
	 */
	public ErrorFilter(@NotNull ErrorPolicy policy, @NotNull ClassLoader plugin) {
		this.policy = policy;

		Set<ClassLoader> owners = Collections.newSetFromMap(new IdentityHashMap<>());
		owners.add(plugin);
		owners.addAll(policy.getOwners());
		this.owners = owners;
	}

	/**
	 * Tells whether an error is reported, counting it against the limit when it is.
	 *
	 * @param error the error
	 * @return whether the error is reported
	 */
	public boolean accepts(@NotNull Throwable error) {
		return !ignored(error) && owned(error) && withinLimit();
	}

	/**
	 * Tells whether an error, or one of its causes, is of an ignored type or has an ignored message.
	 *
	 * @param error the error
	 * @return whether the error is ignored
	 */
	public boolean ignored(@NotNull Throwable error) {
		for (Throwable current : chain(error)) {
			if (policy.getIgnoredTypes().contains(current.getClass())) return true;

			String message = current.getMessage();
			if (message == null) continue;

			for (Pattern pattern : policy.getIgnoredMessages())
				if (pattern.matcher(message).find()) return true;
		}

		return false;
	}

	/**
	 * Tells whether an error started in the plugin's own code.
	 *
	 * @param error the error
	 * @return whether the error is the plugin's own
	 */
	public boolean owned(@NotNull Throwable error) {
		List<Throwable> chain = chain(error);
		Collections.reverse(chain);

		for (Throwable current : chain) {
			for (StackTraceElement frame : current.getStackTrace()) {
				Origin origin = origin(frame.getClassName());
				if (origin != Origin.PLATFORM) return origin == Origin.OWNED;
			}
		}

		return false;
	}

	private synchronized boolean withinLimit() {
		long now = System.nanoTime();
		if (now - windowStart >= WINDOW.toNanos()) {
			windowStart = now;
			reports = 0;
		}
		if (reports >= policy.getLimit()) return false;

		reports++;
		return true;
	}

	private @NotNull Origin origin(@NotNull String className) {
		Origin cached = origins.get(className);
		if (cached != null) return cached;

		Origin origin = resolve(className);
		if (origins.size() < MAX_CACHED_CLASSES) origins.put(className, origin);

		return origin;
	}

	private @NotNull Origin resolve(@NotNull String className) {
		boolean visible = false;
		for (ClassLoader owner : owners) {
			Class<?> type = load(className, owner);
			if (type == null) continue;
			if (owners.contains(type.getClassLoader())) return Origin.OWNED;

			visible = true;
		}

		return visible ? Origin.PLATFORM : Origin.FOREIGN;
	}

	private static @Nullable Class<?> load(@NotNull String className, @NotNull ClassLoader loader) {
		try {
			return Class.forName(className, false, loader);
		} catch (ClassNotFoundException | LinkageError ignored) {
			return null;
		}
	}

	private static @NotNull List<Throwable> chain(@NotNull Throwable error) {
		Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<Throwable> chain = new LinkedHashSet<>();
		for (Throwable current = error; current != null && visited.add(current); current = current.getCause())
			chain.add(current);

		return new ArrayList<>(chain);
	}

	private enum Origin {
		/** A class of an owning class loader. */
		OWNED,

		/** A class owners see but did not define, such as the JDK's or the platform's. */
		PLATFORM,

		/** A class no owner can see, such as another plugin's or a third-party extension's. */
		FOREIGN
	}
}
