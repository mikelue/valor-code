package guru.mikelue.farming.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import guru.mikelue.farming.config.KafkaConfig;
import guru.mikelue.farming.model.Block;
import guru.mikelue.farming.model.LandLog;
import guru.mikelue.farming.model.LogActivity;
import guru.mikelue.farming.model.Block.BlockId;
import guru.mikelue.farming.model.Block.Status;
import guru.mikelue.farming.model.RandomModels;
import guru.mikelue.farming.repos.cassandra.LandLogRepos;
import guru.mikelue.farming.repos.jpa.BlockRepos;
import guru.mikelue.misc.testlib.AbstractEmbededKafkaTestBase;

import reactor.core.publisher.Mono;

import static guru.mikelue.farming.model.LogActivity.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.INSTANT;
import static org.assertj.core.api.InstanceOfAssertFactories.SHORT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;

@Import({FarmingConsumerService.class})
public class FarmingConsumerServiceTest extends AbstractEmbededKafkaTestBase {
	@Autowired
	private KafkaTemplate<Block.BlockId, Block> kafkaTemplate;

	@MockBean
	private BlockRepos mockBlockRepos;
	@MockBean
	private LandLogRepos mockLandLogRepos;

	public FarmingConsumerServiceTest() {}

	/**
	 * Tests the handling(consumeing) of block message for sowing.
	 */
	@Test
	void handleSowing()
	{
		var sampleBlock = RandomModels.randomBlock();
		sampleBlock.setStatus(Status.ScheduledSow);
		sampleBlock.setSowTime(null);
		sampleBlock.setMatureTime(null);
		sampleBlock.setHarvestAmount(null);

		kafkaTemplate.send(
			KafkaConfig.TOPIC_SOWING,
			sampleBlock.getBlockId(),
			sampleBlock
		);

		/**
		 * Mocks the calling to BlockRepos/LandLogRepos
		 */
		when(mockBlockRepos.findById(sampleBlock.getBlockId()))
			.thenReturn(Optional.of(sampleBlock));
		when(mockBlockRepos.updateToBeSowed(any(Block.class)))
			.thenReturn(1);
		when(mockLandLogRepos.save(any(LandLog.class)))
			.thenReturn(Mono.just(LandLog.from(sampleBlock)));
		// :~)

		var verifyMode = timeout(15000).times(1);

		/**
		 * Asserts the block object received by BlockRepos.updateToBeSowed()
		 */
		var blockArgv = ArgumentCaptor.forClass(Block.class);
		verify(mockBlockRepos, verifyMode)
			.updateToBeSowed(blockArgv.capture());

		var testedBlock = blockArgv.getValue();
		var assertBlock = assertThat(testedBlock);

		assertBlock
			.hasFieldOrPropertyWithValue("status", Status.Occupied)
			.hasFieldOrPropertyWithValue("comment", sampleBlock.getComment())
			.extracting("matureTime", INSTANT)
			.isAfter(testedBlock.getSowTime());

		assertBlock
			.extracting("harvestAmount", SHORT)
			.isGreaterThan((short)0);
		// :~)

		assertLandLog(Sowing);

		clearInvocations(mockBlockRepos);
		clearInvocations(mockLandLogRepos);
	}

	/**
	 * Tests the handling(consumeing) of block message for harvesting.
	 */
	@Test
	void handleHarvesting()
	{
		var sampleBlock = RandomModels.randomBlock();
		sampleBlock.setStatus(Status.ScheduledHarvest);

		kafkaTemplate.send(
			KafkaConfig.TOPIC_HARVESTING,
			sampleBlock.getBlockId(),
			sampleBlock
		);

		mockAndAssertCleanedBlock(null, Harvesting);
	}

	/**
	 * Tests the handling(consumeing) of block message for cleaning.
	 */
	@Test
	void handleCleaning()
	{
		var sampleBlock = RandomModels.randomBlock();
		sampleBlock.setStatus(Status.Occupied);

		kafkaTemplate.send(
			KafkaConfig.TOPIC_CLEANING,
			sampleBlock.getBlockId(),
			sampleBlock
		);

		mockAndAssertCleanedBlock(sampleBlock.getComment(), Cleaning);
	}

	private void mockAndAssertCleanedBlock(
		String expectedComment, LogActivity expectedActivity
	) {
		var sampleBlock = RandomModels.randomBlock();

		/**
		 * Mocks the calling to BlockRepos/LandLogRepos
		 */
		when(mockBlockRepos.findById(any(BlockId.class)))
			.thenReturn(Optional.of(sampleBlock));
		when(mockBlockRepos.updateForCleaning(any(Block.class)))
			.thenReturn(1);
		when(mockLandLogRepos.save(any(LandLog.class)))
			.thenReturn(Mono.just(LandLog.from(sampleBlock)));
		// :~)

		/**
		 * Asserts the block object received by BlockRepos.updateToBeSowed()
		 */
		var verifyMode = timeout(15000).times(1);
		var blockArgv = ArgumentCaptor.forClass(Block.class);
		verify(mockBlockRepos, verifyMode)
			.updateForCleaning(blockArgv.capture());

		assertThat(blockArgv.getValue())
			.hasFieldOrPropertyWithValue("status", Status.Available)
			.hasFieldOrPropertyWithValue("crop", null)
			.hasFieldOrPropertyWithValue("sowTime", null)
			.hasFieldOrPropertyWithValue("matureTime", null)
			.hasFieldOrPropertyWithValue("harvestAmount", null)
			.hasFieldOrPropertyWithValue("comment", expectedComment);
		// :~)

		assertLandLog(expectedActivity);

		clearInvocations(mockBlockRepos);
		clearInvocations(mockLandLogRepos);
	}

	private void assertLandLog(LogActivity expectedActivity)
	{
		var verifyMode = timeout(15000).times(1);

		/**
		 * Asserts the block object received by LandLogRepos.save()
		 */
		var landLogArgv = ArgumentCaptor.forClass(LandLog.class);
		verify(mockLandLogRepos, verifyMode)
			.save(landLogArgv.capture());

		var assertLog = assertThat(
			landLogArgv.getValue()
		);

		assertLog
			.hasFieldOrPropertyWithValue("activity", expectedActivity)
			.extracting("usedTimeSecond", SHORT)
			.isGreaterThan((short)0);

		assertLog
			.extracting(LandLog::getTime, INSTANT)
			.isBefore(Instant.now());
		// :~)
	}
}
