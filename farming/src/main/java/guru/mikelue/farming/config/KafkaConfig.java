package guru.mikelue.farming.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import org.apache.kafka.clients.admin.NewTopic;
import com.fasterxml.jackson.databind.ObjectMapper;

import guru.mikelue.farming.kafka.BlockSerialization;
import guru.mikelue.farming.model.Block;
import guru.mikelue.farming.model.Block.BlockId;

@Configuration(proxyBeanMethods=false)
public class KafkaConfig {
	public final static String TOPIC_SOWING = "sowing";
	public final static String TOPIC_HARVESTING = "harvesting";
	public final static String TOPIC_CLEANING = "cleaning";

	public KafkaConfig() {}

	@Bean
	NewTopic buildTopicForSowing(
		@Value("${kafka.topic.sowing.partitions}")
		int partitions,
		@Value("${kafka.topic.sowing.replicas}")
		int replicas
	) {
		return TopicBuilder.name(TOPIC_SOWING)
			.partitions(partitions)
			.replicas(replicas)
			.build();
	}

	@Bean
	NewTopic buildTopicForHarvesting(
		@Value("${kafka.topic.harvesting.partitions}")
		int partitions,
		@Value("${kafka.topic.harvesting.replicas}")
		int replicas
	) {
		return TopicBuilder.name(TOPIC_HARVESTING)
			.partitions(partitions)
			.replicas(replicas)
			.build();
	}

	@Bean
	NewTopic buildTopicForCleaning(
		@Value("${kafka.topic.cleaning.partitions}")
		int partitions,
		@Value("${kafka.topic.cleaning.replicas}")
		int replicas
	) {
		return TopicBuilder.name(TOPIC_CLEANING)
			.partitions(partitions)
			.replicas(replicas)
			.build();
	}

	@Bean
	DefaultKafkaProducerFactoryCustomizer blockProducerFactory(
		@Autowired
		ObjectMapper objectMapper
	) {
		return untypedFactory -> {
			@SuppressWarnings("unchecked")
			var factory = (DefaultKafkaProducerFactory<BlockId, Block>)untypedFactory;

			factory.setKeySerializer(new BlockSerialization.KeySerializerImpl());
			factory.setValueSerializer(new JsonSerializer<>(objectMapper));
		};
	}

	@Bean
	DefaultKafkaConsumerFactoryCustomizer blockConsumerFactory(
		@Autowired
		ObjectMapper objectMapper
	) {
		return untypedFactory -> {
			@SuppressWarnings("unchecked")
			var factory = (DefaultKafkaConsumerFactory<BlockId, Block>)untypedFactory;

			var jsonDeserializer = new JsonDeserializer<Block>(objectMapper);
			jsonDeserializer.trustedPackages("guru.mikelue.farming.model");

			factory.setKeyDeserializer(new BlockSerialization.KeyDeserializerImpl());
			factory.setValueDeserializer(jsonDeserializer);
		};
	}
}
