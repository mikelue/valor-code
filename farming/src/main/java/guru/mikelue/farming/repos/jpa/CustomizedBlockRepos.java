package guru.mikelue.farming.repos.jpa;

import java.util.List;
import java.util.UUID;

import guru.mikelue.farming.model.Block;

public interface CustomizedBlockRepos {
	List<Block> findAvailableByLandId(
		UUID landId, int askedBlocks
	);

	List<Block> findOccupiedByLandId(
		UUID landId, int askedBlocks
	);
}
