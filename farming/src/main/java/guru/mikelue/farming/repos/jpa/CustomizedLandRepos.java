package guru.mikelue.farming.repos.jpa;

import java.util.Collection;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import guru.mikelue.farming.model.Block;
import guru.mikelue.farming.model.Land;

public interface CustomizedLandRepos {
	Land addNewWithBlocks(Land newLand);

	@Transactional
	void addNewBlocks(Collection<Block> newBlocks);

	/**
	 * Deletes a land
	 *
	 * @return The number of blocks contained by the delete land
	 */
	@Transactional
	int purge(UUID landId);
}
