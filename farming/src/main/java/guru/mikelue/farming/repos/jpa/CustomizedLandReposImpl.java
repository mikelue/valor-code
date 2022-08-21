package guru.mikelue.farming.repos.jpa;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.persistence.criteria.CriteriaDelete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import guru.mikelue.farming.model.Block;
import guru.mikelue.farming.model.Land;

public class CustomizedLandReposImpl extends AbstractReposImplBase implements CustomizedLandRepos {
	@Autowired @Lazy
	private LandRepos selfRepos;

	public final static int BATCH_SIZE = 128;

	public CustomizedLandReposImpl() {}

    @Override
    public Land addNewWithBlocks(Land newLand)
	{
		var now = Instant.now();
		newLand.setCreationTime(now);
        newLand = selfRepos.save(newLand);

		List<Block> currentBlocks = new ArrayList<>(BATCH_SIZE);
		for (var i = 0; i < newLand.getSize(); i++) {
			if (currentBlocks.size() >= BATCH_SIZE) {
				selfRepos.addNewBlocks(currentBlocks);
				currentBlocks.clear();
			}

			var newBlock = new Block();
			newBlock.setId((short)i);
			newBlock.setLand(newLand);
			newBlock.setStatus(Block.Status.Available);
			newBlock.setUpdateTime(now);

			currentBlocks.add(newBlock);
		}

		if (currentBlocks.size() > 0) {
			selfRepos.addNewBlocks(currentBlocks);
		}

		return newLand;
    }

	@Override
	public void addNewBlocks(Collection<Block> newBlocks)
	{
		getLogger().debug("Batch insertion for [{}] blocks.", newBlocks.size());
		var em = getEntityManager();
		for (var b: newBlocks) {
			em.persist(b);
		}
	}

    @Override
    public int purge(UUID landId)
	{
		var deleteBlock = getEntityManager()
			.createQuery("""
			DELETE Block b
			WHERE b.landId = ?1
			""")
			.setParameter(1, landId);

		var deletedRowsOfBlock = deleteBlock.executeUpdate();
		selfRepos.deleteById(landId);

        return deletedRowsOfBlock;
    }
}
