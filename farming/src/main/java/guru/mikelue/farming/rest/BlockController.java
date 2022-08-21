package guru.mikelue.farming.rest;

import java.util.Map;
import java.util.UUID;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import guru.mikelue.farming.model.AskBlockAction;
import guru.mikelue.farming.model.Block;
import guru.mikelue.farming.repos.jpa.BlockRepos;
import guru.mikelue.farming.service.FarmingService;
import guru.mikelue.farming.service.UnsuitableCropException;
import guru.mikelue.farming.validate.Groups.ForCleaningBlock;
import guru.mikelue.farming.web.CodeAndDetail;
import guru.mikelue.farming.web.CodeAndDetailException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class BlockController extends AbstractControllerBase {
	@Autowired
	private BlockRepos blockRepos;
	@Autowired
	private FarmingService farmingService;

	public BlockController() {}

	@GetMapping("/land/{land_id}/blocks")
	Flux<Block> listBlocksByLand(
		@PathVariable("land_id") UUID landId
	) {
		return Mono.just(landId)
			.flatMapMany(id ->
				Flux.fromIterable(blockRepos.findByLandIdOrderById(id))
			);
	}

	@PostMapping("/land/{land_id}/sow")
	Flux<Block> askSowing(
		@PathVariable("land_id") UUID landId,
		@RequestBody @Valid
		Mono<AskBlockAction> sowing
	) {
		return sowing
			.map(action -> {
				action.setLandId(landId);
				return action;
			})
			.flatMapMany(farmingService::askSow)
			.onErrorMap(UnsuitableCropException.class, e ->
				new CodeAndDetailException(
					HttpStatus.BAD_REQUEST,
					new CodeAndDetail(
						1,
						Map.<String, Object>of(
							"climate", e.getLand().getClimate(),
							"crop", e.getTargetCrop()
						)
					),
					e
				)
			);
	}

	@PostMapping("/land/{land_id}/clean")
	Mono<Object> askClean(
		@PathVariable("land_id") UUID landId,
		@RequestBody @Validated(ForCleaningBlock.class)
		Mono<AskBlockAction> cleanRequest
	) {
		return cleanRequest
			.map(action -> {
				action.setLandId(landId);
				return action;
			})
			.flatMap(action -> farmingService
				.askClean(action)
				.count()
				.map(n -> Map.of(
					"number_of_available_blocks", action.getAskedBlocks() - n,
					"scheduled_blocks_for_cleaning", n
				))
			);
	}
}
