package guru.mikelue.misc.springframework.data.web;

import java.util.stream.Stream;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

public class ReactivePageableParamResolver
	implements SyncHandlerMethodArgumentResolver
{
	public final static String DEFAULT_PARAM_NAME_PAGE = "page";
	public final static String DEFAULT_PARAM_NAME_PAGE_SIZE = "page-size";
	public final static int DEFAULT_PARAM_VALUE_PAGE = 0;
	public final static int DEFAULT_PARAM_VALUE_PAGE_SIZE = 20;

	private int defaultPage = DEFAULT_PARAM_VALUE_PAGE;
	private int defaultPageSize = DEFAULT_PARAM_VALUE_PAGE_SIZE;

	private String paramNameOfPage = DEFAULT_PARAM_NAME_PAGE;
	private String paramNameOfPageSize = DEFAULT_PARAM_NAME_PAGE_SIZE;
	private ReactiveSortParamResolver sortResolver = new ReactiveSortParamResolver();

	public ReactivePageableParamResolver() {}

	/**
	 * Gets default value of page number.<p>
	 *
	 * @return default value of page number
	 */
	public int getDefaultPage()
	{
		return defaultPage;
	}

	/**
	 * Sets default value of page number.<p>
	 *
	 * @param newDefaultPage default value of page number
	 */
	public void setDefaultPage(int newDefaultPage)
	{
		defaultPage = newDefaultPage;
	}

	/**
	 * Gets default size of page.<p>
	 *
	 * @return default size of page
	 */
	public int getDefaultPageSize()
	{
		return defaultPageSize;
	}

	/**
	 * Sets default size of page.<p>
	 *
	 * @param newDefaultPageSize default size of page
	 */
	public void setDefaultPageSize(int newDefaultPageSize)
	{
		defaultPageSize = newDefaultPageSize;
	}

	/**
	 * Gets parameter name for page.<p>
	 *
	 * @return parameter name for page
	 */
	public String getParamNameOfPage()
	{
		return paramNameOfPage;
	}

	/**
	 * Sets parameter name for page.<p>
	 *
	 * @param newParamNameOfPage parameter name for page
	 */
	public void setParamNameOfPage(String newParamNameOfPage)
	{
		paramNameOfPage = newParamNameOfPage;
	}

	/**
	 * Gets parameter name for page size.<p>
	 *
	 * @return parameter name for page size
	 */
	public String getParamNameOfPageSize()
	{
		return paramNameOfPageSize;
	}

	/**
	 * Gets parameter resolver for {@link Sort}.<p>
	 *
	 * @return parameter resolver for {@link Sort}
	 */
	public ReactiveSortParamResolver getSortResolver()
	{
		return sortResolver;
	}

	/**
	 * Sets parameter resolver for {@link Sort}.<p>
	 *
	 * @param newSortResolver parameter resolver for {@link Sort}
	 */
	public void setSortResolver(ReactiveSortParamResolver newSortResolver)
	{
		sortResolver = newSortResolver;
	}

	/**
	 * Sets parameter name for page size.<p>
	 *
	 * @param newParamNameOfPageSize parameter name for page size
	 */
	public void setParamNameOfPageSize(String newParamNameOfPageSize)
	{
		paramNameOfPageSize = newParamNameOfPageSize;
	}

    @Override
    public boolean supportsParameter(MethodParameter param)
	{
        return param.getParameterType().equals(Pageable.class);
    }

    @Override
    public Object resolveArgumentValue(MethodParameter param, BindingContext bindCtx, ServerWebExchange webExchange)
	{
		var paramProcessor = new IntegralParamProcessor(
			webExchange.getApplicationContext()
				.getBean(ConversionService.class),
			webExchange.getRequest()
		);

		var currentDefaultPage = defaultPage;
		var currentDefaultPageSize = defaultPageSize;

		Sort sort = Sort.unsorted();

		/**
		 * Processes @PageDefault
		 */
		var pageDefaultAnno = MergedAnnotations.from(param.getParameter())
			.get(PageableDefault.class);
		if (pageDefaultAnno.isPresent()) {
			var pageDefault = pageDefaultAnno.synthesize();

			currentDefaultPage = pageDefault.page();
			currentDefaultPageSize = pageDefault.size();

			if (pageDefault.sort().length > 0) {
				var sortSyntaxProcessor = sortResolver.getSortSyntaxProcessor();
				Sort.Order[] orders = Stream.of(pageDefault.sort())
					.map(property -> sortSyntaxProcessor.propertyToOrder(
						property, Sort.DEFAULT_DIRECTION
					))
					.toArray(Sort.Order[]::new);

				sort = sort.and(Sort.by(orders));
			}
		}
		// :~)

		var page = paramProcessor.getParamOrDefault(paramNameOfPage, currentDefaultPage);
		var pageSize = paramProcessor.getParamOrDefault(paramNameOfPageSize, currentDefaultPageSize);

		/**
		 * Resolves by @SortDefault, HTTP headers, or HTTP query string
		 */
		var sortBySortDefault = (Sort)sortResolver.resolveArgumentValue(param, bindCtx, webExchange);
		sort = sort.and(sortBySortDefault);
		// :~)

        return PageRequest.of(page, pageSize, sort);
    }
}

/**
 * Retrieves parameter value from:
 *
 * <ol>
 * 	<li>HTTP header</li>
 * 	<li>HTTP query string</li>
 * </ol>
 *
 * with above priority.
 */
class IntegralParamProcessor {
	private final ConversionService conversionService;
	private final ServerHttpRequest httpRequest;

	IntegralParamProcessor(ConversionService newConversionService, ServerHttpRequest newHttpRequest)
	{
		conversionService = newConversionService;
		httpRequest = newHttpRequest;
	}

	int getParamOrDefault(String key, int defaultValue)
	{
		var resultValue = getParamOrDefault(
			httpRequest.getHeaders(),
			key, -1
		);

		if (resultValue == -1) {
			resultValue = getParamOrDefault(
				httpRequest.getQueryParams(),
				key, -1
			);
		}

		return resultValue == -1 ? defaultValue : resultValue;
	}

	private int getParamOrDefault(MultiValueMap<String, String> map, String key, int defaultValue)
	{
		if (map.containsKey(key)) {
			var integralValue = conversionService.convert(
				map.getFirst(key),
				Integer.class
			);

			if (integralValue != null) {
				return integralValue;
			}
		}

		return defaultValue;
	}
}
