package guru.mikelue.farming.rest;

import java.time.Duration;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import guru.mikelue.farming.base.AbstractITBase;
import guru.mikelue.farming.junit.cassandra.CassandraData;
import guru.mikelue.farming.junit.cassandra.CassandraData.Phase;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@DirtiesContext(classMode=ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(
	/**
	 * Prevents the cron job of harvesting clean some blocks.
	 */
	properties= {
		"schedule.harvesting.initial-delay=PT10M",
		"schedule.harvesting.fixed-delay=PT10M",
	}
)
public class BlockControllerIT extends AbstractITBase {
	public BlockControllerIT() {}

	/**
	 * Tests the listing of block by land id.
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"""
				INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate)
				VALUES ('ac01e374-1465-11ed-864e-00155debdaa8', 'evergreen huckleberry', 10, 'Tropical');
				""",
				"""
				INSERT INTO vc_block(bl_ld_id, bl_sow_time, bl_id)
				VALUES
					('ac01e374-1465-11ed-864e-00155debdaa8', NOW(), 0),
					('ac01e374-1465-11ed-864e-00155debdaa8', NOW(), 1),
					('ac01e374-1465-11ed-864e-00155debdaa8', NOW(), 2),
					('ac01e374-1465-11ed-864e-00155debdaa8', NOW(), 3),
					('ac01e374-1465-11ed-864e-00155debdaa8', NOW(), 4);
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
		@Sql(
			statements={
				"""
				DELETE FROM vc_block
				WHERE bl_ld_id = 'ac01e374-1465-11ed-864e-00155debdaa8';
				DELETE FROM vc_land
					WHERE ld_id = 'ac01e374-1465-11ed-864e-00155debdaa8';
				"""
			},
			executionPhase=AFTER_TEST_METHOD
		)
	})
	void listBlocksByLand()
	{
		getWebTestClient()
			.get()
			.uri("/land/ac01e374-1465-11ed-864e-00155debdaa8/blocks")
			.exchange()
			.expectBody(JsonNode.class)
			.value(LogJsonContent)
			.value(jsonResp -> assertJsonContent(jsonResp)
				.extractingJsonPathNumberValue("@.length()")
				.isEqualTo(5)
			);
	}

	private final static Duration DEFAULT_TIMEOUT = Duration.ofMinutes(1);
	private final static long SLEEP_TIME = 3000;

	/**
	 * Tests the asking for sowing.
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"""
				INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate)
				VALUES ('cc0e5b4e-16ee-11ed-83e1-00155debdaa8', 'water chestnut', 10, 'Mild');
				""",
				"""
				INSERT INTO vc_block(bl_ld_id, bl_id)
				VALUES
					('cc0e5b4e-16ee-11ed-83e1-00155debdaa8', 0),
					('cc0e5b4e-16ee-11ed-83e1-00155debdaa8', 1),
					('cc0e5b4e-16ee-11ed-83e1-00155debdaa8', 2),
					('cc0e5b4e-16ee-11ed-83e1-00155debdaa8', 3),
					('cc0e5b4e-16ee-11ed-83e1-00155debdaa8', 4);
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
		@Sql(
			statements={
				"""
				DELETE FROM vc_block
				WHERE bl_ld_id = 'cc0e5b4e-16ee-11ed-83e1-00155debdaa8';
				DELETE FROM vc_land
					WHERE ld_id = 'cc0e5b4e-16ee-11ed-83e1-00155debdaa8';
				"""
			},
			executionPhase=AFTER_TEST_METHOD
		)
	})
	@CassandraData(
		cqls="""
			DELETE FROM vc_land_log_by_time
			WHERE ll_ld_id = cc0e5b4e-16ee-11ed-83e1-00155debdaa8
		""",
		phase=Phase.AFTER
	)
	void askSowing()
	{
		var requestBody = createObjectNode()
			.put("asked_blocks", 3)
			.put("crop", 9)
			.put("comment", "dog");

		getWebTestClient()
			.post()
			.uri("/land/cc0e5b4e-16ee-11ed-83e1-00155debdaa8/sow")
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isOk()
			.expectBody(JsonNode.class)
			.value(LogJsonContent)
			.value(jsonNode -> assertJsonContent(jsonNode)
				.extractingJsonPathNumberValue("$.length()")
				.isEqualTo(3)
			);

		/**
		 * Asserts the blocks which are sowed as "Occupied"
		 */
		checkBlockTableUntilTimeout(
			"""
			SELECT COUNT(*)
			FROM vc_block
			WHERE bl_ld_id = 'cc0e5b4e-16ee-11ed-83e1-00155debdaa8'
				AND bl_status = 'Occupied'
			""",
			3
		);
		// :~)
	}

	/**
	 * Tests the asking for cleaning.
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"""
				INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate)
				VALUES ('cc0e5b4e-16ee-11ed-83e1-00155debdaa8', 'water chestnut', 10, 'Mild');
				""",
				"""
				INSERT INTO vc_block(bl_ld_id, bl_id, bl_sow_time, bl_mature_time, bl_harvest_amount, bl_status)
				VALUES
					('cc0e5b4e-16ee-11ed-83e1-00155debdaa8', 0, NOW() - INTERVAL '20 M', NOW() + INTERVAL '20 M', 3, 'Occupied'),
					('cc0e5b4e-16ee-11ed-83e1-00155debdaa8', 1, NOW() - INTERVAL '20 M', NOW() + INTERVAL '20 M', 5, 'Occupied'),
					('cc0e5b4e-16ee-11ed-83e1-00155debdaa8', 2, NOW() - INTERVAL '20 M', NOW() + INTERVAL '20 M', 2, 'Occupied'),
					('cc0e5b4e-16ee-11ed-83e1-00155debdaa8', 3, NOW() - INTERVAL '20 M', NOW() + INTERVAL '20 M', 1, 'Occupied'),
					('cc0e5b4e-16ee-11ed-83e1-00155debdaa8', 4, NOW() - INTERVAL '20 M', NOW() + INTERVAL '20 M', 8, 'Occupied'),
					('cc0e5b4e-16ee-11ed-83e1-00155debdaa8', 5, null, null, null, 'Available');
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
		@Sql(
			statements={
				"""
				DELETE FROM vc_block
				WHERE bl_ld_id = 'cc0e5b4e-16ee-11ed-83e1-00155debdaa8';
				DELETE FROM vc_land
					WHERE ld_id = 'cc0e5b4e-16ee-11ed-83e1-00155debdaa8';
				"""
			},
			executionPhase=AFTER_TEST_METHOD
		)
	})
	@CassandraData(
		cqls="""
			DELETE FROM vc_land_log_by_time
			WHERE ll_ld_id = cc0e5b4e-16ee-11ed-83e1-00155debdaa8
		""",
		phase=Phase.AFTER
	)
	void askClean()
	{
		var requestBody = createObjectNode()
			.put("asked_blocks", 3)
			.put("comment", "panda");

		getWebTestClient()
			.post()
			.uri("/land/cc0e5b4e-16ee-11ed-83e1-00155debdaa8/clean")
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isOk()
			.expectBody(JsonNode.class)
			.value(LogJsonContent)
			.value(jsonNode -> assertJsonContent(jsonNode)
				.hasJsonPathValue("[?(@.number_of_available_blocks == 1)]")
				.hasJsonPathValue("[?(@.scheduled_blocks_for_cleaning == 2)]")
			);

		/**
		 * Asserts the blocks which are freed as "Available"
		 */
		checkBlockTableUntilTimeout(
			"""
			SELECT COUNT(*)
			FROM vc_block
			WHERE bl_ld_id = 'cc0e5b4e-16ee-11ed-83e1-00155debdaa8'
				AND bl_status = 'Available'
			""",
			3
		);
		// :~)
	}

	private void checkBlockTableUntilTimeout(
		String getCountSql, int atLeastCount
	) {
		/**
		 * Keeps checking the number matched rows in database
		 */
		assertTimeoutPreemptively(
			DEFAULT_TIMEOUT,
			() -> {
				do {
					Thread.sleep(SLEEP_TIME);

					var counter = (Number)getEntityManager()
						.createNativeQuery(getCountSql)
						.getSingleResult();

					if (counter.intValue() >= atLeastCount) {
						break;
					}
				} while (true);
			}
		);
		// :~)
	}
}
