package guru.mikelue.farming.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.RandomUtils;

import static guru.mikelue.farming.model.Crop.*;

/**
 * Randomized generator for farming properties over various crops.
 */
public class CropProperties {
	private final static Map<Crop, Range> SowingTime;
	private final static Map<Crop, Range> GrowingTime;
	private final static Map<Crop, Range> HarvestingTime;
	private final static Map<Crop, Range> HarvestingQuantity;

	static {
		SowingTime = initSowingTime();
		GrowingTime = initGrowingTime();
		HarvestingTime = initHarvestingTime();
		HarvestingQuantity = initHarvestingQuantity();
	}

	private CropProperties() {}

	/**
	 * Generates(randomly) sowing time for crops.
	 */
	public final static int getSowingTime(Crop crop)
	{
		return SowingTime.get(crop).randomValue();
	}

	/**
	 * Generates(randomly) growing time for crops.
	 */
	public final static int getGrowingTime(Crop crop)
	{
		return GrowingTime.get(crop).randomValue();
	}

	/**
	 * Generates(randomly) harvesting time for crops.
	 */
	public final static int getHarvestingTime(Crop crop)
	{
		return HarvestingTime.get(crop).randomValue();
	}

	/**
	 * Generates(randomly) quantity of harvesting for crops.
	 */
	public final static short getHarvestingQuanity(Crop crop)
	{
		return (short)HarvestingQuantity.get(crop).randomValue();
	}

	private final static Map<Crop, Range> initGrowingTime()
	{
		var workMap = new HashMap<Crop, Range>(16);

		workMap.put(Manioc, new Range(15, 20));
		workMap.put(Rice, new Range(17, 23));
		workMap.put(Yams, new Range(15, 25));
		workMap.put(Grape, new Range(10, 20));
		workMap.put(Tomato, new Range(10, 20));
		workMap.put(Pumpkin, new Range(10, 15));
		workMap.put(Kale, new Range(10, 20));
		workMap.put(Spinach, new Range(10, 25));
		workMap.put(Lettuce, new Range(5, 12));

		return Collections.unmodifiableMap(workMap);
	}

	private final static Map<Crop, Range> initSowingTime()
	{
		var workMap = new HashMap<Crop, Range>(16);

		workMap.put(Manioc, new Range(2, 5));
		workMap.put(Rice, new Range(2, 5));
		workMap.put(Yams, new Range(2, 5));
		workMap.put(Grape, new Range(3, 8));
		workMap.put(Tomato, new Range(3, 8));
		workMap.put(Pumpkin, new Range(3, 8));
		workMap.put(Kale, new Range(4, 10));
		workMap.put(Spinach, new Range(4, 10));
		workMap.put(Lettuce, new Range(4, 10));

		return Collections.unmodifiableMap(workMap);
	}

	private final static Map<Crop, Range> initHarvestingTime()
	{
		var workMap = new HashMap<Crop, Range>(16);

		workMap.put(Manioc, new Range(2, 5));
		workMap.put(Rice, new Range(2, 5));
		workMap.put(Yams, new Range(2, 5));
		workMap.put(Grape, new Range(3, 7));
		workMap.put(Tomato, new Range(3, 7));
		workMap.put(Pumpkin, new Range(3, 7));
		workMap.put(Kale, new Range(2, 5));
		workMap.put(Spinach, new Range(5, 10));
		workMap.put(Lettuce, new Range(5, 10));

		return Collections.unmodifiableMap(workMap);
	}

	private final static Map<Crop, Range> initHarvestingQuantity()
	{
		var workMap = new HashMap<Crop, Range>(16);

		workMap.put(Manioc, new Range(5, 10));
		workMap.put(Rice, new Range(10, 20));
		workMap.put(Yams, new Range(10, 15));
		workMap.put(Grape, new Range(5, 15));
		workMap.put(Tomato, new Range(15, 30));
		workMap.put(Pumpkin, new Range(3, 8));
		workMap.put(Kale, new Range(5, 12));
		workMap.put(Spinach, new Range(10, 30));
		workMap.put(Lettuce, new Range(10, 20));

		return Collections.unmodifiableMap(workMap);
	}
}

record Range(int min, int max) {
	Range(int min, int max) {
		this.min = min;
		this.max = max + 1;
	}

	int randomValue()
	{
		return RandomUtils.nextInt(
			min, max
		);
	}
}
