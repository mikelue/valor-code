package guru.mikelue.farming.base;

import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.cassandra.DataCassandraTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.cassandra.ReactiveSessionFactory;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.data.cassandra.core.cql.ReactiveCqlTemplate;
import org.springframework.test.context.TestExecutionListeners;

import guru.mikelue.farming.config.CassandraConfig;
import guru.mikelue.farming.junit.cassandra.CassandraDataListener;
import guru.mikelue.misc.testlib.AbstractTestBase;

@DataCassandraTest
@Import({CassandraConfig.class, TestCassandraConfig.class})
@TestExecutionListeners(listeners={CassandraDataListener.class}, mergeMode=MERGE_WITH_DEFAULTS)
public abstract class AbstractCassandraTestBase extends AbstractTestBase {
	@Autowired
	private ReactiveCqlTemplate cqlTmpl;
	@Autowired
	private ReactiveCassandraTemplate cassandraTmpl;

	protected AbstractCassandraTestBase() {}

	public ReactiveCqlTemplate getCqlTemplate()
	{
		return cqlTmpl;
	}

	public ReactiveCassandraTemplate getCassandraTemplate()
	{
		return cassandraTmpl;
	}
}

class TestCassandraConfig {
	@Bean
	ReactiveCqlTemplate cqlTemplate(
		ReactiveSessionFactory reactiveSessionFactory
	) {
		return new ReactiveCqlTemplate(reactiveSessionFactory);
	}
}
