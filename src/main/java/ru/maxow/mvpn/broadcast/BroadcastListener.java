package ru.maxow.mvpn.broadcast;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.maxow.mvpn.adapter.telegram.TelegramSenderService;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRole;
import ru.maxow.mvpn.user.UserService;

/**
 * Listener for broadcast messages from Kafka topic.
 * It processes incoming broadcast requests and sends messages to the target audience.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BroadcastListener {

  UserService userService;
  TelegramSenderService senderService;

  /**
   * Listens to the broadcast topic and handles incoming broadcast requests.
   *
   * @param broadcastRequestDto the broadcast request data transfer object
   */
  @KafkaListener(topics = BroadcastService.BROADCAST_TOPIC_NAME, groupId = "broadcast-group-dev")
  public void handleBroadcast(BroadcastRequestDto broadcastRequestDto) {
    if (broadcastRequestDto == null
        || broadcastRequestDto.message() == null
        || broadcastRequestDto.message().isEmpty()) {
      log.warn("Received empty broadcast request");
      return;
    }

    if (broadcastRequestDto.targetAudience() == null) {
      log.warn("Received broadcast request with null target audience");
      return;
    }

    List<User> targetUsers = findTargetUsers(broadcastRequestDto);
    log.info("Got task into Kafka to mailing for {} users", targetUsers.size());

    for (User user : targetUsers) {
      if (user.getUserTelegramId() != null) {
        try {
          senderService.sendMessage(
              user.getUserTelegramId().toString(),
              broadcastRequestDto.message()
          );
          Thread.sleep(100); // delay for telegram API
        } catch (Exception e) {
          log.error("Cannot send message user with ID {}: {}", user.getId(), e.getMessage());
        }
      }
    }
    log.info("Finish mailing for Kafka task");
  }

  private List<User> findTargetUsers(BroadcastRequestDto requestDto) {
    try {
      TargetAudience target = requestDto.targetAudience();
      return switch (target) {
        case ALL -> userService.findAll();
        case REGULAR -> userService.getUsersByRole(UserRole.REGULAR);
        case VIP -> userService.getUsersByRole(UserRole.VIP);
        case ADMIN -> userService.getUsersByRole(UserRole.ADMIN);
        case CUSTOM_LIST ->
            (requestDto.customUserIds() != null && !requestDto.customUserIds().isEmpty())
                ? userService.getUsersByTelegramIds(requestDto.customUserIds())
                : new ArrayList<>();
      };
    } catch (Exception e) {
      log.error("Cannot find target audience for request {}: {}",
          requestDto.targetAudience(), e.getMessage());
    }
    return new ArrayList<>();
  }

}
