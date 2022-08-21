package guru.mikelue.farming.repos.jpa;

import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.junit.jupiter.api.Test;

import guru.mikelue.farming.base.AbstractJpaTestBase;
import guru.mikelue.farming.model.Block;
import guru.mikelue.farming.model.Crop;
import guru.mikelue.farming.model.RandomModels;
import guru.mikelue.farming.model.Block.BlockId;
import guru.mikelue.farming.model.Block.Status;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.*;
import static org.assertj.core.api.Assertions.assertThat;

public class BlockReposTest extends AbstractJpaTestBase {
	@Autowired
	private BlockRepos testedRepos;

	public BlockReposTest() {}

	/**
	 * Tests the modifying of block status.
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"""
				INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate)
				VALUES('cff1c7da-16c8-11ed-8120-00155debdaa8', 'Wendy', 1, 'Dry');
				""",
				"""
				INSERT INTO vc_block(bl_ld_id, bl_id, bl_status)
				VALUES
					('cff1c7da-16c8-11ed-8120-00155debdaa8', 0, 'Available');
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
	})
	void updateStatusByCheckPreviousOne()
	{
		var sampleBlockId = new BlockId(UUID.fromString("cff1c7da-16c8-11ed-8120-00155debdaa8"), (short)0);
		var sampleBlock = testedRepos.findById(sampleBlockId).get();
		sampleBlock.setUpdateTime(Instant.now());
		sampleBlock.setComment("bok choy");
		sampleBlock.setCrop(Crop.Rice);

		assertThat(
			testedRepos.updateStatusByCheckPreviousOne(
				sampleBlock, Status.ScheduledSow
			)
		)
			.isEqualTo(1);

		/**
		 * Nothing changed
		 */
		assertThat(
			testedRepos.updateStatusByCheckPreviousOne(
				sampleBlock, Status.ScheduledHarvest
			)
		)
			.isEqualTo(0);
		// :~)

