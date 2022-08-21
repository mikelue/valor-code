package guru.mikelue.misc.springframework.data.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

/**
 * This class gives method to change property of {@link Sort}, {@link Order}, or {@link Pageable}.
 *
 */
public class PagePropertyChanger {
	/**
	 * Builds a changer for mapping name of property to another one.
	 */
	public static PagePropertyChanger from(Map<String, String> propertyMapping)
	{
		var newChanger = new PagePropertyChanger();
		newChanger.propertyMapper = Collections.unmodifiableMap(propertyMapping);

		return newChanger;
	}

	private Map<String, String> propertyMapper;

	private PagePropertyChanger() {}

	public Pageable mapProperty(Pageable pageable)
	{
		return PageRequest.of(
			pageable.getPageNumber(),
			pageable.getPageSize(),
			mapProperty(pageable.getSort())
		);
	}

	public Sort mapProperty(Sort sort)
	{
		return Sort.by(mapProperty(sort.toList()));
	}

	public Order mapProperty(Order sourceOrder)
	{
		var newOrder = sourceOrder;

		if (propertyMapper.containsKey(sourceOrder.getProperty())) {
			newOrder = new Order(
				sourceOrder.getDirection(),
				propertyMapper.get(sourceOrder.getProperty()),
				sourceOrder.getNullHandling()
			);
		}

		return newOrder;
	}

	public List<Order> mapProperty(List<Order> orders)
	{
		var newOrders = new ArrayList<Order>(orders.size());

		for (var sourceOrder: orders) {
			newOrders.add(mapProperty(sourceOrder));
		}

		return newOrders;
	}
}
