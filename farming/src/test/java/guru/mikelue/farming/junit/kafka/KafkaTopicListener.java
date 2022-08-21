package guru.mikelue.farming.junit.kafka;

import static guru.mikelue.farming.junit.kafka.KafkaTopicAction.Phase.BEFORE;

import java.util.List;

import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import org.apache.kafka.clients.admin.Admin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This listener recognize {@link KafkaTopicAction} to perform actions before/after annotated classs/methods.
 */
public class KafkaTopicListener extends AbstractTestExecutionListener {
	private final static Logger logger = LoggerFactory.getLogger(KafkaTopicListener.class);

	private final static String ATTR_AFTER_ANNOTATION_CLASS = "_kafka_data_after_class";
	private final static String ATTR_AFTER_ANNOTATION_METHOD = "_kafka_data_after_method";

	public KafkaTopicListener() {}

	@Override
 	public void beforeTestClass(TestContext testContext)
	{
		var testClass = testContext.getTestClass();

		MergedAnnotations.from(testClass, SearchStrategy.INHERITED_ANNOTATIONS)
			.stream(KafkaTopicAction.class)
			.forEach(mergedAnnotation -> {
				var kafkaTopics = mergedAnnotation.synthesize();

				if (kafkaTopics.phase().equals(BEFORE)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Class [{}] has BEFORE operation. {}", testClass.getSimpleName(), kafkaTopics);
					}

					processKafkaAnnotation(kafkaTopics, testContext);
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Class [{}] has AFTER operation. {}", testClass.getSimpleName(), kafkaTopics);
					}

					testContext.setAttribute(ATTR_AFTER_ANNOTATION_CLASS, kafkaTopics);
				}
			});
	}

	@Override
 	public void beforeTestMethod(TestContext testContext)
	{
		var testMethod = testContext.getTestMethod();
		MergedAnnotations.from(testMethod)
			.stream(KafkaTopicAction.class)
			.forEach(mergedAnnotation -> {
				var kafkaAction = mergedAnnotation.synthesize();

				if (kafkaAction.phase().equals(BEFORE)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Method [{}] has BEFORE operation. {}", testMethod.getName(), kafkaAction);
					}
					processKafkaAnnotation(kafkaAction, testContext);
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Method [{}] has AFTER operation. {}", testMethod.getName(), kafkaAction);
					}
					testContext.setAttribute(ATTR_AFTER_ANNOTATION_CLASS, kafkaAction);
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
			processKafkaAnnotation(
				(KafkaTopicAction)testContext.getAttribute(ATTR_AFTER_ANNOTATION_CLASS),
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
			processKafkaAnnotation(
				(KafkaTopicAction)testContext.getAttribute(ATTR_AFTER_ANNOTATION_METHOD),
				testContext
			);
		} finally {
			testContext.removeAttribute(ATTR_AFTER_ANNOTATION_METHOD);
		}
	}

	private static void processKafkaAnnotation(KafkaTopicAction topicAction, TestContext testContext)
	{
		var topics = List.of(topicAction.topics());

		if (topics.size() == 0) {
			return;
		}

		logger.debug("[Kafka] Going to remove topics: {}", (Object)topicAction.topics());

		var appContext = testContext.getApplicationContext();

		appContext.getBean(KafkaListenerEndpointRegistry.class)
			.stop();

		try (var kafkaAdmin = appContext.getBean(Admin.class)) {
			kafkaAdmin.deleteTopics(topics);
		}
	}
}
