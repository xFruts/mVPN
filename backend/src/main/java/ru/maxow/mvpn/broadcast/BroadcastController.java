package ru.maxow.mvpn.broadcast;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling broadcast requests.
 */
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BroadcastController {

  BroadcastService broadcastService;

  /**
   * Endpoint to send a broadcast message to a target audience.
   *
   * @param broadcastRequestDto the broadcast request containing the message and target audience
   */
  @PostMapping("/v1/broadcasts")
  public void sendBroadcast(@RequestBody BroadcastRequestDto broadcastRequestDto) {
    broadcastService.sendBroadcast(broadcastRequestDto);
  }

}
