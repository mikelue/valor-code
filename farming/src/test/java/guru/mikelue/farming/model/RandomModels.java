package guru.mikelue.farming.model;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.RandomUtils;
import com.github.javafaker.Faker;
import guru.mikelue.farming.model.Block.Status;

public final class RandomModels {
	private final static Faker faker = new Faker();

	private RandomModels() {}

	private final static LogActivity[] activities = LogActivity.values();
	public static List<LandLog> randomLandLogs(int size, Instant startTime, Duration timeElapse)
	{
		var result = new ArrayList<LandLog>(size);

		for (var i = 0; i < size; i++) {
			var sampleBlock = new Block();
			sampleBlock.setLandId(UUID.randomUUID());
			sampleBlock.setId((short)i);
			sampleBlock.setCrop(Crop.Lettuce);
			sampleBlock.setSowTime(startTime.minusSeconds(10));
			sampleBlock.setMatureTime(startTime.minusSeconds(5));
			sampleBlock.setStatus(Block.Status.Occupied);
			sampleBlock.setHarvestAmount((short)(i + 11));
			sampleBlock.setUpdateTime(startTime);
			sampleBlock.setComment(faker.beer().style());

			var newLog = LandLog.from(sampleBlock);
			newLog.setTime(startTime);
			newLog.setUsedTimeSecond((short)(7 + i));
			newLog.setActivity(activities[
				RandomUtils.nextInt(0, activities.length)
			]);

			result.add(newLog);

			startTime = startTime.plusSeconds(timeElapse.toSeconds());
		}

		return result;
	}
	public static List<LandLog> randomLandLogs(int size)
	{
		return randomLandLogs(
			size,
			Instant.now().truncatedTo(ChronoUnit.SECONDS),
			Duration.ofSeconds(10)
		);
	}

	/**
	 * Builds randomized block, which has:
	 *
	 * <ul>
	 * 	<li>Random sow time from past 30 ~ 20 minutes</li>
	 * 	<li>Random mature time from past 20 ~ 10 minutes</li>
	 * 	<li>Random update time from past 5 ~ 2 minutes</li>
	 * </ul>
	 */
	public static Block randomBlock()
	{
		var newBlock = new Block();
		newBlock.setLandId(UUID.randomUUID());
		newBlock.setId((short)faker.number().numberBetween(0, 30000));
		newBlock.setCrop(
			randomEnum(Crop.class)
		);
		newBlock.setSowTime(
			faker.date().past(30, 20, MINUTES)
				.toInstant()
		);
		newBlock.setMatureTime(
			faker.date().past(20, 10, MINUTES)
				.toInstant()
		);
		newBlock.setHarvestAmount((short)faker.number().numberBetween(0, 10000));
		newBlock.setStatus(
			randomEnum(Status.class)
		);
		newBlock.setComment(faker.book().publisher());
		newBlock.setUpdateTime(
			faker.date().past(5, 2, MINUTES)
				.toInstant()
		);

		return newBlock;
	}

	private static <T extends Enum<?>> T randomEnum(
		Class<T> typeOfEnum
	) {
		var enumConstants = typeOfEnum.getEnumConstants();
		var randomIndexOfEnum = faker.number().numberBetween(
			0, enumConstants.length - 1
		);

		return enumConstants[randomIndexOfEnum];
	}
}
