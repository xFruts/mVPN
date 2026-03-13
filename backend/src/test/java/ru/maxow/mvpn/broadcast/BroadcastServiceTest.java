package ru.maxow.mvpn.broadcast;

import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import ru.maxow.mvpn.model.BroadcastRequestDto;
import ru.maxow.mvpn.model.TargetAudience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BroadcastService - Unit тесты")
class BroadcastServiceTest {

  @Mock
  private KafkaTemplate<String, BroadcastRequestDto> kafkaTemplate;

  private BroadcastService broadcastService;

  @BeforeEach
  void setUp() {
    broadcastService = new BroadcastService("broadcast-requests", kafkaTemplate);
  }

  @Test
  @DisplayName("Должен отправить сообщение в Kafka topic из конфигурации")
  void shouldSendMessageToConfiguredTopic() {
    BroadcastRequestDto dto = new BroadcastRequestDto();
    dto.setMessage("Hello");
    dto.setTargetAudience(TargetAudience.ALL);

    @SuppressWarnings("unchecked")
    SendResult<String, BroadcastRequestDto> sendResult = mock(SendResult.class);
    RecordMetadata recordMetadata = mock(RecordMetadata.class);
    when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
    when(recordMetadata.partition()).thenReturn(0);
    when(recordMetadata.offset()).thenReturn(1L);

    when(kafkaTemplate.send(anyString(), anyString(), any(BroadcastRequestDto.class)))
        .thenReturn(CompletableFuture.completedFuture(sendResult));

    broadcastService.sendBroadcast(dto);

    ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<BroadcastRequestDto> payloadCaptor = ArgumentCaptor.forClass(BroadcastRequestDto.class);

    verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());
    assertThat(topicCaptor.getValue()).isEqualTo("broadcast-requests");
    assertThat(keyCaptor.getValue()).isNotBlank();
    assertThat(payloadCaptor.getValue()).isEqualTo(dto);
  }
}
