package guru.mikelue.farming.web;

import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.web.reactive.function.server.ServerRequest;

public class GlobalErrorAttributes extends DefaultErrorAttributes {
	@Override
	public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options)
	{
		var defaultAttributes = super.getErrorAttributes(request, options);

		var sourceThrowable = getError(request);

		if (CodeAndDetailException.class.isInstance(sourceThrowable)) {
			var sourceCodeAndDetailException = (CodeAndDetailException)sourceThrowable;

			var codeAndDetail = sourceCodeAndDetailException.getCodeAndDetail();
			defaultAttributes.put("code", codeAndDetail.getCode());

			if (codeAndDetail.getDetail() != null) {
				defaultAttributes.put("detail", codeAndDetail.getDetail());
			}
		}

		return defaultAttributes;
	}
}
