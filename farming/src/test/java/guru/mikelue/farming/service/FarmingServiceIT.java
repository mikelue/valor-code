package guru.mikelue.farming.service;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

import java.time.Duration;
import java.util.function.Predicate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import org.junit.jupiter.api.Test;

import guru.mikelue.farming.base.AbstractITBase;
import guru.mikelue.farming.junit.cassandra.CassandraData;
import guru.mikelue.farming.junit.cassandra.CassandraData.Phase;

/**
 * Only tests the methods triggered by Cron job.
 */
@DirtiesContext(classMode=ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(
	properties={
		"schedule.harvesting.initial-delay=PT5S",
		"schedule.harvesting.fixed-delay=PT10M",
		"schedule.too-long-scheduled-activities.initial-delay=PT5S",
		"schedule.too-long-scheduled-activities.fixed-delay=PT10M",
	}
)
public class FarmingServiceIT extends AbstractITBase {
	private final static long SLEEP_TIME = 3000l;

	public FarmingServiceIT() {}

	/**
	 * Tests the cron job for harvesting matured blocks.
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"""
				INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate)
				VALUES ('53d6ab6e-1c6f-11ed-8ed0-00155da861c9', 'evergreen huckleberry', 10, 'Tropical');
				""",
				"""
				INSERT INTO vc_block(bl_ld_id, bl_id, bl_status, bl_crop, bl_mature_time, bl_sow_time)
				VALUES
					('53d6ab6e-1c6f-11ed-8ed0-00155da861c9', 0, 'Occupied', 'Kale', NOW() - Interval '20 minute', NOW() - Interval '30 minute'),
					('53d6ab6e-1c6f-11ed-8ed0-00155da861c9', 1, 'Occupied', 'Kale', NOW() - Interval '20 minute', NOW() - Interval '30 minute'),
					('53d6ab6e-1c6f-11ed-8ed0-00155da861c9', 2, 'Occupied', 'Kale', NOW() - Interval '20 minute', NOW() - Interval '30 minute');
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
		@Sql(
			statements={
				"""
				DELETE FROM vc_block
				WHERE bl_ld_id = '53d6ab6e-1c6f-11ed-8ed0-00155da861c9';
				DELETE FROM vc_land
					WHERE ld_id = '53d6ab6e-1c6f-11ed-8ed0-00155da861c9';
				"""
			},
			executionPhase=AFTER_TEST_METHOD
		)
	})
	@CassandraData(
		cqls="""
			DELETE FROM vc_land_log_by_time
			WHERE ll_ld_id = 53d6ab6e-1c6f-11ed-8ed0-00155da861c9
		""",
		phase=Phase.AFTER
	)
	void processMaturedBlocks() throws Throwable
	{
		var getCounterSql = """
			SELECT COUNT(*)
			FROM vc_block
			WHERE bl_ld_id = '53d6ab6e-1c6f-11ed-8ed0-00155da861c9'
				AND bl_status = 'Available'
		""";

		assertAndWaiting(
			Duration.ofMinutes(1),
			getCounterSql, v -> v >= 3
		);
	}

	/**
	 * Tests the cron job for scheduled blocks of too-long.
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"""
				INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate)
				VALUES ('05626d56-1ddc-11ed-91f1-00155da861c9', 'evergreen huckleberry', 10, 'Tropical');
				""",
				"""
				INSERT INTO vc_block(bl_ld_id, bl_id, bl_status, bl_crop, bl_mature_time, bl_sow_time, bl_harvest_amount, bl_update_time)
				VALUES
					('05626d56-1ddc-11ed-91f1-00155da861c9', 0, 'ScheduledSow', 'Kale', null, null, null,
						NOW() - Interval '50 minute'
					),
					('05626d56-1ddc-11ed-91f1-00155da861c9', 1, 'ScheduledClean', 'Kale', NOW() - Interval '20 minute', NOW() - Interval '30 minute', 20,
						NOW() - Interval '50 minute'
					),
					('05626d56-1ddc-11ed-91f1-00155da861c9', 2, 'ScheduledHarvest', 'Kale', NOW() - Interval '20 minute', NOW() - Interval '30 minute', 30,
						NOW() - Interval '50 minute'
					);
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
		@Sql(
			statements={
				"""
				DELETE FROM vc_block
				WHERE bl_ld_id = '05626d56-1ddc-11ed-91f1-00155da861c9';
				DELETE FROM vc_land
					WHERE ld_id = '05626d56-1ddc-11ed-91f1-00155da861c9';
				"""
			},
			executionPhase=AFTER_TEST_METHOD
		)
	})
	@CassandraData(
		cqls="""
			DELETE FROM vc_land_log_by_time
			WHERE ll_ld_id = 05626d56-1ddc-11ed-91f1-00155da861c9
		""",
		phase=Phase.AFTER
	)
	void processTooLongScheduledBlocks() throws Throwable
	{
		var getCounterSql = """
			SELECT COUNT(*)
			FROM vc_block
			WHERE bl_ld_id = '05626d56-1ddc-11ed-91f1-00155da861c9'
				AND bl_status IN ('ScheduledSow', 'ScheduledClean', 'ScheduledHarvest')
		""";

		assertAndWaiting(
			Duration.ofMinutes(1),
			getCounterSql, v -> v == 0
		);
	}

	private void assertAndWaiting(
		Duration timeout, String sql, Predicate<Integer> countChecker
	) {
		assertTimeoutPreemptively(
			timeout,
			() -> {
				do {
					Thread.sleep(SLEEP_TIME);

					var counter = (Number)getEntityManager()
						.createNativeQuery(sql)
						.getSingleResult();

					if (countChecker.test(counter.intValue())) {
						break;
					}
				} while (true);
			}
		);
	}
}
