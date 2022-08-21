package guru.mikelue.farming.rest;

import org.springframework.data.domain.Pageable;
import guru.mikelue.misc.springframework.data.web.PageableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;

public abstract class AbstractControllerBase {
	public final static int MAX_PAGE_SIZE = 1000;
	private final Logger logger = LoggerFactory.getLogger(AbstractControllerBase.class);

	protected AbstractControllerBase() {}

	public Logger getLogger()
	{
		return logger;
	}

	protected Mono<Pageable> safePageable(Pageable pageable)
	{
		return Mono.just(pageable)
			.map(p -> PageableUtils.LimitSizeOfPage(p, MAX_PAGE_SIZE));
	}
}
