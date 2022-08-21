package guru.mikelue.farming.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.kafka.support.SendResult;
import org.junit.jupiter.api.Test;
import guru.mikelue.farming.model.AskBlockAction;
import guru.mikelue.farming.model.Block;
import guru.mikelue.farming.model.Climate;
import guru.mikelue.farming.model.Block.Status;
import guru.mikelue.farming.model.Crop;
import guru.mikelue.farming.model.Land;
import guru.mikelue.farming.repos.jpa.BlockRepos;
import guru.mikelue.farming.repos.jpa.LandRepos;
import guru.mikelue.misc.testlib.AbstractTestBase;

import mockit.*;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the logic of farming service in mocked environments
 */
public class FarmingServiceTest extends AbstractTestBase {
	@Injectable
	private FarmingProducerService mockQueueService;
	@Injectable
	private BlockRepos mockBlockRepos;
	@Injectable
	private LandRepos mockLandRepos;

	@Tested
	private FarmingService testedService;

	public FarmingServiceTest() {}

	/**
	 * Tests the available blocks for sowing queue.
	 */
	@Test
	void askSow(
		@Mocked
		SendResult<?, ?> mockSendResult
	) {
		final var totalBlocks = (short)10;
		final var availableBlocks = (short)7;

		var sampleAskSowing = new AskBlockAction();
		sampleAskSowing.setLandId(UUID.randomUUID());
		sampleAskSowing.setCrop(Crop.Manioc);
		sampleAskSowing.setAskedBlocks(totalBlocks);
		sampleAskSowing.setComment("Honduras apricot");

		var sampleLand = new Land();
		sampleLand.setId(sampleAskSowing.getLandId());
		sampleLand.setClimate(Climate.Mild);

		var sampleBlocks = randomBlocks(sampleAskSowing);

		/**
		 * Sets-up mocks
		 */
		new Expectations() {{
			mockLandRepos.findById(sampleAskSowing.getLandId());
			result = Optional.of(sampleLand);
			times = 1;

			mockBlockRepos.findAvailableByLandId(
				sampleAskSowing.getLandId(),
				sampleAskSowing.getAskedBlocks()
			);
			result = sampleBlocks;
			times = 1;

			/**
			 * Sets-up available blocks
			 */
			mockBlockRepos.updateStatusByCheckPreviousOne(
				(Block)any, Status.ScheduledSow
			);
			result = buildNumberSerials(totalBlocks, availableBlocks);
			times = totalBlocks;
			// :~)

			mockQueueService.sendSowing((Block)any);
			result = Mono.just(mockSendResult);
			times = availableBlocks;
		}};
		// :~)

		var testedResult = testedService.askSow(sampleAskSowing)
			.collectList()
			.block();

		/**
		 * Asserts the size of result blocks and their modified properties
		 */
		assertThat(testedResult)
			.hasSize(availableBlocks);

		var checkedTime = Instant.now().minusSeconds(3000);
		for (var b: testedResult) {
			assertThat(b)
				.hasFieldOrPropertyWithValue("crop", sampleAskSowing.getCrop())
				.hasFieldOrPropertyWithValue("comment", sampleAskSowing.getComment())
				.hasFieldOrPropertyWithValue("status", Status.ScheduledSow)
				.extracting("updateTime", INSTANT)
				.isAfter(checkedTime);
		}
		// :~)
	}


