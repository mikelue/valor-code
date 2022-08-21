package guru.mikelue.farming.rest;

import static guru.mikelue.farming.junit.cassandra.CassandraData.Phase.AFTER;
import static guru.mikelue.misc.springframework.data.web.ReactivePageableParamResolver.DEFAULT_PARAM_NAME_PAGE;
import static guru.mikelue.misc.springframework.data.web.ReactivePageableParamResolver.DEFAULT_PARAM_NAME_PAGE_SIZE;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.util.StopWatch;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import guru.mikelue.farming.base.AbstractITBase;
import guru.mikelue.farming.junit.cassandra.CassandraData;
import guru.mikelue.farming.junit.cassandra.CassandraDataExecutor;
import guru.mikelue.farming.model.RandomModels;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class LandControllerIT extends AbstractITBase {
	public LandControllerIT() {}

	/**
	 * Tests adding of land.
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"""
				DELETE FROM vc_block WHERE bl_ld_id = (
					SELECT ld_id FROM vc_land
					WHERE ld_name LIKE 'corms-%'
				);
				""",
				"""
				DELETE FROM vc_land WHERE ld_name LIKE 'corms-%';
				"""
			},
			executionPhase=AFTER_TEST_METHOD
		)
	})
	void addNew()
	{
		var jsonBody = createObjectNode()
			.put("name", "corms-88")
			.put("size", 10000)
			.put("climate", 2);

		var watch = new StopWatch();
		watch.start();
		getWebTestClient()
			.mutateWith(webClientTimeout(Duration.ofSeconds(30)))
			.post()
			.uri("/land")
			.bodyValue(jsonBody)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(JsonNode.class)
			.value(LogJsonContent)
			.value(jsonContent -> assertJsonContent(jsonContent)
				.hasJsonPathStringValue("id")
			);
		watch.stop();
		getLogger().info("Passed time: [{}] milliseconds", watch.getTotalTimeMillis());
	}

	/**
	 * Tests the getting of land by its id.
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"""
				INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate)
				VALUES ('b52fc38a-1181-11ed-9368-00155d8fd4c9', 'white mulberry', 10, 'Mild');
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
		@Sql(
			statements={
				"""
				DELETE FROM vc_land WHERE ld_id = 'b52fc38a-1181-11ed-9368-00155d8fd4c9';
				"""
			},
			executionPhase=AFTER_TEST_METHOD
		)
	})
	void getById()
	{
		getWebTestClient()
			.get()
			.uri("/land/b52fc38a-1181-11ed-9368-00155d8fd4c9")
			.exchange()
			.expectStatus().isOk()
			.expectBody(JsonNode.class)
			.value(LogJsonContent)
			.value(respBody -> assertJsonContent(respBody)
				.hasJsonPathValue("[?(@.id == '%s')]", "b52fc38a-1181-11ed-9368-00155d8fd4c9")
			);
	}

	/**
	 * Tests the listing of lands.
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"""
				INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate)
				VALUES
					('b52fc38a-1181-11ed-9368-01155d8fd4c9', 'basil-1', 10, 'Mild'),
					('b52fc38a-1181-11ed-9368-02155d8fd4c9', 'basil-2', 10, 'Mild'),
					('b52fc38a-1181-11ed-9368-03155d8fd4c9', 'basil-3', 10, 'Mild'),
					('b52fc38a-1181-11ed-9368-04155d8fd4c9', 'basil-4', 10, 'Mild'),
					('b52fc38a-1181-11ed-9368-05155d8fd4c9', 'basil-5', 10, 'Mild'),
					('b52fc38a-1181-11ed-9368-06155d8fd4c9', 'basil-6', 10, 'Mild'),
					('b52fc38a-1181-11ed-9368-07155d8fd4c9', 'basil-7', 10, 'Mild'),
					('b52fc38a-1181-11ed-9368-08155d8fd4c9', 'basil-8', 10, 'Mild');
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
		@Sql(
			statements={
				"""
				DELETE FROM vc_land WHERE ld_name LIKE 'basil-%';
				"""
			},
			executionPhase=AFTER_TEST_METHOD
		)
	})
	void list()
	{
		getWebTestClient()
			.get()
			.uri("/lands")
			.header(DEFAULT_PARAM_NAME_PAGE, "1")
			.header(DEFAULT_PARAM_NAME_PAGE_SIZE, "3")
			.exchange()
			.expectStatus().isOk()
			.expectBody(JsonNode.class)
			.value(jsonContent -> assertJsonContent(jsonContent)
				.extractingJsonPathArrayValue("$..name")
				.containsExactly("basil-4", "basil-5", "basil-6")
			);
	}

	/**
	 * Tests the getting of land by its id.
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"""
				INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate)
				VALUES ('bd60fcba-13e1-11ed-a8bb-00155debdaa8', 'evergreen huckleberry', 10, 'Mild');
				;
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
		@Sql(
			statements={
				"""
				DELETE FROM vc_land WHERE ld_id = 'bd60fcba-13e1-11ed-a8bb-00155debdaa8';
				"""
			},
			executionPhase=AFTER_TEST_METHOD
		)
	})
	void modify()
	{
		var requestBody = createObjectNode()
			.put("name", "ximenia caffra");

		getWebTestClient()
			.put()
			.uri("/land/bd60fcba-13e1-11ed-a8bb-00155debdaa8")
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isOk()
			.expectBody(JsonNode.class)
			.value(LogJsonContent)
			.value(respBody -> assertJsonContent(respBody)
				.hasJsonPathValue("[?(@.name == '%s')]",
					requestBody.get("name").asText())
			);
	}

	/**
	 * Tests the getting of land by its id.
	 */
	@Test
	@CassandraData(
		executors={ RandomDataOfLandLog.class }
	)
	@CassandraData(
		cqls={
			"""
			DELETE FROM vc_land_log_by_time
			WHERE ll_ld_id = 1342e058-1f72-11ed-a542-00155da861c9
			"""
		},
		phase=AFTER
	)
	void findByTimeRangeOfLand()
	{
		getWebTestClient()
			.get()
			.uri("/land/1342e058-1f72-11ed-a542-00155da861c9/logs?start_time=2021-03-05T14:30:00Z&end_time=2021-03-05T14:30:59Z")
			.exchange()
			.expectStatus().isOk()
			.expectBody(JsonNode.class)
			.value(LogJsonContent)
			.value(respBody -> assertJsonContent(respBody)
				.extractingJsonPathArrayValue("$")
				.hasSize(6)
			);
	}

	static class RandomDataOfLandLog implements CassandraDataExecutor {
		final static UUID LAND_ID = UUID.fromString("1342e058-1f72-11ed-a542-00155da861c9");

		private final static int NUMBER_OF_ROWS = 100;

		/**
		 * Ranges of update time
		 */
		final static Instant baseTime = Instant.parse("2021-03-05T14:30:00Z");

		@Override
		public Mono<Void> apply(TestContext ctx, ReactiveCassandraTemplate cassandraTemplate)
		{
			return Flux.fromIterable(
				RandomModels.randomLandLogs(
					NUMBER_OF_ROWS, baseTime, Duration.ofSeconds(10)
				)
			)
				.map(log -> {
					log.setLandId(LAND_ID);
					return log;
				})
				.flatMap(cassandraTemplate::insert)
				.then();
		}
	}
}
