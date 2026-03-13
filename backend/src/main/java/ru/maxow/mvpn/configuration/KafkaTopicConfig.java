package ru.maxow.mvpn.configuration;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import ru.maxow.mvpn.model.BroadcastRequestDto;

@Configuration
public class KafkaTopicConfig {

  @Bean
  public NewTopic broadcastTopic(@Value("${app.kafka.topics.broadcast}") String topicName) {
    return TopicBuilder.name(topicName)
        .partitions(1)
        .replicas(1)
        .build();
  }

  @Bean
  public ProducerFactory<String, BroadcastRequestDto> broadcastProducerFactory(
      @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
  ) {
    Map<String, Object> configs = new HashMap<>();
    configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(configs);
  }

  @Bean
  public KafkaTemplate<String, BroadcastRequestDto> broadcastKafkaTemplate(
      ProducerFactory<String, BroadcastRequestDto> broadcastProducerFactory
  ) {
    return new KafkaTemplate<>(broadcastProducerFactory);
  }

  @Bean
  public ConsumerFactory<String, BroadcastRequestDto> broadcastConsumerFactory(
      @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
      @Value("${app.kafka.groups.broadcast}") String groupId
  ) {
    Map<String, Object> configs = new HashMap<>();
    configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configs.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
    configs.put("spring.json.trusted.packages", "ru.maxow.mvpn.model");
    configs.put("spring.json.value.default.type", BroadcastRequestDto.class.getName());
    return new DefaultKafkaConsumerFactory<>(configs);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, BroadcastRequestDto> kafkaListenerContainerFactory(
      ConsumerFactory<String, BroadcastRequestDto> broadcastConsumerFactory
  ) {
    ConcurrentKafkaListenerContainerFactory<String, BroadcastRequestDto> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(broadcastConsumerFactory);
    return factory;
  }
}
