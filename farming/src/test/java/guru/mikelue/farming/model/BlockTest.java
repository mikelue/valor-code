package guru.mikelue.farming.model;

import org.junit.jupiter.api.*;

import guru.mikelue.farming.base.AbstractJsonTestBase;

import java.time.Instant;

public class BlockTest extends AbstractJsonTestBase {
	public BlockTest() {}

	/**
	 * Tests the JSON serialization of block.
	 */
	@Test
	void serialization()
	{
		var now = Instant.now();
		var nowAsEpochSeconds = now.getEpochSecond();

		var sampleBlock = new Block();
		sampleBlock.setId((short)7);
		sampleBlock.setCrop(Crop.Spinach);
		sampleBlock.setSowTime(now);
		sampleBlock.setMatureTime(now);
		sampleBlock.setHarvestAmount((short)23);
		sampleBlock.setStatus(Block.Status.ScheduledClean);
		sampleBlock.setComment("hydrant penguin");
		sampleBlock.setUpdateTime(now);

		getLogger().info("JSON of Block: {}", valueToTree(sampleBlock));

		assertAsJsonContent(sampleBlock)
			.hasJsonPathValue("[?(@.id == %d)]", sampleBlock.getId())
			.hasJsonPathValue("[?(@.crop == %d)]", sampleBlock.getCrop().value())
			.hasJsonPathValue("[?(@.sow_time == %d)]", nowAsEpochSeconds)
			.hasJsonPathValue("[?(@.mature_time == %d)]", nowAsEpochSeconds)
			.hasJsonPathValue("[?(@.harvest_amount == %d)]", sampleBlock.getHarvestAmount())
			.hasJsonPathValue("[?(@.status == %d)]", sampleBlock.getStatus().value())
			.hasJsonPathValue("[?(@.comment == '%s')]", sampleBlock.getComment())
			.hasJsonPathValue("[?(@.update_time == %d)]", nowAsEpochSeconds);
	}
}
