package ru.maxow.mvpn.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import ru.maxow.mvpn.broadcast.BroadcastService;

/**
 * Configuration class for Kafka topics.
 */
@Configuration
public class KafkaTopicConfig {

  /**
   * Creates a Kafka topic for broadcast requests.
   *
   * @return the NewTopic instance representing the broadcast topic
   */
  @Bean
  public NewTopic broadcastTopic() {
    return TopicBuilder.name("${app.kafka.topics.broadcast}")
        .partitions(1)
        .replicas(1)
        .build();
  }
}
