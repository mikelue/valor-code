package guru.mikelue.farming.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods=false)
@EnableJpaRepositories(basePackages="guru.mikelue.farming.repos.jpa")
public class JpaConfig {
	public JpaConfig() {}
}
