package guru.mikelue.farming.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;
import java.util.function.BiFunction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.mikelue.farming.model.AskBlockAction;
import guru.mikelue.farming.model.Block;
import guru.mikelue.farming.model.Block.Status;
import guru.mikelue.farming.model.Climate;
import guru.mikelue.farming.repos.jpa.BlockRepos;
import guru.mikelue.farming.repos.jpa.LandRepos;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class FarmingService {
	private final static Logger logger = LoggerFactory.getLogger(FarmingService.class);

	@Autowired
	private FarmingProducerService queueService;

	@Autowired
	private BlockRepos blockRepos;

	@Autowired
	private LandRepos landRepos;

	public FarmingService () {}

	public Flux<Block> askSow(AskBlockAction sowingInfo)
	{
		var changeTime = Instant.now();

		return Mono.just(sowingInfo)
			// Checks the if the crop fits the climate of land
			.doOnNext(info -> {
				var land = landRepos.findById(info.getLandId())
					.orElseThrow();

				var suitableCrops = Climate.SuitableCrops.get(land.getClimate());
				if (!suitableCrops.contains(info.getCrop())) {
					throw new UnsuitableCropException(land, sowingInfo.getCrop());
				}
			})
			// Looks for available blocks
			.flatMapMany(info ->
				Flux.fromIterable(
					blockRepos.findAvailableByLandId(
						info.getLandId(), info.getAskedBlocks()
					)
				)
				.doOnError(e ->
					logger.warn(
						"Looking for avaialbe blocks has error. [{} <-> {}(blocks)]. {}",
						info.getLandId(), info.getAskedBlocks(), e.getMessage()
					)
				)
				.map(block -> {
					block.setCrop(info.getCrop());
					block.setComment(info.getComment());
					block.setUpdateTime(changeTime);
					return block;
				})
			)
			.mapNotNull(block -> {
				var haveUpdated = blockRepos.updateStatusByCheckPreviousOne(
					block, Status.ScheduledSow
				) == 1;

				return haveUpdated ? block : null;
			})
			.onErrorContinue(
				e -> !UnsuitableCropException.class.isInstance(e),
				(e, b) -> {
					logger.error("The status of block[{}] cannot be updated. Exception: {}", b, e);
				}
			)
			.flatMap(block -> {
				logger.debug("[Send Scheduled Sowing] --> {}.", block);

				block.setStatus(Status.ScheduledSow);
				return queueService.sendSowing(block)
					.publishOn(Schedulers.boundedElastic())
					.thenReturn(block);
			})
			.onErrorContinue(
				e -> !UnsuitableCropException.class.isInstance(e),
				(e, b) -> {
					logger.error("Block[{}] cannot be put into queue. Exception: {}", b, e);
				}
			);
	}

	/**
	 * Performs cleaning of blocks, this method would substract
	 * the number of available blocks(for now) from requested one.
	 */
	public Flux<Block> askClean(AskBlockAction cleaningInfo)
	{
		var changeTime = Instant.now();

		return Mono.just(new AskBlockAction(cleaningInfo))
			// Gets current avaialbe blocks and substracts from the number of requested
			.mapNotNull(info -> {
				var countOfAvailables = blockRepos.countByLandIdAndStatus(
					info.getLandId(), Status.Available, info.getAskedBlocks()
				);

				info.setAskedBlocks(
					(short)(info.getAskedBlocks() - countOfAvailables)
				);

				return info.getAskedBlocks() > 0 ? info : null;
			})
			// Looks for occupied blocks
			.flatMapMany(info ->
				Flux.fromIterable(
					blockRepos.findOccupiedByLandId(
						info.getLandId(), info.getAskedBlocks()
					)
				)
				.doOnError(e ->
					logger.warn(
						"[Clean] Looking for occupied blocks has error. [{} <-> {}(blocks)]. {}",
						info.getLandId(), info.getAskedBlocks(), e.getMessage()
					)
				)
				.map(b -> {
					b.setUpdateTime(changeTime);
					b.setComment(info.getComment());
					return b;
				})
			)
			.mapNotNull(block -> {
				var haveUpdated = blockRepos.updateStatusByCheckPreviousOne(
					block, Status.ScheduledClean
				) == 1;

				return haveUpdated ? block : null;
			})
			.onErrorContinue((e, b) -> {
				logger.error("The status of block[{}] cannot be updated: {}", b, e);
			})
			.flatMap(block -> {
				logger.debug("[Send Scheduled Cleaning] --> {}.", block);

				block.setStatus(Status.ScheduledClean);

				return queueService.sendCleaning(block)
					.publishOn(Schedulers.boundedElastic())
					.thenReturn(block);
			})
			.onErrorContinue((e, b) -> {
				logger.error("Block[{}] cannot be put into queue: {}", b, e);
			});
	}

	private final static int TOO_LONG_PAGE_SIZE = 32;

	@Value("${valor.farming.duration.too-long-scheduled-activities}")
	private Duration durationForTooLongScheduledBlocks;

	/**
	 * Collects out-dated blocks which remain in scheduled status(sowing, harvesting, or cleaning).
	 */
	/**
	 * Collects matured blocks periodically.
	 */
	@Scheduled(
		initialDelayString="${schedule.too-long-scheduled-activities.initial-delay}",
		fixedDelayString="${schedule.too-long-scheduled-activities.fixed-delay}"
	)
	public void processTooLongScheduledBlocks()
	{
		var checkTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)
			.minusSeconds(durationForTooLongScheduledBlocks.toSeconds());

		buildProcessTooLongBlocks(checkTime)
			.doFirst(() -> {
				logger.info("Start finding scheduled blocks of too long");
			})
			.count()
			.doOnNext(n -> {
				logger.info("[Complete] Fetch [{}] blocks of too long", n);
			})
			.block();
	}

	Flux<Block> buildProcessTooLongBlocks(Instant checkTime)
	{
		return Flux
			.push(
				buildSinkerForFindingBlocks(
					checkTime, TOO_LONG_PAGE_SIZE,
					blockRepos::findOldScheduledActivities,
					"Too-Long Scheduled"
				)
			)
			.flatMap(block -> {
				logger.debug("Re-send scheduled blocks: [{}]. Status: {}", block, block.getStatus());

				var sendResult = switch (block.getStatus()) {
					case ScheduledSow -> queueService.sendSowing(block);
					case ScheduledHarvest -> queueService.sendHarvesting(block);
					case ScheduledClean -> queueService.sendCleaning(block);
					default -> {
						throw new RuntimeException("Unable to process non-scheduled block.");
					}
				};

				return sendResult
					.publishOn(Schedulers.boundedElastic())
					.thenReturn(block);
			})
			.onErrorContinue((e, b) -> {
				logger.error("Unable to re-send block: [{}]. Exception: {}", b, e);
			});
	}

	private final static int MATURED_PAGE_SIZE = 32;

	/**
	 * Collects matured blocks periodically.
	 */
	@Scheduled(
		initialDelayString="${schedule.harvesting.initial-delay}",
		fixedDelayString="${schedule.harvesting.fixed-delay}"
	)
	public void processMaturedBlocks()
	{
		var now = Instant.now();

		buildProcessMaturedBlocks(now)
			.doFirst(() -> {
				logger.info("Start finding matured blocks");
			})
			.count()
			.doOnNext(n -> {
				logger.info("[Complete] Fetch [{}] blocks of matured", n);
			})
			.block();
	}

	Flux<Block> buildProcessMaturedBlocks(Instant checkTime)
	{
		return Flux
			.push(
				buildSinkerForFindingBlocks(
					checkTime, MATURED_PAGE_SIZE,
					blockRepos::findMaturedBlocksByTime,
					"Matured"
				)
			)
			.mapNotNull(block -> {
				block.setUpdateTime(checkTime);

				var haveUpdated = blockRepos.updateStatusByCheckPreviousOne(
					block, Status.ScheduledHarvest
				) == 1;

				return haveUpdated ? block : null;
			})
			.onErrorContinue((e, b) -> {
				logger.error("Unable to schedule matured block: [{}]. Exception: {}", b, e);
			})
			.flatMap(block -> {
				logger.debug("[Send Scheduled Harvesting] --> {}.", block);

				block.setStatus(Status.ScheduledHarvest);
				return queueService.sendHarvesting(block)
					.publishOn(Schedulers.boundedElastic())
					.thenReturn(block);
			})
			.onErrorContinue((e, b) -> {
				logger.error("Unable to send message for matured block: [{}]. Exception: {}", b, e);
			});
	}

	private static Consumer<FluxSink<Block>> buildSinkerForFindingBlocks(
		Instant checkTime, int pageSize,
		BiFunction<Instant, Pageable, Slice<Block>> loadDataFunc,
		String message
	) {
		return (sinker) -> {
			var page = 0;

			Slice<Block> foundBlocks = null;

			do {
				var pageable = PageRequest.of(page, pageSize);

				foundBlocks = loadDataFunc.apply(checkTime, pageable);

				logger.debug("[{}] Found blocks: [{}]", message, foundBlocks.getNumberOfElements());

				for (var b: foundBlocks) {
					sinker.next(b);
				}

				page++;
			} while (foundBlocks.hasNext());

			sinker.complete();
		};
	}
}
