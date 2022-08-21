package guru.mikelue.farming.web;

import java.util.Optional;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;

public interface ExceptionalResponse {
	public static <T> Mono<T> notFound(String message)
	{
		return Mono.error(
			() -> new ResponseStatusException(HttpStatus.NOT_FOUND, message)
		);
	}

	public static <T> Mono<T> notFound(String format, Object... args)
	{
		return Mono.error(
			() -> new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				String.format(format, args)
			)
		);
	}

	/**
	 * Builds a function for converting {@link DataIntegrityViolationException} to integral code.
	 *
	 * This function is intended to be used with {@link Mono#onErrorMap}.
	 */
	public static
		Function<DataIntegrityViolationException, ResponseStatusException> buildMapForDataIntegrityViolation(
			Map<String, Integer> messageToCodeMap
		) {
		final var messageToCode = Collections.unmodifiableMap(messageToCodeMap);

		return e -> {
			for (var message: messageToCode.keySet()) {
				if (e.getMessage().contains(message)) {
					return CodeAndDetailException.build(
						HttpStatus.CONFLICT,
						messageToCode.get(message)
					);
				}
			}

			return CodeAndDetailException.build(
				HttpStatus.CONFLICT, -1
			);
		};
	}

	public static <T, O extends Optional<T>> Function<Mono<O>, Publisher<T>> transformNotFoundResponse(
		String format, Object... arguments
	) {
		return sourceMono -> sourceMono
			.filter(Optional::isPresent)
			.map(Optional::get)
			.switchIfEmpty(ExceptionalResponse.notFound(
				format, arguments
			));
	}
}