	/**
	 * Tests the occupied blocks for cleaning queue.
	 */
	@Test
	void askClean(
		@Mocked
		SendResult<?, ?> mockSendResult
	) {
		final var totalBlocks = (short)30;
		final var availableBlocks = (short)20;
		final var occupiedBlocks = (short)(totalBlocks - availableBlocks);
		final var scheduledBlocks = (short)(occupiedBlocks - 3);

		var sampleAskCleaning = new AskBlockAction();
		sampleAskCleaning.setLandId(UUID.randomUUID());
		sampleAskCleaning.setComment("fennelg goat");
		sampleAskCleaning.setAskedBlocks(totalBlocks);

		var sampleBlocks = randomBlocks(
			sampleAskCleaning.getLandId(), occupiedBlocks,
			Status.Occupied
		);

		/**
		 * Sets-up mocks
		 */
		new Expectations() {{
			mockBlockRepos.countByLandIdAndStatus(
				sampleAskCleaning.getLandId(),
				Status.Available,
				sampleAskCleaning.getAskedBlocks()
			);
			result = availableBlocks;
			times = 1;

			mockBlockRepos.findOccupiedByLandId(
				sampleAskCleaning.getLandId(),
				occupiedBlocks
			);
			result = sampleBlocks;
			times = 1;

			/**
			 * Sets-up available blocks
			 */
			mockBlockRepos.updateStatusByCheckPreviousOne(
				(Block)any, Status.ScheduledClean
			);
			result = buildNumberSerials(occupiedBlocks, scheduledBlocks);
			times = occupiedBlocks;
			// :~)

			mockQueueService.sendCleaning((Block)any);
			result = Mono.just(mockSendResult);
			times = scheduledBlocks;
		}};
		// :~)

		var testedResult = testedService.askClean(sampleAskCleaning)
			.collectList()
			.block();

		/**
		 * Asserts the size of result blocks and their modified properties
		 */
		assertThat(testedResult)
			.hasSize(scheduledBlocks);

		var checkedTime = Instant.now().minusSeconds(3000);
		for (var b: testedResult) {
			assertThat(b)
				.hasFieldOrPropertyWithValue("comment", sampleAskCleaning.getComment())
				.hasFieldOrPropertyWithValue("status", Status.ScheduledClean)
				.extracting("updateTime", INSTANT)
				.isAfter(checkedTime);
		}
		// :~)
	}

	/**
	 * Tests the sufficient blocks of available for cleaning.
	 */
	@Test
	void askCleanWithSufficientBlocksOfAvailable()
	{
		final var totalBlocks = (short)30;
		final var availableBlocks = (short)30;

		var sampleAskCleaning = new AskBlockAction();
		sampleAskCleaning.setLandId(UUID.randomUUID());
		sampleAskCleaning.setAskedBlocks(totalBlocks);

		/**
		 * Sets-up mocks
		 */
		new Expectations() {{
			mockBlockRepos.countByLandIdAndStatus(
				sampleAskCleaning.getLandId(),
				Status.Available,
				sampleAskCleaning.getAskedBlocks()
			);
			result = availableBlocks;
			times = 1;

			mockBlockRepos.findOccupiedByLandId(
				(UUID)any, anyShort
			);
			times = 0;

			/**
			 * No-needed for scheduled cleaning
			 */
			mockBlockRepos.updateStatusByCheckPreviousOne(
				(Block)any, Status.ScheduledClean
			);
			times = 0;
			// :~)

			mockQueueService.sendCleaning((Block)any);
			times = 0;
		}};
		// :~)

		StepVerifier.create(
			testedService.askClean(sampleAskCleaning)
		)
			.verifyComplete();
	}

	/**
	 * Tests the exception thrown by improper crop.
	 */
	@Test
	void askSowWithImproperCrop()
	{
		final var totalBlocks = (short)10;

		var sampleAskSowing = new AskBlockAction();
		sampleAskSowing.setLandId(UUID.randomUUID());
		sampleAskSowing.setCrop(Crop.Lettuce);
		sampleAskSowing.setAskedBlocks(totalBlocks);
		sampleAskSowing.setComment("Honduras apricot");

		var sampleLand = new Land();
		sampleLand.setId(sampleAskSowing.getLandId());
		sampleLand.setName("peacock Inc.");
		sampleLand.setClimate(Climate.Tropical);

		/**
		 * Sets-up mocks
		 */
		new Expectations() {{
			mockLandRepos.findById(sampleAskSowing.getLandId());
			result = Optional.of(sampleLand);
			times = 1;

			mockBlockRepos.findAvailableByLandId((UUID)any, anyInt);
			times = 0;
		}};
		// :~)

		var testedResult = testedService.askSow(sampleAskSowing);

		StepVerifier.create(testedResult)
			.expectErrorSatisfies(e -> assertThat(e)
				.isInstanceOf(UnsuitableCropException.class)
				.hasMessageContainingAll(
					"Tropical", "Lettuce",
					sampleLand.getName(), sampleLand.getId().toString()
				)
			)
			.verify();
	}

