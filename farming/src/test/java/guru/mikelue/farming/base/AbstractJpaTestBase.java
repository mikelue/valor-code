package guru.mikelue.farming.base;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import guru.mikelue.farming.config.JpaConfig;
import guru.mikelue.misc.testlib.AbstractTestBase;

@DataJpaTest(showSql=false)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@Import({JpaConfig.class})
@ImportAutoConfiguration({
	JacksonAutoConfiguration.class
})
public abstract class AbstractJpaTestBase extends AbstractTestBase {
	@PersistenceContext
	private EntityManager em;

	protected AbstractJpaTestBase() {}

	protected EntityManager getEntityManager()
	{
		return em;
	}

	protected void clearEntityManager()
	{
		em.flush();
		em.clear();
	}
}
