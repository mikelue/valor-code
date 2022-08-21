package guru.mikelue.farming.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.support.SendResult;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

import guru.mikelue.farming.config.KafkaConfig;
import guru.mikelue.farming.model.Block;
import guru.mikelue.farming.model.Block.BlockId;
import guru.mikelue.misc.testlib.AbstractEmbededKafkaTestBase;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Import(FarmingProducerService.class)
public class FarmingProducerServiceTest extends AbstractEmbededKafkaTestBase {
	@Autowired
	private FarmingProducerService testedService;

	public FarmingProducerServiceTest() {}

	/**
	 * Tests the sending of block message for sowing.
	 */
	@Test
	void sendSowing()
	{
		var sampleBlock = randomBlock();

		var testedResult = testedService.sendSowing(sampleBlock);

		assertMonoAndKafkaTopic(
			"Test-sendSowing",
			testedResult, KafkaConfig.TOPIC_SOWING
		);
	}

	/**
	 * Tests the sending of block message for harvesting.
	 */
	@Test
	void sendHarvesting()
	{
		var sampleBlock = randomBlock();
		var testedResult = testedService.sendHarvesting(sampleBlock);

		assertMonoAndKafkaTopic(
			"Test-sendHarvesting",
			testedResult, KafkaConfig.TOPIC_HARVESTING
		);
	}

	/**
	 * Tests the sending of block message for cleaning.
	 */
	@Test
	void sendCleaning()
	{
		var sampleBlock = randomBlock();
		var testedResult = testedService.sendCleaning(sampleBlock);

		assertMonoAndKafkaTopic(
			"Test-sendCleaning",
			testedResult, KafkaConfig.TOPIC_CLEANING
		);
	}

	private void assertMonoAndKafkaTopic(
		String gropuId,
		Mono<SendResult<BlockId, Block>> testedResult, String topicName
	) {
		StepVerifier.create(testedResult)
			.assertNext( r -> {
				var metadata = r.getRecordMetadata();

				getLogger().info("SendResult. Offset: [{}]. Key size: [{}]. Value size: [{}]",
					metadata.offset(),
					metadata.serializedKeySize(),
					metadata.serializedValueSize()
				);
			})
			.verifyComplete();

		// var testedRecords = consumeAllWith(gropuId, topicName);
		var testedRecords = consumeAllWith(gropuId, topicName);
		assertThat(testedRecords.count())
			.isEqualTo(1);
	}
	private Block randomBlock()
	{
		var sampleBlock = new Block();
		sampleBlock.setLandId(UUID.randomUUID());
		sampleBlock.setId((short)RandomUtils.nextInt(0, Short.MAX_VALUE));

		return sampleBlock;
	}
}
