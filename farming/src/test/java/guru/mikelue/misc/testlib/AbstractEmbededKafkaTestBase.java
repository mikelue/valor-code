package guru.mikelue.misc.testlib;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import guru.mikelue.farming.config.KafkaConfig;

@SpringBootTest(
	classes={
		KafkaAutoConfiguration.class,
		JacksonAutoConfiguration.class,
		KafkaConfig.class
	},
	properties={
		"spring.kafka.consumer.group-id=FarmingProducerServiceTest",
		"spring.kafka.consumer.auto-offset-reset=earliest",
		"kafka.topic.sowing.partitions=1",
		"kafka.topic.sowing.replicas=1",
		"kafka.topic.harvesting.partitions=1",
		"kafka.topic.harvesting.replicas=1",
		"kafka.topic.cleaning.partitions=1",
		"kafka.topic.cleaning.replicas=1",
	}
)
@EmbeddedKafka(
	partitions=1,
	topics={ KafkaConfig.TOPIC_SOWING },
	bootstrapServersProperty="spring.kafka.bootstrap-servers"
)
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractEmbededKafkaTestBase extends AbstractTestBase {
	@Autowired
	private ApplicationContext appContext;

	protected AbstractEmbededKafkaTestBase() {}

	public <K, V> ConsumerRecords<K, V> consumeAllWith(String groupId, String... topics)
	{
		var consumer = this.<K, V>subscribeAndGetConsumerWith(groupId, topics);
		return consumer.poll(Duration.ofSeconds(1));
	}

	public <K, V> ConsumerRecords<K, V> consumeAll(String... topics)
	{
		var consumer = this.<K, V>subscribeAndGetConsumer(topics);
		return consumer.poll(Duration.ofSeconds(1));
	}

	public <K, V> Consumer<K, V> subscribeAndGetConsumerWith(String groupId, String... topics)
	{
		var consumer = this.<K, V>getConsumerFactory()
			.createConsumer(groupId, Thread.currentThread().getName());
		consumer.subscribe(List.of(topics));
		return consumer;
	}

	public <K, V> Consumer<K, V> subscribeAndGetConsumer(String... topics)
	{
		var consumer = this.<K, V>getConsumerFactory()
			.createConsumer(getClass().getSimpleName(), Thread.currentThread().getName());
		consumer.subscribe(List.of(topics));
		return consumer;
	}

	@SuppressWarnings("unchecked")
	public <K, V> ConsumerFactory<K, V> getConsumerFactory()
	{
		return (ConsumerFactory<K, V>)appContext.getBean(ConsumerFactory.class);
	}

	@SuppressWarnings("unchecked")
	public <K, V> ConsumerFactory<K, V> getProducerFactory()
	{
		return (ConsumerFactory<K, V>)appContext.getBean(ProducerFactory.class);
	}
}
