package me.whereareiam.statistica.common;

import me.whereareiam.statistica.Election;
import me.whereareiam.statistica.Statistica;
import me.whereareiam.statistica.common.metric.CachedElection;
import me.whereareiam.statistica.common.metric.SharedMetrics;
import me.whereareiam.statistica.error.ErrorPolicy;
import me.whereareiam.statistica.metric.MetricSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Configures the {@link Statistica} of a plugin; each platform module extends it with what that
 * platform needs to create the backends.
 * <p>
 * A backend is turned on by giving its id or token, and only then is its library used, so a plugin
 * that ships only one backend's library never touches the other's classes. The platform's
 * {@code createBStats} and {@code createFastStats} must therefore reach that library through a
 * separate class, never in their own body.
 *
 * @param <B> the platform's builder
 */
public abstract class StatisticaBuilder<B extends StatisticaBuilder<B>> {
	private static final Pattern FASTSTATS_TOKEN = Pattern.compile("[a-z0-9]{32}");

	private @Nullable Integer bstatsId;
	private @Nullable String faststatsToken;
	private @NotNull Election election = Election.standalone();
	private @NotNull MetricSet metrics = MetricSet.empty();
	private @NotNull ErrorPolicy errors = ErrorPolicy.defaults();

	/**
	 * Sends to bStats. The platform's builder exposes it with what its bStats library needs.
	 *
	 * @param serviceId id of the plugin on bStats
	 * @return this builder
	 * @throws IllegalArgumentException if {@code serviceId} is not positive
	 */
	protected @NotNull B bstatsService(int serviceId) {
		if (serviceId <= 0) throw new IllegalArgumentException("A bStats id is positive: " + serviceId);

		this.bstatsId = serviceId;
		return self();
	}

	/**
	 * Sends to FastStats, metrics and errors. Requires the backend's library, the {@code faststats}
	 * capability of the platform module.
	 *
	 * @param token project token from the FastStats dashboard; it ships with the plugin and is not
	 *              a secret
	 * @return this builder
	 * @throws IllegalArgumentException if {@code token} is not a FastStats project token
	 */
	public @NotNull B faststats(@NotNull String token) {
		Objects.requireNonNull(token, "token");
		if (!FASTSTATS_TOKEN.matcher(token).matches())
			throw new IllegalArgumentException("A FastStats token is 32 characters of a-z and 0-9");

		this.faststatsToken = token;
		return self();
	}

	/**
	 * Decides which instance sends the shared metrics. Without it, every instance is elected.
	 *
	 * @param election the election
	 * @return this builder
	 */
	public @NotNull B election(@NotNull Election election) {
		this.election = Objects.requireNonNull(election, "election");
		return self();
	}

	/**
	 * Declares the metrics to send.
	 *
	 * @param metrics the metrics
	 * @return this builder
	 */
	public @NotNull B metrics(@NotNull MetricSet metrics) {
		this.metrics = Objects.requireNonNull(metrics, "metrics");
		return self();
	}

	/**
	 * Declares the metrics to send.
	 *
	 * @param metrics declares the metrics on a new builder
	 * @return this builder
	 */
	public @NotNull B metrics(@NotNull Consumer<MetricSet.Builder> metrics) {
		MetricSet.Builder builder = MetricSet.builder();
		metrics.accept(builder);

		return metrics(builder.build());
	}

	/**
	 * Decides which errors are reported and what is hidden from them.
	 *
	 * @param errors the policy
	 * @return this builder
	 */
	public @NotNull B errors(@NotNull ErrorPolicy errors) {
		this.errors = Objects.requireNonNull(errors, "errors");
		return self();
	}

	/**
	 * Decides which errors are reported and what is hidden from them.
	 *
	 * @param errors describes the policy on a new builder that starts from the defaults
	 * @return this builder
	 */
	public @NotNull B errors(@NotNull Consumer<ErrorPolicy.Builder> errors) {
		ErrorPolicy.Builder builder = ErrorPolicy.builder();
		errors.accept(builder);

		return errors(builder.build());
	}

	/**
	 * Creates the {@link Statistica}. Nothing is sent before {@link Statistica#start()}.
	 *
	 * @return the configured statistics
	 * @throws IllegalStateException if neither bStats nor FastStats was configured
	 */
	public @NotNull Statistica build() {
		if (bstatsId == null && faststatsToken == null)
			throw new IllegalStateException("Configure bStats, FastStats or both");

		CachedElection cachedElection = new CachedElection(election);
		MetricSet elected = SharedMetrics.elect(metrics, cachedElection);

		List<Backend> backends = new ArrayList<>(2);
		if (bstatsId != null) backends.add(createBStats(bstatsId, elected));
		if (faststatsToken != null) backends.add(createFastStats(faststatsToken, elected, errors));

		return new DefaultStatistica(backends, cachedElection);
	}

	/**
	 * Creates the bStats backend of this platform.
	 *
	 * @param serviceId id of the plugin on bStats
	 * @param metrics   metrics to send
	 * @return the backend
	 */
	protected abstract @NotNull Backend createBStats(int serviceId, @NotNull MetricSet metrics);

	/**
	 * Creates the FastStats backend of this platform.
	 *
	 * @param token   project token
	 * @param metrics metrics to send
	 * @param errors  which errors to report
	 * @return the backend
	 */
	protected abstract @NotNull Backend createFastStats(
			@NotNull String token,
			@NotNull MetricSet metrics,
			@NotNull ErrorPolicy errors
	);

	@SuppressWarnings("unchecked")
	private @NotNull B self() {
		return (B) this;
	}
}
