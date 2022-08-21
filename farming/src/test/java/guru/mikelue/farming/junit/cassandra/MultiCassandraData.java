package guru.mikelue.farming.junit.cassandra;

import java.lang.annotation.*;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented @Inherited
public @interface MultiCassandraData {
	CassandraData[] value();
}
