/**
 * This package provides declarative style for preparing/cleaning data for Cassandra database.<p>
 *
 * Add listener:
 *
 * <pre>{@code
 * @TestExecutionListeners({ CassandraDataListener.class })
 * public class YourTestClass {
 *     // Your class ...
 * }
 * }</pre>
 *
 * Usage example:
 *
 * <pre>{@code
 * @Test
 * @CassandraData(
 *     cqls={
 *         """
 *         INSERT INTO tab_1(col_1, col_2, col_3) VALUES('200', 'victoria.saunders@xw19.bell.cloud', 89)
 *         """
 *     },
 *     resources="classpath:a/b/c/Something.cql",
 *     executors=CassandraDataExecutor.class,
 *     phase=Phase.BEFORE
 * )
 * void yourTest()
 * {
 *     // Your tests ....
 * }
 * }</pre>
 *
 * As above sample shown, annotate your test method/class with {@link CassandraData}, and
 * put your CQL, CQL file, or implementation of {@link CassandraDataExecutor} to alter data around your tests.
 *
 * @see CassandraDataListener
 * @see CassandraData
 * @see CassandraDataExecutor
 */
package guru.mikelue.farming.junit.cassandra;
