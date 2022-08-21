package guru.mikelue.farming.repos.jpa;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class AbstractReposImplBase {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private EntityManager em;

	protected AbstractReposImplBase() {}

	public EntityManager getEntityManager()
	{
		return em;
	}

	public Logger getLogger()
	{
		return logger;
	}
}