	/**
	 * Tests the matured blocks for harvesting queue.
	 */
	@Test
	void buildProcessMaturedBlocks(
		@Mocked
		SendResult<?, ?> mockSendResult
	) {
		// More than one page
		final var totalBlocks = (short)50;
		final var matchedBlocks = (short)40;

		/**
		 * Sets-up mocks
		 */
		new Expectations() {{
			mockBlockRepos.findMaturedBlocksByTime(
				(Instant)any, (Pageable)any

			);
			var sampleSlicesOfBlocks = buildMultiplePages(totalBlocks, 30, Status.Occupied);
			returns(sampleSlicesOfBlocks[0], sampleSlicesOfBlocks[1]);
			// times = 1;

			/**
			 * Sets-up available blocks
			 */
			mockBlockRepos.updateStatusByCheckPreviousOne(
				(Block)any, Status.ScheduledHarvest
			);
			result = buildNumberSerials(totalBlocks, matchedBlocks);
			times = totalBlocks;
			// :~)

			mockQueueService.sendHarvesting((Block)any);
			result = Mono.just(mockSendResult);
			times = matchedBlocks;
		}};
		// :~)

		var now = Instant.now();
		var testedResult = testedService.buildProcessMaturedBlocks(now)
			.collectList()
			.block();

		/**
		 * Asserts the size of result blocks and their modified properties
		 */
		assertThat(testedResult)
			.hasSize(matchedBlocks);

		for (var b: testedResult) {
			var assertBlock = assertThat(b);

			assertBlock
				.hasFieldOrPropertyWithValue("status", Status.ScheduledHarvest)
				.extracting("updateTime", INSTANT)
				.isEqualTo(now);
		}
		// :~)
	}

	/**
	 * Tests the processing for scheduled blocks of too-long.
	 */
	@Test
	void buildProcessTooLongBlocks()
	{
		var testTime = Instant.now();

		final var sampleBlocks = new ArrayList<Block>(6);
		sampleBlocks.addAll(randomBlocks(UUID.randomUUID(), 2, Status.ScheduledSow));
		sampleBlocks.addAll(randomBlocks(UUID.randomUUID(), 2, Status.ScheduledClean));
		sampleBlocks.addAll(randomBlocks(UUID.randomUUID(), 2, Status.ScheduledHarvest));

		new Expectations() {{
			mockBlockRepos.findOldScheduledActivities(testTime, (Pageable)any);
			result = new SliceImpl<>(sampleBlocks, PageRequest.of(1, 10), false);
			times = 1;

			mockQueueService.sendSowing((Block)any);
			result = Mono.empty();
			times = 2;

			mockQueueService.sendCleaning((Block)any);
			result = Mono.empty();
			times = 2;

			mockQueueService.sendHarvesting((Block)any);
			result = Mono.empty();
			times = 2;
		}};

		var testedResult = testedService.buildProcessTooLongBlocks(testTime);
		StepVerifier.create(testedResult)
			.expectNextCount(sampleBlocks.size())
			.verifyComplete();
	}

	private static List<Integer> buildNumberSerials(
		int totalTimes, int successfulTimes
	) {
		var resultList = new ArrayList<Integer>(totalTimes);

		for (var i = 0; i < totalTimes; i++) {
			resultList.add(
				i < successfulTimes ? 1 : 0
			);
		}

		return resultList;
	}
	private static List<Block> randomBlocks(AskBlockAction sowInfo)
	{
		return randomBlocks(sowInfo.getLandId(), sowInfo.getAskedBlocks(), Status.Available);
	}

	private static List<Block> randomBlocks(UUID landId, int number, Status status)
	{
		var resultList = new ArrayList<Block>(number);

		var updateTime = Instant.now().minusSeconds(1200);
		for (var i = 0; i < number; i++) {
			var newBlock = new Block();
			newBlock.setId((short)i);
			newBlock.setLandId(landId);
			newBlock.setStatus(status);
			newBlock.setCrop(Crop.Rice);
			newBlock.setUpdateTime(updateTime);

			resultList.add(newBlock);
		}

		return resultList;
	}

	private static Object[] buildMultiplePages(int totalSize, int pageSize, Status status)
	{
		var numberOfPages = totalSize / pageSize + 1;
		var resultPages = new ArrayList<Slice<Block>>(numberOfPages);

		for (var i = 0; i < numberOfPages; i++) {
			var numberOfBlocks = Math.min(totalSize, pageSize);

			var sampleBlocks = randomBlocks(UUID.randomUUID(), numberOfBlocks, status);
			var lastPage = i == (numberOfPages - 1);

			var newPage = new SliceImpl<Block>(sampleBlocks, PageRequest.of(i, pageSize), !lastPage);

			resultPages.add(newPage);

			totalSize -= pageSize;
		}

		return resultPages.toArray();
	}
}
