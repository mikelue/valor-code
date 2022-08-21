package guru.mikelue.farming.repos.jpa;

import java.util.List;
import java.util.UUID;

import guru.mikelue.farming.model.Block;

public class CustomizedBlockReposImpl extends AbstractReposImplBase implements CustomizedBlockRepos {
	public CustomizedBlockReposImpl() {}

	@Override
	public List<Block> findAvailableByLandId(
		UUID landId, int askedBlocks
	) {
		return findByLandIdAndStatus(landId, Block.Status.Available, askedBlocks);
	}

	@Override
	public List<Block> findOccupiedByLandId(
		UUID landId, int askedBlocks
	) {
		return findByLandIdAndStatus(landId, Block.Status.Occupied, askedBlocks);
	}

	private List<Block> findByLandIdAndStatus(
		UUID landId, Block.Status status, int askedBlocks
	) {
		return getEntityManager()
			.createNamedQuery("Block.findByLandIdAndStatus", Block.class)
			.setParameter("status", status)
			.setParameter("land_id", landId)
			.setMaxResults(askedBlocks)
			.getResultList();
	}
}
