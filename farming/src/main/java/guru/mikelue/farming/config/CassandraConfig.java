package guru.mikelue.farming.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

@Configuration(proxyBeanMethods=false)
@EnableCassandraRepositories(basePackages="guru.mikelue.farming.repos.cassandra")
public class CassandraConfig {
	public CassandraConfig() {}
}
