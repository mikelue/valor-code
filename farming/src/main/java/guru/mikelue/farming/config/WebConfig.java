package guru.mikelue.farming.config;

import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

import guru.mikelue.farming.web.GlobalErrorAttributes;
import guru.mikelue.misc.springframework.data.web.ReactivePageableParamResolver;
import guru.mikelue.misc.springframework.data.web.ReactiveSortParamResolver;

@Configuration(proxyBeanMethods=false)
public class WebConfig implements WebFluxConfigurer {
    @Override
    public void configureArgumentResolvers(ArgumentResolverConfigurer configurer)
	{
		var sortResolver = new ReactiveSortParamResolver();
		var pageableResolver = new ReactivePageableParamResolver();
		pageableResolver.setSortResolver(sortResolver);

		configurer.addCustomResolver(sortResolver);
		configurer.addCustomResolver(pageableResolver);
    }

	@Bean
	ErrorAttributes defaultErrorAttributes()
	{
		return new GlobalErrorAttributes();
	}
}
