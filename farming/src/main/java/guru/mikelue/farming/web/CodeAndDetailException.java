package guru.mikelue.farming.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CodeAndDetailException extends ResponseStatusException {
	private final CodeAndDetail detail;

	/**
	 * Builds {@link CodeAndDetailException} with only code value.
	 */
	public static CodeAndDetailException build(HttpStatus httpStatus, int code)
	{
		return new CodeAndDetailException(
			httpStatus, new CodeAndDetail(code)
		);
	}

	public CodeAndDetailException(
		HttpStatus httpStatus, CodeAndDetail newDetail
	) {
		super(httpStatus);
		detail = newDetail;
	}

	public CodeAndDetailException(
		HttpStatus httpStatus, CodeAndDetail newDetail, Throwable cause
	) {
		super(httpStatus, cause.getMessage(), cause);

		detail = newDetail;
	}

	public CodeAndDetail getCodeAndDetail()
	{
		return detail;
	}
}
