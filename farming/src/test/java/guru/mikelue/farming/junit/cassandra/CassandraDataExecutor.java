package guru.mikelue.farming.junit.cassandra;

import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.test.context.TestContext;

import reactor.core.publisher.Mono;

/**
 * The type used to executor operations to Cassandra, with {@link CassandraData#executors}.
 *
 * The instance would be implemented in <em>stateless way</em>.
 *
 * @see CassandraData
 */
@FunctionalInterface
public interface CassandraDataExecutor {
	/**
	 * This function get called by corresponding phase defined by {@link CassandraData}.
	 *
	 * @param ctx The test context provided by SpringFramework
	 * @param cqlTemplate The utility to execute Cassandra operations by Java code.
	 *
	 * @return A reactive execution
	 */
	Mono<Void> apply(TestContext ctx, ReactiveCassandraTemplate cassandraTemplate);
}
