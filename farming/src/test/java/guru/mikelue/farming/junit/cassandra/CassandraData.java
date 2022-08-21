package guru.mikelue.farming.junit.cassandra;

import java.lang.annotation.*;

/**
 * Used to indicate a method of class to be wrapped with
 * Cassandra operations {@link CassandraData.Phase#BEFORE BEFORE} tests or {@link CassandraData.Phase#AFTER AFTER} tests.
 *
 * The precedence of loading operations:
 * <ol>
 * 	<li>{@link CassandraData#cqls cqls}</li>
 * 	<li>{@link CassandraData#resources resources}</li>
 * 	<li>{@link CassandraData#executors executors}</li>
 * </ol>
 *
 * The {@link CassandraData#resources resources} would be loaded by <em>ApplicationContext</em> in {@link TestContext} provided by SpringFramework.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented @Inherited
@Repeatable(value=MultiCassandraData.class)
public @interface CassandraData {
	/**
	 * The phase to apply Cassandra operations.
	 */
	public enum Phase {
		/**
		 * Applies Cassandra operations after class or method.
		 */
		AFTER,
		/**
		 * Applies Cassandra operations before class or method.
		 */
		BEFORE;
	}

	/**
	 * As plain text of CQL fed to Cassandra database.
	 *
	 * @return The execution sequence is same as value of of this field.
	 */
	public String[] cqls() default {};
	/**
	 * As CQL files fed to Cassandra database.
	 *
	 * @return The execution sequence is same as value of of this field.
	 */
	public String[] resources() default {};
	/**
	 * As java code for execution
	 *
	 * @return The execution sequence is same as value of of this field.
	 */
	public Class<? extends CassandraDataExecutor>[] executors() default {};
	/**
	 * The phase of lifecycle applied to method/class.
	 *
	 * @return The phase trigger corresponding Cassandra operations.
	 */
	public Phase phase() default Phase.BEFORE;
}
