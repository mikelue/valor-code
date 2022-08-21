package guru.mikelue.misc.springframework.data.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public interface PageableUtils {
	/**
	 * Limits the number and size of page
	 */
	public static Pageable LimitPageable(Pageable pageable, int maxNumber, int maxSize)
	{
		int finalPageNumber = pageable.getPageNumber();
		int finalPageSize = pageable.getPageSize();

		if (finalPageNumber <= maxNumber && finalPageSize <= maxSize) {
			return pageable;
		}

		finalPageNumber = finalPageNumber <= maxNumber ?
			finalPageNumber : maxNumber;
		finalPageSize = finalPageSize <= maxSize ?
			finalPageSize : maxSize;

		return PageRequest.of(
			finalPageNumber, finalPageSize,
			pageable.getSort()
		);
	}

	/**
	 * Limits the size of page
	 */
	public static Pageable LimitNumberOfPageable(Pageable pageable, int max)
	{
		return LimitPageable(
			pageable, max, pageable.getPageSize()
		);
	}

	/**
	 * Limits the number of page
	 */
	public static Pageable LimitSizeOfPage(Pageable pageable, int max)
	{
		return LimitPageable(
			pageable, pageable.getPageNumber(), max
		);
	}
}
