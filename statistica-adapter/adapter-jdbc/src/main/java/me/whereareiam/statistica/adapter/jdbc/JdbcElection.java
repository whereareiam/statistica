package me.whereareiam.statistica.adapter.jdbc;

import me.whereareiam.statistica.Election;
import me.whereareiam.strata.MigrationStream;
import me.whereareiam.strata.MigrationTarget;
import me.whereareiam.strata.adapter.database.DatabaseContext;
import me.whereareiam.strata.adapter.database.DatabaseTarget;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.sql.DataSource;

/**
 * Elects the instance that sends shared metrics through a lease in a database every instance uses.
 * <p>
 * The lease is one row: who holds it and until when. An instance takes it when it is free or
 * expired and renews it while it holds it, in a single conditional update, so two instances never
 * hold it at once. When the holder stops, it gives the lease up; when it dies, another instance
 * takes over once the lease expires. A database only one instance uses always elects it.
 * <p>
 * The table belongs to the application's migrations. With Strata's database adapter on the
 * classpath, {@link #schema(String)} declares it as a stream of its own, to be run with the
 * application's other streams; other migration tools run {@link #createTableStatement()}:
 *
 * <pre>{@code
 * JdbcElection election = JdbcElection.of(dataSource, "myplugin_statistica_lease");
 *
 * new Strata(List.of(pluginDatabase, election.schema("my-plugin/statistica"))).migrate();
 * }</pre>
 * <p>
 * The statements are plain SQL that SQLite, H2, MySQL, MariaDB and PostgreSQL all run.
 */
public final class JdbcElection implements Election {
	/** How long a lease lasts without renewal, by default. Longer than a backend's interval of 30 minutes. */
	public static final Duration DEFAULT_LEASE = Duration.ofHours(1);

	private static final Pattern TABLE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,62}");

	private final @NotNull DataSource dataSource;
	private final @NotNull String table;
	private final long leaseMillis;
	private final @NotNull String holder = UUID.randomUUID().toString();

	private JdbcElection(@NotNull DataSource dataSource, @NotNull String table, @NotNull Duration lease) {
		Objects.requireNonNull(table, "table");
		if (!TABLE.matcher(table).matches()) throw new IllegalArgumentException("Invalid table name: " + table);
		if (lease.isNegative() || lease.isZero()) throw new IllegalArgumentException("The lease must be positive: " + lease);

		this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
		this.table = table;
		this.leaseMillis = lease.toMillis();
	}

	/**
	 * Returns an election with leases of {@link #DEFAULT_LEASE}.
	 *
	 * @param dataSource the database every instance uses
	 * @param table      name of the lease table, unique to the application, such as
	 *                   {@code myplugin_statistica_lease}
	 * @return the election
	 * @throws IllegalArgumentException if {@code table} is not a plain SQL identifier
	 */
	public static @NotNull JdbcElection of(@NotNull DataSource dataSource, @NotNull String table) {
		return new JdbcElection(dataSource, table, DEFAULT_LEASE);
	}

	/**
	 * Returns an election.
	 *
	 * @param dataSource the database every instance uses
	 * @param table      name of the lease table, unique to the application
	 * @param lease      how long a lease lasts without renewal; longer than the backends' interval
	 * @return the election
	 * @throws IllegalArgumentException if {@code table} is not a plain SQL identifier or
	 *                                  {@code lease} is not positive
	 */
	public static @NotNull JdbcElection of(
			@NotNull DataSource dataSource,
			@NotNull String table,
			@NotNull Duration lease
	) {
		return new JdbcElection(dataSource, table, lease);
	}

	/**
	 * Declares the lease table as a Strata stream on the election's database.
	 *
	 * @param streamId id of the stream, such as {@code my-plugin/statistica}
	 * @return the stream
	 */
	public @NotNull MigrationStream<DatabaseContext> schema(@NotNull String streamId) {
		return schema(streamId, new DatabaseTarget(dataSource));
	}

	/**
	 * Declares the lease table as a Strata stream on a target the application already uses.
	 *
	 * @param streamId id of the stream, such as {@code my-plugin/statistica}
	 * @param target   the database every instance uses
	 * @return the stream
	 */
	public @NotNull MigrationStream<DatabaseContext> schema(
			@NotNull String streamId,
			@NotNull MigrationTarget<DatabaseContext> target
	) {
		return MigrationStream.<DatabaseContext>builder()
				.id(streamId)
				.target(target)
				.creation(this::create)
				.build();
	}

	@Override
	public boolean elected() throws SQLException {
		long now = System.currentTimeMillis();

		try (Connection connection = dataSource.getConnection()) {
			if (claim(connection, now)) return true;
			if (exists(connection)) return false;

			return insert(connection, now);
		}
	}

	@Override
	public void release() throws SQLException {
		String sql = "UPDATE " + table + " SET expires_at = 0 WHERE id = 1 AND holder = ?";

		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, holder);
			statement.executeUpdate();
		}
	}

	/**
	 * Returns the statement that creates this election's lease table, for an application whose
	 * migrations are not Strata streams.
	 *
	 * @return the statement of {@link #createTableStatement(String)}
	 */
	public @NotNull String createTableStatement() {
		return createTableStatement(table);
	}

	/**
	 * Returns the statement that creates a lease table unless it exists, which every supported
	 * database runs. The lease row is added by the first election.
	 *
	 * @param table name of the lease table
	 * @return a {@code CREATE TABLE IF NOT EXISTS} statement
	 * @throws IllegalArgumentException if {@code table} is not a plain SQL identifier
	 */
	public static @NotNull String createTableStatement(@NotNull String table) {
		if (!TABLE.matcher(table).matches()) throw new IllegalArgumentException("Invalid table name: " + table);

		return "CREATE TABLE IF NOT EXISTS " + table + " ("
				+ "id INT NOT NULL PRIMARY KEY, "
				+ "holder VARCHAR(36) NOT NULL, "
				+ "expires_at BIGINT NOT NULL)";
	}

	private boolean claim(@NotNull Connection connection, long now) throws SQLException {
		String sql = "UPDATE " + table + " SET holder = ?, expires_at = ? WHERE id = 1 AND (holder = ? OR expires_at < ?)";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, holder);
			statement.setLong(2, now + leaseMillis);
			statement.setString(3, holder);
			statement.setLong(4, now);

			return statement.executeUpdate() == 1;
		}
	}

	private boolean exists(@NotNull Connection connection) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM " + table + " WHERE id = 1");
			 ResultSet result = statement.executeQuery()) {
			return result.next();
		}
	}

	/**
	 * Adds the lease row held by this instance. When another instance added it first, the insert
	 * fails on the primary key, which every database reports differently, so the row is looked for
	 * again instead.
	 */
	private boolean insert(@NotNull Connection connection, long now) throws SQLException {
		String sql = "INSERT INTO " + table + " (id, holder, expires_at) VALUES (1, ?, ?)";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, holder);
			statement.setLong(2, now + leaseMillis);

			return statement.executeUpdate() == 1;
		} catch (SQLException failure) {
			if (exists(connection)) return false;

			throw failure;
		}
	}

	private void create(@NotNull DatabaseContext database) throws SQLException {
		database.execute(createTableStatement());
	}
}
