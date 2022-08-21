package guru.mikelue.farming.junit.cassandra;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.cassandra.core.cql.ReactiveCqlTemplate;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.data.cassandra.core.cql.CqlTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import reactor.core.publisher.Flux;
import org.junit.platform.commons.JUnitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static guru.mikelue.farming.junit.cassandra.CassandraData.Phase.*;

/**
 * This listener recognize {@link CassandraData} and construct corresponding CQLs, resource files, or {@link CassandraDataExecutor}
 * to be executed with configured phase.<p>
 *
 * You must ensure your application context has {@link CqlTemplate} bean before using this listener.
 */
public class CassandraDataListener extends AbstractTestExecutionListener {
	private final Logger logger = LoggerFactory.getLogger(CassandraDataListener.class);

	private final static String ATTR_AFTER_ANNOTATION_CLASS = "_cassandra_data_after_class";
	private final static String ATTR_AFTER_ANNOTATION_METHOD = "_cassandra_data_after_method";

	public CassandraDataListener() {}

	@Override
 	public void beforeTestClass(TestContext testContext)
	{
		var testClass = testContext.getTestClass();

		MergedAnnotations.from(testClass)
			.stream(CassandraData.class)
			.forEach(mergedAnnotation -> {
				var cassandraData = mergedAnnotation.synthesize();

				if (cassandraData.phase().equals(BEFORE)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Class [{}] has BEFORE operation. {}", testClass.getSimpleName(), cassandraData);
					}
					processCassandraAnnotation(cassandraData, testContext);
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Class [{}] has AFTER operation. {}", testClass.getSimpleName(), cassandraData);
					}
					testContext.setAttribute(ATTR_AFTER_ANNOTATION_CLASS, cassandraData);
				}
			});
	}

	@Override
 	public void beforeTestMethod(TestContext testContext)
	{
		var testMethod = testContext.getTestMethod();
		MergedAnnotations.from(testMethod)
			.stream(CassandraData.class)
			.forEach(mergedAnnotation -> {
				var cassandraData = mergedAnnotation.synthesize();

				if (cassandraData.phase().equals(BEFORE)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Method [{}] has BEFORE operation. {}", testMethod.getName(), cassandraData);
					}
					processCassandraAnnotation(cassandraData, testContext);
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Method [{}] has AFTER operation. {}", testMethod.getName(), cassandraData);
					}
					testContext.setAttribute(ATTR_AFTER_ANNOTATION_CLASS, cassandraData);
				}
			});
	}

	@Override
 	public void afterTestClass(TestContext testContext)
	{
		if (!testContext.hasAttribute(ATTR_AFTER_ANNOTATION_CLASS)) {
			return;
		}

		try {
			processCassandraAnnotation(
				(CassandraData)testContext.getAttribute(ATTR_AFTER_ANNOTATION_CLASS),
				testContext
			);
		} finally {
			testContext.removeAttribute(ATTR_AFTER_ANNOTATION_CLASS);
		}
	}

	@Override
 	public void afterTestMethod(TestContext testContext)
	{
		if (!testContext.hasAttribute(ATTR_AFTER_ANNOTATION_METHOD)) {
			return;
		}

		try {
			processCassandraAnnotation(
				(CassandraData)testContext.getAttribute(ATTR_AFTER_ANNOTATION_METHOD),
				testContext
			);
		} finally {
			testContext.removeAttribute(ATTR_AFTER_ANNOTATION_METHOD);
		}
	}

	private void processCassandraAnnotation(
		CassandraData annotationInstant, TestContext testContext
	) {
		if (annotationInstant == null) {
			return;
		}

		var cqlTemplate = testContext.getApplicationContext()
			.getBean(ReactiveCqlTemplate.class);

		executeCqls(annotationInstant.cqls(), cqlTemplate);

		if (annotationInstant.resources().length > 0) {
			executeCqlFiles(annotationInstant.resources(), testContext.getApplicationContext(), cqlTemplate);
		}

		runExecutors(
			annotationInstant.executors(), testContext,
			testContext.getApplicationContext().getBean(ReactiveCassandraTemplate.class)
		);
	}

	private void executeCqls(String[] cqls, ReactiveCqlTemplate cqlTemplate)
	{
		Flux.fromArray(cqls)
			.flatMap(cqlTemplate::execute)
			.then()
			.block();
	}

	private void executeCqlFiles(String[] resources, ApplicationContext appContext, ReactiveCqlTemplate cqlTemplate)
	{
		for (var resourcePath: resources) {
			var resource = appContext.getResource(resourcePath);

			if (!resource.exists()) {
				logger.warn("Resource \"{}\" is not existing.", resourcePath);
				continue;
			}

			BufferedReader cqlReader = null;
			try {
				cqlReader = new BufferedReader(
					new InputStreamReader(resource.getInputStream())
				);

				var cqls = cqlReader.lines()
					.collect(Collectors.joining(" "))
					.split(";");

				executeCqls(cqls, cqlTemplate);
			} catch (IOException e) {
				logger.error("Reader from resource \"{}\" had error.", resourcePath);
				throw new JUnitException("Unable to reader from resource file.", e);
			} finally {
				if (cqlReader != null) {
					try {
						cqlReader.close();
					} catch (IOException e) {
						logger.error("Close resource \"{}\" had error.", resourcePath);
						throw new JUnitException("Unable to close reader of resource file.", e);
					}
				}
			}
		}
	}

	private Map<Class<? extends CassandraDataExecutor>, CassandraDataExecutor> cachedInstancesOfExecutors =
		new ConcurrentHashMap<>(16);
	private void runExecutors(Class<? extends CassandraDataExecutor>[] executors, TestContext testContext, ReactiveCassandraTemplate cassandraTemplate)
	{
		Flux.fromArray(executors)
			.map(executorType -> {
				if (!cachedInstancesOfExecutors.containsKey(executorType)) {
					var execImpl = BeanUtils.instantiateClass(executorType);
					cachedInstancesOfExecutors.put(executorType, execImpl);
				}

				return cachedInstancesOfExecutors.get(executorType);
			})
			.flatMap(executorInstant -> executorInstant.apply(testContext, cassandraTemplate))
			.then()
			.block();
	}
}
