package guru.mikelue.farming.repos.cassandra;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestContext;

import guru.mikelue.farming.base.AbstractCassandraTestBase;
import guru.mikelue.farming.junit.cassandra.CassandraData;
import guru.mikelue.farming.junit.cassandra.CassandraDataExecutor;
import guru.mikelue.farming.model.Block;
import guru.mikelue.farming.model.Crop;
import guru.mikelue.farming.model.LandLog;
import guru.mikelue.farming.model.LogActivity;
import guru.mikelue.farming.model.RandomModels;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.*;
import static guru.mikelue.farming.junit.cassandra.CassandraData.Phase.*;

public class LandLogReposTest extends AbstractCassandraTestBase {
	@Autowired
	private LandLogRepos testedRepos;

	public LandLogReposTest() {}

	/**
	 * Tests the adding of land log.
	 */
	@Test
	@CassandraData(
		cqls={
			"""
			DELETE FROM vc_land_log_by_time
			WHERE ll_ld_id = d62e176d-a04c-4710-9ef5-cdf4db1781d5
			"""
		},
		phase=AFTER
	)
	void add()
	{
		var landId = UUID.fromString("d62e176d-a04c-4710-9ef5-cdf4db1781d5");

		var sampleBlock = new Block();
		sampleBlock.setLandId(landId);
		sampleBlock.setId((short)30);
		sampleBlock.setCrop(Crop.Lettuce);
		sampleBlock.setSowTime(Instant.now().minus(10, SECONDS).truncatedTo(SECONDS));
		sampleBlock.setMatureTime(Instant.now().plus(10, SECONDS).truncatedTo(SECONDS));
		sampleBlock.setStatus(Block.Status.Occupied);
		sampleBlock.setHarvestAmount((short)30);
		sampleBlock.setUpdateTime(Instant.now().truncatedTo(SECONDS));
		sampleBlock.setComment("news legs copy");

		var newLog = LandLog.from(sampleBlock);
		newLog.setTime(sampleBlock.getUpdateTime().truncatedTo(SECONDS));
		newLog.setUsedTimeSecond((short)78);
		newLog.setActivity(LogActivity.Harvesting);

		/**
		 * Asserts the successful insertion
		 */
		StepVerifier.create(testedRepos.save(newLog))
			.expectNextCount(1)
			.verifyComplete();
		// :~)

		var savedPk = LandLog.PK.from(
			landId, newLog.getTime(), sampleBlock.getId()
		);

		/**
		 * Asserts the saved data
		 */
		StepVerifier.create(
			testedRepos.findById(savedPk)
		)
			.assertNext(savedLog -> {
				assertThat(savedLog)
					.hasFieldOrPropertyWithValue("activity", newLog.getActivity())
					.hasFieldOrPropertyWithValue("usedTimeSecond", newLog.getUsedTimeSecond())
					.extracting("payload")
					.hasFieldOrPropertyWithValue("crop", sampleBlock.getCrop())
					.hasFieldOrPropertyWithValue("sowTime", sampleBlock.getSowTime())
					.hasFieldOrPropertyWithValue("matureTime", sampleBlock.getMatureTime())
					.hasFieldOrPropertyWithValue("harvestAmount", sampleBlock.getHarvestAmount())
					.hasFieldOrPropertyWithValue("updateTime", sampleBlock.getUpdateTime())
					.hasFieldOrPropertyWithValue("comment", sampleBlock.getComment());
			})
			.verifyComplete();
		// :~)
	}

	/**
	 * Tests the listing of logs by land(paging).
	 */
	@Test
	@CassandraData(
		executors={ RandomDataOfLandLog.class },
		phase=BEFORE
	)
	@CassandraData(
		cqls={
			"""
			DELETE FROM vc_land_log_by_time
			WHERE ll_ld_id = 417f4362-10d1-11ed-875f-00155d8fd4c9
			"""
		},
		phase=AFTER
	)
	void findByLandId()
	{
		final var sampleLandId = UUID.fromString("417f4362-10d1-11ed-875f-00155d8fd4c9");
		final var askedSize = 6;

		var testedResult = testedRepos.findByPk_LandId(
			sampleLandId,
			CassandraPageRequest.first(askedSize)
		);
		StepVerifier.create(testedResult)
			.assertNext(slice -> assertThat(slice).hasSize(askedSize))
			.verifyComplete();
	}

	/**
	 * Tests the listing of logs by land(paging).
	 */
	@CassandraData(
		executors={ RandomDataOfLandLog.class },
		phase=BEFORE
	)
	@CassandraData(
		cqls={
			"""
			DELETE FROM vc_land_log_by_time
			WHERE ll_ld_id = 417f4362-10d1-11ed-875f-00155d8fd4c9
			"""
		},
		phase=AFTER
	)
	@ParameterizedTest
	@MethodSource
	void findByTimeRangeOfLand(
		int askedSize,
		Instant startTime, Instant endTime,
		Object[] expectedBlockIds
	) {
		var pageable = CassandraPageRequest.of(
			0, askedSize,
			Sort.by(Sort.Direction.DESC, "pk.time")
		);
		var testedResult = testedRepos.findByTimeRangeOfLand(
			RandomDataOfLandLog.LAND_ID, startTime, endTime,
			pageable
		)
			.map(Slice::toList);

		StepVerifier.create(testedResult)
			.assertNext(l -> {
				assertThat(l)
					.hasSize(expectedBlockIds.length)
					.extracting("pk.blockId")
					.containsExactly(expectedBlockIds);
			})
			.verifyComplete();
	}
	static Arguments[] findByTimeRangeOfLand()
	{
		return new Arguments[] {
			arguments(6, null, null, new Short[] { 9, 8, 7, 6, 5, 4 }),
			arguments(6,
				"2020-05-11T10:31:10Z", null,
				new Short[] { 9, 8, 7 }
			),
			arguments(6,
				null, "2020-05-11T10:30:40Z",
				new Short[] { 4, 3, 2, 1, 0 }
			),
			arguments(6,
				"2020-05-11T10:30:20Z",
				"2020-05-11T10:30:40Z",
				new Short[] { 4, 3, 2 }
			),
		};
	}

	static class RandomDataOfLandLog implements CassandraDataExecutor {
		final static UUID LAND_ID = UUID.fromString("417f4362-10d1-11ed-875f-00155d8fd4c9");

		private final static int NUMBER_OF_ROWS = 10;

		/**
		 * Ranges of update time
		 * From 202-05-11T10:30:00Z ~ 2020-05-11T10:31:40Z
		 */
		final static Instant baseTime = Instant.parse("2020-05-11T10:30:00Z");

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
