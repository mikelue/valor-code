package guru.mikelue.farming.repos.cassandra;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import guru.mikelue.farming.model.LandLog;

import reactor.core.publisher.Mono;

@Repository
public interface LandLogRepos extends ReactiveCrudRepository<LandLog, LandLog.PK>, CustomizedLangLogRepos {
	Mono<Slice<LandLog>> findByPk_LandId(
		@Param("land_id") UUID landId,
		Pageable pageable
	);
}
