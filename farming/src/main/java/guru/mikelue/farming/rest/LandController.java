package guru.mikelue.farming.rest;

import static org.springframework.data.domain.Sort.Direction.DESC;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import guru.mikelue.farming.model.Land;
import guru.mikelue.farming.model.LandLog;
import guru.mikelue.farming.repos.cassandra.LandLogRepos;
import guru.mikelue.farming.repos.jpa.LandRepos;
import guru.mikelue.farming.validate.Groups;
import guru.mikelue.farming.web.ExceptionalResponse;
import guru.mikelue.misc.springframework.data.web.PagePropertyChanger;
import guru.mikelue.misc.springframework.data.web.PageableUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class LandController extends AbstractControllerBase {
	@Autowired
	private LandRepos landRepos;
	@Autowired
	private LandLogRepos landLogRepos;

	private static final Function<DataIntegrityViolationException, ResponseStatusException> LandIntegrityViolationMapper =
		ExceptionalResponse.buildMapForDataIntegrityViolation(
			Map.of("unq_vc_land__ld_name", 1)
		);
	private static final String FORMAT_UNABLE_TO_FIND_LAND = "Unable to find land. Id: [%s]";

	public LandController() {}

	@GetMapping("/land/{land_id}")
	Mono<Land> getById(
		@PathVariable("land_id") UUID landId
	) {
		return Mono.just(landId)
			.map(landRepos::findById)
			.transform(ExceptionalResponse.transformNotFoundResponse(
				FORMAT_UNABLE_TO_FIND_LAND, landId
			));
	}

	@GetMapping("/lands")
	Flux<Land> list(
		@PageableDefault(sort="name")
		Pageable pageable
	) {
		return safePageable(pageable)
			.flatMapMany(p -> Flux.fromIterable(
				landRepos.findAll(p)
			));
	}

	@PostMapping("/land")
	@ResponseStatus(HttpStatus.CREATED)
	Mono<Land> addNew(
		@RequestBody @Valid
		Mono<Land> newLand
	) {
		return newLand
			.map(land -> landRepos.addNewWithBlocks(land))
			.onErrorMap(DataIntegrityViolationException.class, LandIntegrityViolationMapper);
	}

	@PutMapping("/land/{land_id}")
	Mono<Land> modify(
		@PathVariable("land_id") UUID landId,
		@RequestBody @Validated(value=Groups.WhenUpdate.class)
		Mono<Land> modifiedContent
	) {
		return modifiedContent
			.doOnNext(land -> {
				land.setId(landId);
				landRepos.save(land);
			})
			.onErrorMap(DataIntegrityViolationException.class, LandIntegrityViolationMapper)
			.map(savedLand -> landRepos.findById(savedLand.getId()))
			.transform(ExceptionalResponse.transformNotFoundResponse(
				FORMAT_UNABLE_TO_FIND_LAND, landId
			));
	}

	private final static PagePropertyChanger listLogsPropertyMapper =
		PagePropertyChanger.from(Map.of("time", "pk.time"));

	@GetMapping("/land/{land_id}/logs")
	Flux<LandLog> list(
		@PathVariable("land_id")
		UUID landId,
		@RequestParam(name="start_time", required=false)
		Instant startTime,
		@RequestParam(name="end_time", required=false)
		Instant endTime,
		@PageableDefault(sort="time", direction=DESC)
		Pageable pageable
	) {
		var pageInfo = PageableUtils.LimitSizeOfPage(pageable, 1000);
		pageInfo = listLogsPropertyMapper.mapProperty(pageInfo);

		return landLogRepos.findByTimeRangeOfLand(
			landId, startTime, endTime, pageInfo
		)
			.flatMapMany(
				slice -> Flux.fromIterable(slice)
			);
	}

	@DeleteMapping("/land/{land_id}")
	Mono<Object> delete(
		@PathVariable("land_id")
		UUID landId
	) {
		return Mono.fromSupplier(
			() -> landRepos.purge(landId)
		)
			.map(
				n -> Map.of("number_of_blocks", n)
			);
	}
}
