package guru.mikelue.farming.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.function.Consumer;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTestersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.JsonContentAssert;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.data.cassandra.ReactiveSessionFactory;
import org.springframework.data.cassandra.core.cql.ReactiveCqlTemplate;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.Builder;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import guru.mikelue.farming.config.KafkaConfig;
import guru.mikelue.farming.junit.cassandra.CassandraDataListener;
import guru.mikelue.farming.junit.kafka.KafkaTopicAction;
import guru.mikelue.farming.junit.kafka.KafkaTopicListener;
import guru.mikelue.misc.testlib.AbstractTestBase;

@SpringBootTest(
	webEnvironment=RANDOM_PORT,
	properties={
		"spring.test.jsontesters.enabled=true",
		// "spring.kafka.consumer.auto-offset-reset=earliest"
	}
)
@Import({KafkaTestConfig.class, CassandraTestConfig.class})
@ImportAutoConfiguration(JsonTestersAutoConfiguration.class)
@TestExecutionListeners(listeners={KafkaTopicListener.class, CassandraDataListener.class}, mergeMode=MERGE_WITH_DEFAULTS)
@KafkaTopicAction(topics={ KafkaConfig.TOPIC_CLEANING, KafkaConfig.TOPIC_HARVESTING, KafkaConfig.TOPIC_SOWING })
public abstract class AbstractITBase extends AbstractTestBase {
	@Autowired
	private WebTestClient webClient;

	@Autowired
	private JacksonTester<JsonNode> jacksonTester;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ApplicationContext appCtx;

	@Autowired
	private EntityManager entityManager;

	protected AbstractITBase() {}
	protected final Consumer<JsonNode> LogJsonContent = jsonNode -> {
		getLogger().info("Output JSON: << {} >>", jsonNode);
	};

	public WebTestClient getWebTestClient()
	{
		return webClient;
	}

	public <T extends JsonNode> JsonContent<JsonNode> asJsonContent(T object)
	{
		try {
			return jacksonTester.write(object);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public <T extends JsonNode> JsonContentAssert assertJsonContent(T object)
	{
		return assertThat(asJsonContent(object));
	}

	public ObjectMapper getObjectMapper()
	{
		return objectMapper;
	}

	public ObjectNode createObjectNode()
	{
		return getObjectMapper().createObjectNode();
	}

	public ArrayNode createArrayNode()
	{
		return getObjectMapper().createArrayNode();
	}

	public EntityManager getEntityManager()
	{
		return entityManager;
	}

	public WebTestClientConfigurer webClientTimeout(Duration timeout)
	{
		return new WebTestClientConfigurer() {
            @Override
            public void afterConfigurerAdded(
				Builder builder, WebHttpHandlerBuilder httpHandlerBuilder,
				ClientHttpConnector connector
			) {
				builder.responseTimeout(timeout);
            }
		};
	}

	public final static int DEFAULT_KAFKA_CONSUMER_TIMEOUT = 2000;

	@SuppressWarnings("unchecked")
	public <K, V> ConsumerFactory<K, V> getConsumerFactory()
	{
		return (ConsumerFactory<K, V>)appCtx.getBean(ConsumerFactory.class);
	}

	/**
	 * Creates and assigns all of the partitions of the topic to the consumer.
	 */
	public <K, V> org.apache.kafka.clients.consumer.Consumer<K, V> createConsumer(String groupId, String topic)
	{
		var factory = this.<K, V>getConsumerFactory();
		var newConsumer = factory.createConsumer(groupId, "");

		var partitions = newConsumer.partitionsFor(topic);
		var topicPartitions = new ArrayList<TopicPartition>(partitions.size());
		for (var partitionInfo: partitions) {
			topicPartitions.add(
				new TopicPartition(topic, partitionInfo.partition())
			);
		}

		newConsumer.assign(topicPartitions);

		return newConsumer;
	}

	/**
	 * Retreves all of the records in certain topic.
	 */
	public <K, V> ConsumerRecords<K, V> consumerAllRecords(String groupId, String topic)
	{
		return this.<K, V>consumerAllRecords(groupId, topic, DEFAULT_KAFKA_CONSUMER_TIMEOUT);
	}

	/**
	 * Retreves all of the records in certain topic(with timeout).
	 */
	public <K, V> ConsumerRecords<K, V> consumerAllRecords(String groupId, String topic, int timeoutInMilliseconds)
	{
		var kafkaConsumer = this.<K,V>createConsumer(groupId, topic);
		var messages = KafkaTestUtils.getRecords(kafkaConsumer, timeoutInMilliseconds);
		return messages;
	}
}

class KafkaTestConfig {
    @Bean @Scope(SCOPE_PROTOTYPE)
    Admin nativeKafkaAdmin(
		KafkaAdmin admin
	) {
		return Admin.create(
			admin.getConfigurationProperties()
		);
    }
}

class CassandraTestConfig {
	@Bean
	ReactiveCqlTemplate cqlTemplate(ReactiveSessionFactory sessionFactory)
	{
		return new ReactiveCqlTemplate(sessionFactory);
	}
}
