package ru.maxow.mvpn.broadcast;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ru.maxow.mvpn.model.BroadcastRequestDto;

@Slf4j
@Service
public class BroadcastService {

  private final String broadcastTopicName;
  private final KafkaTemplate<String, BroadcastRequestDto> kafkaTemplate;

  public BroadcastService(
      @Value("${app.kafka.topics.broadcast}") String broadcastTopicName,
      KafkaTemplate<String, BroadcastRequestDto> kafkaTemplate
  ) {
    this.broadcastTopicName = broadcastTopicName;
    this.kafkaTemplate = kafkaTemplate;
  }

  public void sendBroadcast(BroadcastRequestDto broadcastRequestDto) {
    String key = UUID.randomUUID().toString();
    log.info("Attempting to send broadcast request to topic: {}, key: {}", broadcastTopicName, key);
    CompletableFuture<SendResult<String, BroadcastRequestDto>> future =
        kafkaTemplate.send(broadcastTopicName, key, broadcastRequestDto);

    future.whenComplete((result, ex) -> {
      if (ex == null) {
        log.info("Sent broadcast successfully. Topic: {}, Partition: {}, Offset: {}",
            broadcastTopicName,
            result.getRecordMetadata().partition(),
            result.getRecordMetadata().offset());
      } else {
        log.error("Failed to send broadcast to topic: {}", broadcastTopicName, ex);
      }
    });
  }
}
