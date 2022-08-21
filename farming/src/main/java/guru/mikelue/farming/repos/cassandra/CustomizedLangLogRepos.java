package guru.mikelue.farming.repos.cassandra;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import guru.mikelue.farming.model.LandLog;

import reactor.core.publisher.Mono;

public interface CustomizedLangLogRepos {
	Mono<Slice<LandLog>> findByTimeRangeOfLand(
		UUID landId,
		Instant startTime, Instant endTime,
		Pageable pageable
	);
}
