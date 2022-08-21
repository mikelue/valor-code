package guru.mikelue.farming.repos.jpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.Test;

import guru.mikelue.farming.base.AbstractJpaTestBase;
import guru.mikelue.farming.model.Block;
import guru.mikelue.farming.model.Climate;
import guru.mikelue.farming.model.Land;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.*;
import static org.springframework.transaction.annotation.Propagation.NEVER;

import java.util.UUID;

public class LandReposTest extends AbstractJpaTestBase {
	@Autowired
	private LandRepos testedRepos;
	@Autowired
	private BlockRepos blockRepos;

	public LandReposTest() {}

	/**
	 * Tests the deletion of land(with blocks)
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"""
				INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate)
				VALUES
					('116c87f6-2103-11ed-891e-00155da861c9', 'white mulberry', 10, 'Mild');
				""",
				"""
				INSERT INTO vc_block(bl_ld_id, bl_id, bl_status)
				VALUES
					('116c87f6-2103-11ed-891e-00155da861c9', 3, 'Available'),
					('116c87f6-2103-11ed-891e-00155da861c9', 1, 'Available'),
					('116c87f6-2103-11ed-891e-00155da861c9', 2, 'Available'),
					('116c87f6-2103-11ed-891e-00155da861c9', 0, 'Available');
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
	})
	void purge()
	{
		var sampleLandId = UUID.fromString("116c87f6-2103-11ed-891e-00155da861c9");

		var deletedRows = testedRepos.purge(sampleLandId);

		/**
		 * Asserts the deletion of data
		 */
		assertThat(deletedRows)
			.isEqualTo(4);
		assertThat(testedRepos.existsById(sampleLandId))
			.isFalse();
		// :~)
	}

	/**
	 * Tests the adding of new land(with blocks)
	 */
	@Test
	@Transactional(propagation=NEVER)
	@SqlGroup({
		@Sql(
			statements={
				"""
				DELETE FROM vc_block
				WHERE bl_ld_id = (
					SELECT ld_id FROM vc_land
					WHERE ld_name = 'panda'
				);
				DELETE FROM vc_land
				WHERE ld_name = 'panda';
				"""
			},
			executionPhase=AFTER_TEST_METHOD
		),
	})
	void add()
	{
		var newLand = new Land();
		newLand.setName("panda");
		newLand.setSize((short)(CustomizedLandReposImpl.BATCH_SIZE + 77));
		newLand.setClimate(Climate.Polar);
		testedRepos.addNewWithBlocks(newLand);

		/**
		 * Asserts the blocks
		 */
		var blocks = blockRepos.findByLandIdOrderById(newLand.getId());

		assertThat(blocks).hasSize(newLand.getSize());

		assertThat(blocks.get(5).getStatus()).isEqualTo(Block.Status.Available);
		// :~)
	}

	/**
	 * Tests the modification of name.
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"INSERT INTO vc_land(ld_name, ld_size, ld_climate) VALUES('cloveg', 20, 'Dry')"
			},
			executionPhase=BEFORE_TEST_METHOD
		),
	})
	void changeName()
	{
		var targetLand = testedRepos.findLandByName("cloveg");
		clearEntityManager();

		final var sampleName = "Great";
		targetLand.setName(sampleName);
		testedRepos.save(targetLand);

		targetLand = testedRepos.findLandByName(sampleName);
		assertThat(targetLand.getName()).isEqualTo(sampleName);
	}

	/**
	 * Tests listing of lands(paging).
	 */
	@Test
	@SqlGroup({
		@Sql(
			statements={
				"""
				INSERT INTO vc_land(ld_id, ld_name, ld_size, ld_climate)
				VALUES
					('2d42c72c-0f1c-11ed-a77a-00155d8fd4c9', 'white mulberry', 10, 'Mild'),
					('51c535fa-1145-11ed-9fee-00155d8fd4c9', 'white Farmer', 20, 'Dry'),
					('54fad7e8-1145-11ed-9255-00155d8fd4c9', 'white Curaçao', 11, 'Mild'),
					('58982338-1145-11ed-86af-00155d8fd4c9', 'white writing', 21, 'Dry'),
					('5c021a74-1145-11ed-b10a-00155d8fd4c9', 'white giraffe', 31, 'Continental'),
					('5e6d006c-1145-11ed-8635-00155d8fd4c9', 'white Archeologist', 41, 'Dry'),
					('65d90652-1145-11ed-b223-00155d8fd4c9', 'white koala', 51, 'Tropical');
				"""
			},
			executionPhase=BEFORE_TEST_METHOD
		),
	})
	void findAll()
	{
		var testedResult = testedRepos.findAll(
			PageRequest.of(1, 3)
		);

		assertThat(testedResult).hasSize(3);
	}
}
