package guru.mikelue.farming.service;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import guru.mikelue.farming.config.KafkaConfig;
import guru.mikelue.farming.model.Block;
import guru.mikelue.farming.model.Block.Status;
import guru.mikelue.farming.model.CropProperties;
import guru.mikelue.farming.model.LandLog;
import guru.mikelue.farming.model.LogActivity;
import guru.mikelue.farming.repos.cassandra.LandLogRepos;
import guru.mikelue.farming.repos.jpa.BlockRepos;

import static java.time.temporal.ChronoUnit.SECONDS;
import static guru.mikelue.farming.model.LogActivity.*;

@Service
public class FarmingConsumerService {
	private final Logger logger = LoggerFactory.getLogger(FarmingConsumerService.class);

	@Autowired
	private BlockRepos blockRepos;
	@Autowired
	private LandLogRepos landLogRepos;

	public FarmingConsumerService () {}

	@KafkaListener(id="farming-sowing", topics=KafkaConfig.TOPIC_SOWING)
	public void handleSowing(Block block)
	{
		var crop = block.getCrop();
		var sowingDuration = Duration.ofSeconds(
			CropProperties.getSowingTime(crop)
		);
		var matureDuration = Duration.ofSeconds(
			CropProperties.getGrowingTime(crop)
		);

		Mono.just(block)
			.map(b -> {
				var now = Instant.now().truncatedTo(SECONDS);
				b.setSowTime(now);
				b.setUpdateTime(now);

				return b;
			})
			.delayElement(sowingDuration) // Waiting for sowing
			.map(b -> { // Set-up mature time and status
				b.setMatureTime(
					Instant.now()
						.truncatedTo(SECONDS)
						.plusSeconds(matureDuration.toSeconds())
				);
				b.setHarvestAmount(
					CropProperties.getHarvestingQuanity(b.getCrop())
				);
				b.setStatus(Status.Occupied);

				logger.debug("Sowing: [{}]", b);
				return b;
			})
			.mapNotNull(b -> {
				var existingBlock = blockRepos.findById(b.getBlockId());

				if (!existingBlock.isPresent()) {
					logger.warn("[Sowing] The block is not existing in database: [{}]", b.getBlockId());
					return null;
				}

				var updatedCount = blockRepos.updateToBeSowed(b);

				if (updatedCount == 1) {
					/**
					 * Builds log of land if the updating of block is successful
					 */
					var landLog = LandLog.from(existingBlock.get());
					landLog.setActivity(Sowing);
					landLog.setUsedTimeSecond((short)sowingDuration.toSeconds());
					landLog.setTime(b.getSowTime());

					return landLog;
					// :~)
				}

				logger.warn("Updating block[{}] to be sowed had nothing updated.", block);
				return null;
			})
			.flatMap(landLogRepos::save)
			.block();
	}

	@KafkaListener(id="farming-harvesting", topics=KafkaConfig.TOPIC_HARVESTING)
	public void handleHarvesting(Block block)
	{
		var crop = block.getCrop();
		var harvestingTime = Instant.now().truncatedTo(SECONDS);
		var harvestingDuration = Duration.ofSeconds(
			CropProperties.getHarvestingTime(crop)
		);

		Mono.just(block)
			.delayElement(harvestingDuration)
			.map(b -> {
				b = buildCleanBlock(
					harvestingTime, "Harvesting: [{}] for [{}]",
					harvestingDuration
				)
					.apply(b);
				b.setComment(null);
				return b;
			})
			.mapNotNull(buildUpdateCleanAndMapToLandLog(
				Harvesting, harvestingDuration, "Harvesting block[{}] to be cleaned had nothing updated."
			))
			.flatMap(landLogRepos::save)
			.block();
	}

	@KafkaListener(id="farming-cleaning", topics=KafkaConfig.TOPIC_CLEANING)
	public void handleCleaning(Block block)
	{
		var cleaningTime = Instant.now().truncatedTo(SECONDS);
		var cleaningDuration = Duration.ofSeconds(
			RandomUtils.nextInt(2, 10)
		);

		Mono.just(block)
			.delayElement(cleaningDuration)
			.map(buildCleanBlock(
				cleaningTime,
				"Cleaning: [{}] for [{}]", cleaningDuration
			))
			.mapNotNull(buildUpdateCleanAndMapToLandLog(
				Cleaning, cleaningDuration, "Cleaning block[{}] had nothing updated."
			))
			.flatMap(landLogRepos::save)
			.block();
	}

	private Function<Block, LandLog> buildUpdateCleanAndMapToLandLog(
		LogActivity activity, Duration usedTime, String warningFormat
	) {
		return b -> {
			var existingBlock = blockRepos.findById(b.getBlockId());
			if (!existingBlock.isPresent()) {
				logger.warn("[Sowing] The block is not existing in database: [{}]", b.getBlockId());
				return null;
			}

			var updatedCount = blockRepos.updateForCleaning(b);

			if (updatedCount == 1) {
				logger.debug("Have cleaned block[{}].", b);

				/**
				 * Builds log of land if the updating of block is successful
				 */
				var landLog = LandLog.from(existingBlock.get());
				landLog.setActivity(activity);
				landLog.setUsedTimeSecond((short)usedTime.toSeconds());
				landLog.setTime(b.getUpdateTime());

				return landLog;
				// :~)
			}

			logger.warn("Cleaning block[{}] had nothing updated.", b);
			return null;
		};
	}

	private Function<Block, Block> buildCleanBlock(Instant time, String loggingFormat, Object... formatArgs)
	{
		return block -> { // Clean-up the block
			logger.debug(loggingFormat, block, formatArgs);

			block.setCrop(null);
			block.setSowTime(null);
			block.setMatureTime(null);
			block.setHarvestAmount(null);
			block.setStatus(Status.Available);
			block.setUpdateTime(time);

			return block;
		};
	}
}
