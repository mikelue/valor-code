package guru.mikelue.farming.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import guru.mikelue.farming.config.KafkaConfig;
import guru.mikelue.farming.model.Block;

import reactor.core.publisher.Mono;

@Service
public class FarmingProducerService {
	@Autowired
	private KafkaTemplate<Block.BlockId, Block> kafkaTemplate;

	public FarmingProducerService () {}

	public Mono<SendResult<Block.BlockId, Block>> sendSowing(Block block)
	{
		return sendBlockToTopic(
			block, KafkaConfig.TOPIC_SOWING,
			"Unable to send sowing. Message: {}. Block: {}"
		);
	}

	public Mono<SendResult<Block.BlockId, Block>> sendHarvesting(Block block)
	{
		return sendBlockToTopic(
			block, KafkaConfig.TOPIC_HARVESTING,
			"Unable to send harvesting. Message: {}. Block: {}"
		);
	}

	public Mono<SendResult<Block.BlockId, Block>> sendCleaning(Block block)
	{
		return sendBlockToTopic(
			block, KafkaConfig.TOPIC_CLEANING,
			"Unable to send cleaning. Message: {}. Block: {}"
		);
	}

	private Mono<SendResult<Block.BlockId, Block>> sendBlockToTopic(
		Block block,
		String topicName, String messageForError
	) {
		return Mono.fromCompletionStage(
			() -> kafkaTemplate.send(
				topicName,
				block.getBlockId(), block
			).completable()
		);
	}
}