		/**
		 * Assserts the changed status
		 */
		clearEntityManager();
		sampleBlock = testedRepos.getReferenceById(sampleBlockId);
		assertThat(sampleBlock)
			.extracting("status", "updateTime", "comment", "crop")
			.containsSequence(
				Status.ScheduledSow, sampleBlock.getUpdateTime(),
				sampleBlock.getComment(), sampleBlock.getCrop()
			);
		// :~)
	}

	/**
	 * Tests the adding of new land(with blocks)
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"""
				INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate)
				VALUES('3cdd8d8e-147b-11ed-8e6f-00155debdaa8', 'white mulberry', 10, 'Dry');
				""",
				"""
				INSERT INTO vc_block(bl_ld_id, bl_id, bl_status)
				VALUES
					('3cdd8d8e-147b-11ed-8e6f-00155debdaa8', 3, 'Available'),
					('3cdd8d8e-147b-11ed-8e6f-00155debdaa8', 1, 'Available'),
					('3cdd8d8e-147b-11ed-8e6f-00155debdaa8', 2, 'Occupied'),
					('3cdd8d8e-147b-11ed-8e6f-00155debdaa8', 0, 'Available');
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
	})
	void findByLandIdOrderById()
	{
		/**
		 * Asserts the blocks
		 */
		var blocks = testedRepos.findByLandIdOrderById(
			UUID.fromString("3cdd8d8e-147b-11ed-8e6f-00155debdaa8")
		);

		var expectedIds = (Object[])
			new Short[] { 0, 1, 2, 3 };

		assertThat(blocks)
			.asList()
			.hasSize(4)
			.extracting("id")
			.containsExactly(expectedIds);
		// :~)
	}

	/**
	 * Tests the finding of available blocks by land.
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate) VALUES('2d42c72c-0f1c-11ed-a77a-00155d8fd4c9', 'white mulberry', 10, 'Dry')",
				"""
				INSERT INTO vc_block(bl_ld_id, bl_id, bl_status)
				VALUES
					('2d42c72c-0f1c-11ed-a77a-00155d8fd4c9', 0, 'Available'),
					('2d42c72c-0f1c-11ed-a77a-00155d8fd4c9', 1, 'Available'),
					('2d42c72c-0f1c-11ed-a77a-00155d8fd4c9', 2, 'Occupied'),
					('2d42c72c-0f1c-11ed-a77a-00155d8fd4c9', 3, 'Available'),
					('2d42c72c-0f1c-11ed-a77a-00155d8fd4c9', 4, 'ScheduledHarvest'),
					('2d42c72c-0f1c-11ed-a77a-00155d8fd4c9', 5, 'Available'),
					('2d42c72c-0f1c-11ed-a77a-00155d8fd4c9', 6, 'Available'),
					('2d42c72c-0f1c-11ed-a77a-00155d8fd4c9', 7, 'Available');
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
	})
	void findAvailableByLandId()
	{
		var testedResult = testedRepos.findAvailableByLandId(
			UUID.fromString("2d42c72c-0f1c-11ed-a77a-00155d8fd4c9"), 4
		);

		assertThat(testedResult)
			.hasSize(4)
			.extracting("id")
			.containsExactly((short)0, (short)1,(short)3,(short)5);
	}

	/**
	 * Tests the finding of occupied blocks by land.
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate) VALUES('507aa36c-0f31-11ed-b3f9-00155d8fd4c9', 'white mulberry', 10, 'Dry')",
				"""
				INSERT INTO vc_block(bl_ld_id, bl_id, bl_status)
				VALUES
					('507aa36c-0f31-11ed-b3f9-00155d8fd4c9', 0, 'Available'),
					('507aa36c-0f31-11ed-b3f9-00155d8fd4c9', 1, 'Available'),
					('507aa36c-0f31-11ed-b3f9-00155d8fd4c9', 2, 'Occupied'),
					('507aa36c-0f31-11ed-b3f9-00155d8fd4c9', 3, 'Available'),
					('507aa36c-0f31-11ed-b3f9-00155d8fd4c9', 4, 'ScheduledHarvest'),
					('507aa36c-0f31-11ed-b3f9-00155d8fd4c9', 5, 'Occupied'),
					('507aa36c-0f31-11ed-b3f9-00155d8fd4c9', 6, 'Occupied'),
					('507aa36c-0f31-11ed-b3f9-00155d8fd4c9', 7, 'Available');
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
	})
	void findOccupiedByLandId()
	{
		var testedResult = testedRepos.findOccupiedByLandId(
			UUID.fromString("507aa36c-0f31-11ed-b3f9-00155d8fd4c9"), 4
		);

		assertThat(testedResult)
			.hasSize(3)
			.extracting("id")
			.containsExactly((short)2, (short)5,(short)6);
	}

	/**
	 * Tests the find of matured(occupied) blocks by time.
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"""
				INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate)
				VALUES
					('87874134-191e-11ed-a8ab-00155da86b6b', 'white mulberry', 10, 'Dry'),
					('ff7b4bfe-191e-11ed-b5a7-00155da86b6b', 'Archeologist', 10, 'Dry');
				INSERT INTO vc_block(bl_ld_id, bl_id, bl_mature_time, bl_status)
				VALUES
					('87874134-191e-11ed-a8ab-00155da86b6b', 0, null, 'Available'),
					('87874134-191e-11ed-a8ab-00155da86b6b', 1, null, 'ScheduledSow'),
					('ff7b4bfe-191e-11ed-b5a7-00155da86b6b', 1, '2018-05-04T10:10:32Z', 'ScheduledHarvest'),
					('87874134-191e-11ed-a8ab-00155da86b6b', 7, '2018-05-04T10:10:30Z', 'ScheduledClean'),
					('87874134-191e-11ed-a8ab-00155da86b6b', 4, '2018-05-04T10:15:07Z', 'Occupied'), /* Not matured */
					('87874134-191e-11ed-a8ab-00155da86b6b', 2, '2018-05-04T10:10:30Z', 'Occupied'),
					('ff7b4bfe-191e-11ed-b5a7-00155da86b6b', 3, '2018-05-04T10:10:32Z', 'Occupied'),
					('87874134-191e-11ed-a8ab-00155da86b6b', 6, '2018-05-04T10:10:34Z', 'Occupied');
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
	})
	void findMaturedBlocksByTime()
	{
		var peagable = PageRequest.of(0, 10);

		var sampleTime = Instant.parse("2018-05-04T10:12:00Z");
		var testedResult = testedRepos.findMaturedBlocksByTime(
			sampleTime, peagable
		);

		assertThat(testedResult)
			.hasSize(3)
			.extracting("id")
			.containsExactly((short)2, (short)3, (short)6);
	}

	/**
	 * Tests the counting by status in a land.
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"""
				INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate)
				VALUES
					('bba9ee72-19e5-11ed-9f5a-00155da861c9', 'white mulberry', 10, 'Dry');
				INSERT INTO vc_block(bl_ld_id, bl_id, bl_mature_time, bl_status)
				VALUES
					('bba9ee72-19e5-11ed-9f5a-00155da861c9', 0, null, 'Available'),
					('bba9ee72-19e5-11ed-9f5a-00155da861c9', 1, null, 'Available'),
					('bba9ee72-19e5-11ed-9f5a-00155da861c9', 2, null, 'Available'),
					('bba9ee72-19e5-11ed-9f5a-00155da861c9', 3, null, 'Available'),
					('bba9ee72-19e5-11ed-9f5a-00155da861c9', 4, '2018-05-04T10:15:07Z', 'Occupied'),
					('bba9ee72-19e5-11ed-9f5a-00155da861c9', 5, '2018-05-04T10:10:30Z', 'Occupied'),
					('bba9ee72-19e5-11ed-9f5a-00155da861c9', 6, '2018-05-04T10:10:32Z', 'Occupied'),
					('bba9ee72-19e5-11ed-9f5a-00155da861c9', 7, '2018-05-04T10:10:34Z', 'Occupied');
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
	})
	void countByLandIdAndStatus()
	{
		final var sampleLandId = UUID.fromString("bba9ee72-19e5-11ed-9f5a-00155da861c9");

		assertThat(
			testedRepos.countByLandIdAndStatus(
				sampleLandId, Status.Available, (short)20
			)
		)
			.isEqualTo((short)4);

		assertThat(
			testedRepos.countByLandIdAndStatus(
				sampleLandId, Status.Occupied, (short)2
			)
		)
			.isEqualTo((short)2);
	}

	/**
	 * Tests the updating for sowed block.
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"""
				INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate)
				VALUES
					('f2c008f0-1c52-11ed-b00f-00155da861c9', 'white mulberry', 10, 'Dry');
				INSERT INTO vc_block(bl_ld_id, bl_id, bl_mature_time, bl_status)
				VALUES
					('f2c008f0-1c52-11ed-b00f-00155da861c9', 0, null, 'ScheduledSow'),
					('f2c008f0-1c52-11ed-b00f-00155da861c9', 1, null, 'Occupied');
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
	})
	void updateToBeSowed()
	{
		var sampleBlock = RandomModels.randomBlock();
		sampleBlock.setLandId(UUID.fromString("f2c008f0-1c52-11ed-b00f-00155da861c9"));
		sampleBlock.setId((short)0);

		testedRepos.updateToBeSowed(sampleBlock);

		var testedBlock = testedRepos.getReferenceById(sampleBlock.getBlockId());
		assertThat(testedBlock)
			.hasFieldOrPropertyWithValue("status", Status.Occupied)
			.hasFieldOrPropertyWithValue("crop", sampleBlock.getCrop())
			.hasFieldOrPropertyWithValue("sowTime", sampleBlock.getSowTime())
			.hasFieldOrPropertyWithValue("matureTime", sampleBlock.getMatureTime())
			.hasFieldOrPropertyWithValue("harvestAmount", sampleBlock.getHarvestAmount())
			.hasFieldOrPropertyWithValue("updateTime", sampleBlock.getUpdateTime())
			.hasFieldOrPropertyWithValue("comment", sampleBlock.getComment());
	}

	/**
	 * Tests the updating for cleaned block.
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"""
				INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate)
				VALUES
					('690af39c-1c73-11ed-8d6e-00155da861c9', 'white mulberry', 10, 'Dry');
				INSERT INTO vc_block(bl_ld_id, bl_id, bl_crop, bl_sow_time, bl_mature_time, bl_harvest_amount, bl_status)
				VALUES
					('690af39c-1c73-11ed-8d6e-00155da861c9', 0, 'Yams', NOW(), NOW(), 3, 'Occupied'),
					('690af39c-1c73-11ed-8d6e-00155da861c9', 1, 'Yams', NOW(), NOW(), 4, 'Occupied');
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
	})
	void updateForCleaning()
	{
		var sampleBlock = RandomModels.randomBlock();
		sampleBlock.setLandId(UUID.fromString("690af39c-1c73-11ed-8d6e-00155da861c9"));
		sampleBlock.setId((short)0);

		testedRepos.updateForCleaning(sampleBlock);

		/**
		 * Asserts the cleaned block
		 */
		var testedBlock = testedRepos.getReferenceById(sampleBlock.getBlockId());
		assertThat(testedBlock)
			.hasFieldOrPropertyWithValue("status", Status.Available)
			.hasFieldOrPropertyWithValue("crop", null)
			.hasFieldOrPropertyWithValue("sowTime", null)
			.hasFieldOrPropertyWithValue("matureTime", null)
			.hasFieldOrPropertyWithValue("harvestAmount", null)
			.hasFieldOrPropertyWithValue("updateTime", sampleBlock.getUpdateTime())
			.hasFieldOrPropertyWithValue("comment", sampleBlock.getComment());
		// :~)

		/**
		 * Asserts other(unchanged) blocks
		 */
		testedBlock = testedRepos.getReferenceById(
			new BlockId(sampleBlock.getLandId(), (short)1)
		);

		assertThat(testedBlock)
			.hasFieldOrPropertyWithValue("status", Status.Occupied)
			.doesNotReturn(null, Block::getCrop)
			.doesNotReturn(null, Block::getSowTime)
			.doesNotReturn(null, Block::getMatureTime)
			.doesNotReturn(null, Block::getHarvestAmount);
		// :~)
	}

	/**
	 * Tests the updating for cleaned block.
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"""
				INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate)
				VALUES
					('d1eccae2-1d3c-11ed-a9e0-00155da861c9', 'white mulberry', 10, 'Dry');
				INSERT INTO vc_block(bl_ld_id, bl_id, bl_crop, bl_sow_time, bl_mature_time, bl_harvest_amount, bl_status, bl_update_time)
				VALUES
					('d1eccae2-1d3c-11ed-a9e0-00155da861c9', 2, 'Yams', NOW(), NOW(), 3, 'ScheduledSow', NOW() - INTERVAL '30 Minute'),
					('d1eccae2-1d3c-11ed-a9e0-00155da861c9', 1, 'Yams', NOW(), NOW(), 4, 'ScheduledHarvest', NOW() - INTERVAL '29 Minute'),
					('d1eccae2-1d3c-11ed-a9e0-00155da861c9', 0, 'Yams', NOW(), NOW(), 4, 'ScheduledClean', NOW() - INTERVAL '28 Minute'),
					('d1eccae2-1d3c-11ed-a9e0-00155da861c9', 3, 'Yams', NOW(), NOW(), 4, 'Occupied', NOW() - INTERVAL '30 Minute'),
					('d1eccae2-1d3c-11ed-a9e0-00155da861c9', 4, 'Yams', NOW(), NOW(), 4, 'ScheduledSow', NOW() - INTERVAL '5 Minute'),
					('d1eccae2-1d3c-11ed-a9e0-00155da861c9', 5, 'Yams', NOW(), NOW(), 4, 'ScheduledSow', NOW() - INTERVAL '5 Minute');
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
	})
	void findOldScheduledActivities()
	{
		var pageable = PageRequest.of(0, 10);

		var testedResult = testedRepos.findOldScheduledActivities(
			Instant.now().minusSeconds(1200),
			pageable
		);

		assertThat(testedResult.getContent())
			.hasSize(3)
			.extracting("id")
			.containsExactly((short) 2, (short) 1, (short) 0);
	}
}
