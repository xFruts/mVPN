package ru.maxow.mvpn.broadcast;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/** Service for sending broadcast requests to a Kafka topic. */
@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastService {

  public static final String BROADCAST_TOPIC_NAME = "broadcast-requests";

  private final KafkaTemplate<String, BroadcastRequestDto> kafkaTemplate;

  /**
   * Sends a broadcast request to the Kafka topic.
   *
   * @param broadcastRequestDto the broadcast request data transfer object
   */
  public void sendBroadcast(BroadcastRequestDto broadcastRequestDto) {
    log.info("Sending broadcast request to topic Kafka: {}", BROADCAST_TOPIC_NAME);
    kafkaTemplate.send(BROADCAST_TOPIC_NAME, broadcastRequestDto);
  }

}
