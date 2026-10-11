package me.whereareiam.statistica.adapter.jdbc;

import me.whereareiam.strata.Strata;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcElectionTest {
	private static final String TABLE = "test_statistica_lease";

	@ParameterizedTest
	@MethodSource("databases")
	void electsOneInstanceAtATime(DataSource database) throws Exception {
		JdbcElection first = migrated(database, JdbcElection.of(database, TABLE));
		JdbcElection second = migrated(database, JdbcElection.of(database, TABLE));

		assertTrue(first.elected());
		assertFalse(second.elected());
		assertTrue(first.elected());
	}

	@ParameterizedTest
	@MethodSource("databases")
	void handsTheLeaseOverOnRelease(DataSource database) throws Exception {
		JdbcElection first = migrated(database, JdbcElection.of(database, TABLE));
		JdbcElection second = JdbcElection.of(database, TABLE);

		first.elected();
		first.release();

		assertTrue(second.elected());
		assertFalse(first.elected());
	}

	@ParameterizedTest
	@MethodSource("databases")
	void takesAnExpiredLeaseOver(DataSource database) throws Exception {
		JdbcElection first = migrated(database, JdbcElection.of(database, TABLE, Duration.ofMillis(1)));
		JdbcElection second = JdbcElection.of(database, TABLE);

		first.elected();
		Thread.sleep(5);

		assertTrue(second.elected());
	}

	private static JdbcElection migrated(DataSource database, JdbcElection election) {
		new Strata(List.of(election.schema("test/statistica"))).migrate();
		return election;
	}

	static Stream<DataSource> databases() throws IOException {
		Path file = Files.createTempFile("statistica", ".db");
		file.toFile().deleteOnExit();

		JdbcDataSource h2 = new JdbcDataSource();
		h2.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");

		SQLiteDataSource sqlite = new SQLiteDataSource();
		sqlite.setUrl("jdbc:sqlite:" + file);

		return Stream.of(h2, sqlite);
	}
}
