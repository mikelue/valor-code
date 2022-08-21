package guru.mikelue.farming.repos.jpa;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import guru.mikelue.farming.model.Block;
import guru.mikelue.farming.model.Block.Status;

public interface BlockRepos extends JpaRepository<Block, Block.BlockId>, CustomizedBlockRepos {
	/**
	 * Finds blocks stay in scheduled activity before certain time.
	 */
	@Query(
		"""
		SELECT b
		FROM Block b
		WHERE b.updateTime <= :checked_time
			AND b.status IN ('ScheduledSow', 'ScheduledHarvest', 'ScheduledClean')
		ORDER BY b.updateTime ASC
		"""
	)
	Slice<Block> findOldScheduledActivities(
		@Param("checked_time") Instant checkedTime,
		Pageable pageable
	);

	List<Block> findByLandIdOrderById(UUID landId);

	/**
	 * Updates the block to new status with checking of previous one.
	 *
	 * @return Gives 0 if the update is not effective
	 */
	@Transactional
	@Modifying
	@Query(
		"""
		UPDATE Block b
		SET b.status = :new_status,
			b.updateTime = :#{#source_block.updateTime},
			b.crop = :#{#source_block.crop},
			b.comment = :#{#source_block.comment}
		WHERE b.landId = :#{#source_block.landId}
			AND b.id = :#{#source_block.id}
			AND b.status = :#{#source_block.status}
		"""
	)
	int updateStatusByCheckPreviousOne(
		@Param("source_block") Block block,
		@Param("new_status") Status status
	);

	/**
	 * Updates the block to "occupied" with crop and maturing information.
	 *
	 * @return Gives 0 if the update is not effective
	 */
	@Transactional
	@Modifying
	@Query(
		"""
		UPDATE Block b
		SET b.status = 'Occupied',
			b.crop = :#{#block.crop},
			b.sowTime = :#{#block.sowTime},
			b.matureTime = :#{#block.matureTime},
			b.harvestAmount = :#{#block.harvestAmount},
			b.updateTime = :#{#block.updateTime},
			b.comment = :#{#block.comment}
		WHERE b.landId = :#{#block.landId}
			AND b.id = :#{#block.id}
			AND b.status = 'ScheduledSow'
		"""
	)
	int updateToBeSowed(
		@Param("block") Block block
	);

	/**
	 * Updates the block to "available" for cleaning action.
	 *
	 * @return Gives 0 if the update is not effective
	 */
	@Transactional
	@Modifying
	@Query(
		"""
		UPDATE Block b
		SET b.status = 'Available',
			b.crop = null,
			b.sowTime = null,
			b.matureTime = null,
			b.harvestAmount = null,
			b.updateTime = :#{#block.updateTime},
			b.comment = :#{#block.comment}
		WHERE b.landId = :#{#block.landId}
			AND b.id = :#{#block.id}
		"""
	)
	int updateForCleaning(
		@Param("block") Block block
	);

	/**
	 * Finds blocks which are ready to be harvested.
	 *
	 * @return Gives 0 if the update is not effective
	 */
	@Query(
		"""
		SELECT b
		FROM Block b
		WHERE b.status = 'Occupied'
			AND b.matureTime <= :checked_time
		ORDER BY b.matureTime ASC
		"""
	)
	Slice<Block> findMaturedBlocksByTime(
		@Param("checked_time")
		Instant checkedTime,
		Pageable page
	);

	@Query(
		value="""
		SELECT COUNT(*)
		FROM (
			SELECT bl_ld_id, bl_id
			FROM vc_block
			WHERE bl_ld_id = :land_id
				AND bl_status = :#{#status.toString()}\\:\\:enum_block_status
			LIMIT :limit
		) AS b
		""",
		nativeQuery=true
	)
	short countByLandIdAndStatus(
		@Param("land_id") UUID landId,
		@Param("status") Status status,
		@Param("limit") short limit
	);
}
