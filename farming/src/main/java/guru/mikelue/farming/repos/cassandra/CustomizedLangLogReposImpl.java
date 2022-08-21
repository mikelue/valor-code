package guru.mikelue.farming.repos.cassandra;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.data.cassandra.core.query.Criteria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import guru.mikelue.farming.model.LandLog;

import reactor.core.publisher.Mono;

public class CustomizedLangLogReposImpl implements CustomizedLangLogRepos {
	@Autowired
	private ReactiveCassandraTemplate cassandraTemplate;

	public CustomizedLangLogReposImpl() {}

    @Override
    public Mono<Slice<LandLog>> findByTimeRangeOfLand(
		UUID landId, Instant startTime, Instant endTime,
		Pageable pageable
	) {
		var query = Query.query(
			Criteria.where("pk.landId")
				.is(landId)
		);

		if (startTime != null) {
			query = query.and(
				Criteria.where("pk.time")
					.gte(startTime)
			);
		}

		if (endTime != null) {
			query = query.and(
				Criteria.where("pk.time")
					.lte(endTime)
			);
		}

        return cassandraTemplate.slice(
			query.pageRequest(pageable),
			LandLog.class
		);
    }
}
