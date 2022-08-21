package guru.mikelue.misc.springframework.data.web;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.SortDefault;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This resolver would resolve sorting properties by following priority:
 *
 * <ol>
 * 	<li>HTTP header of {@link ReactiveSortParamResolver#getSortParameterName}</li>
 * 	<li>HTTP query string of {@link ReactiveSortParamResolver#getSortParameterName}</li>
 * 	<li>Use value {@link SortDefault}</li>
 * 	<li>Use value of {@link ReactiveSortParamResolver#setDefaultFallback()}</li>
 * </ol>
 *
 * The default parameter name is {@value #DEFAULT_PARAM_NAME_PAGE_SORT}.
 *
 * @see ReactivePageableParamResolver
 */
public class ReactiveSortParamResolver
	implements SyncHandlerMethodArgumentResolver
{
	private final static Logger logger = LoggerFactory.getLogger(ReactiveSortParamResolver.class);

	public final static String DEFAULT_PARAM_NAME_PAGE_SORT = "page-sort";
	public final static String DEFAULT_PROP_DELIMITER = ",";
	public final static String DEFAULT_SORT_DELIMITER = ":";

	private Sort defaultFallback = Sort.unsorted();
	private String sortParameterName = DEFAULT_PARAM_NAME_PAGE_SORT;
	private SortSyntaxProcessor syntaxProcessor = new SortSyntaxProcessor(
		DEFAULT_PROP_DELIMITER, DEFAULT_SORT_DELIMITER,
		Sort.unsorted()
	);

	public ReactiveSortParamResolver() {}

	/**
	 * Gets parameter name of sort properties.<p>
	 *
	 * @return parameter name of sort properties
	 */
	public String getSortParameterName()
	{
		return sortParameterName;
	}

	/**
	 * Sets parameter name of sort properties.<p>
	 *
	 * @param newSortParameterName parameter name of sort properties
	 */
	public void setSortParameterName(String newSortParameterName)
	{
		sortParameterName = newSortParameterName;
	}

	/**
	 * Gets delimiter between properties for sort string.<p>
	 *
	 * @return delimiter between properties for sort string
	 */
	public String getPropDelimiter()
	{
		return syntaxProcessor.getPropDelimiter();
	}

	/**
	 * Sets delimiter between properties for sort string.<p>
	 *
	 * @param newPropDelimiter delimiter between properties for sort string
	 */
	public void setPropDelimiter(String newPropDelimiter)
	{
		syntaxProcessor = new SortSyntaxProcessor(
			newPropDelimiter,
			syntaxProcessor.getSortDelimiter(),
			syntaxProcessor.getDefaultFallback()
		);
	}

	/**
	 * Gets sort delimiter, which indicates sorting detail liking direction.<p>
	 *
	 * @return sort delimiter, which indicates sorting detail liking direction
	 */
	public String getSortDelimiter()
	{
		return syntaxProcessor.getSortDelimiter();
	}

	/**
	 * Sets sort delimiter, which indicates sorting detail liking direction.<p>
	 *
	 * @param newSortDelimiter sort delimiter, which indicates sorting detail liking direction
	 */
	public void setSortDelimiter(String newSortDelimiter)
	{
		syntaxProcessor = new SortSyntaxProcessor(
			syntaxProcessor.getPropDelimiter(),
			newSortDelimiter,
			syntaxProcessor.getDefaultFallback()
		);
	}

	/**
	 * Gets fallback of sorting properties.<p>
	 *
	 * @return fallback of sorting properties
	 */
	public Sort getDefaultFallback()
	{
		return syntaxProcessor.getDefaultFallback();
	}

	/**
	 * Sets fallback of sorting properties.<p>
	 *
	 * @param newDefaultFallback fallback of sorting properties
	 */
	public void setDefaultFallback(Sort newDefaultFallback)
	{
		syntaxProcessor = new SortSyntaxProcessor(
			syntaxProcessor.getPropDelimiter(),
			syntaxProcessor.getSortDelimiter(),
			newDefaultFallback
		);
	}

	public SortSyntaxProcessor getSortSyntaxProcessor()
	{
		return syntaxProcessor;
	}

    @Override
    public boolean supportsParameter(MethodParameter param)
	{
		return Sort.class.equals(param.getParameterType());
    }

    @Override
    public Object resolveArgumentValue(MethodParameter param, BindingContext bindCtx, ServerWebExchange webExchange)
	{
		var httpRequest = webExchange.getRequest();

		var sort = defaultFallback;
		var defaultDirection = Sort.DEFAULT_DIRECTION;

		/**
		 * Loads properties from @SortDefault
		 */
		var annoSortDefault = MergedAnnotations.from(param.getParameter())
			.get(SortDefault.class);

		if (annoSortDefault.isPresent()) {
			var annoValue = annoSortDefault.synthesize();

			var orders = Stream.of(annoValue.sort())
				.map(sortValue -> syntaxProcessor.propertyToOrder(sortValue, annoValue.direction()))
				.collect(Collectors.toList());

			sort = sort.and(Sort.by(orders));
			defaultDirection = annoValue.direction();
		}
		// :~)

		/**
		 * Loads properties from HTTP header
		 */
		var headers = httpRequest.getHeaders();
		if (headers.containsKey(sortParameterName)) {
			var value = headers.getFirst(sortParameterName);

			logger.debug("HTTP header[{}] has value: [{}]", sortParameterName, value);

			return syntaxProcessor.buildSort(value, defaultDirection);
		}
		// :~)

		/**
		 * Loads properties from HTTP query string
		 */
		var queryParams = httpRequest.getQueryParams();
		if (queryParams.containsKey(sortParameterName)) {
			var value = queryParams.getFirst(sortParameterName);

			logger.debug("HTTP query string[{}] has value: [{}]", sortParameterName, value);

			return syntaxProcessor.buildSort(value, defaultDirection);
		}
		// :~)

        return sort;
    }
}

class SortSyntaxProcessor {
	private final String sortDelimiter;
	private final String propertyDelimiter;
	private final Sort defaultFallback;

	private final Logger logger = LoggerFactory.getLogger(ReactiveSortParamResolver.class);

	SortSyntaxProcessor(String propertyDelimiter, String sortDelimiter, Sort defaultFallback)
	{
		logger.debug(
			"PropertyDelimiter: \"{}\",  SortDelimiter: \"{}\".",
			propertyDelimiter, sortDelimiter
		);

		this.propertyDelimiter = propertyDelimiter;
		this.sortDelimiter = sortDelimiter;
		this.defaultFallback = defaultFallback;
	}

	Sort buildSort(String sortProperties, Direction defaultDirection)
	{
		var orders = Stream.of(
			sortProperties.split(propertyDelimiter)
		)
			.map(StringUtils::trimToNull)
			.filter(v -> v != null)
			.map(sortValue -> propertyToOrder(sortValue, defaultDirection))
			.collect(Collectors.toList());

		return orders.size() > 0 ? Sort.by(orders) : defaultFallback;
	}

	Order propertyToOrder(String property, Direction defaultDirection)
	{
		var propertyName = property;
		var directionText = "";

		var sortDelimiterIndex = property.indexOf(sortDelimiter);
		if (sortDelimiterIndex > 0) {
			propertyName = property.substring(0, sortDelimiterIndex);
			directionText = property.substring(sortDelimiterIndex + 1);
		}

		Direction direction = defaultDirection;

		switch (directionText.toLowerCase()) {
			case "asc":
				direction = Direction.ASC;
				break;
			case "desc":
				direction = Direction.DESC;
				break;
		}

		return new Sort.Order(direction, propertyName);
	}

	/**
	 * Gets delimiter between properties for sort string.<p>
	 *
	 * @return delimiter between properties for sort string
	 */
	public String getPropDelimiter()
	{
		return propertyDelimiter;
	}

	/**
	 * Gets sort delimiter, which indicates sorting detail liking direction.<p>
	 *
	 * @return sort delimiter, which indicates sorting detail liking direction
	 */
	public String getSortDelimiter()
	{
		return sortDelimiter;
	}

	/**
	 * Gets fallback of sorting properties.<p>
	 *
	 * @return fallback of sorting properties
	 */
	public Sort getDefaultFallback()
	{
		return defaultFallback;
	}
}
