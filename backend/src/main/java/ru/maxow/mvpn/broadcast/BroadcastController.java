package ru.maxow.mvpn.broadcast;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.RestController;
import ru.maxow.mvpn.api.BroadcastsApi;
import ru.maxow.mvpn.model.BroadcastRequestDto;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BroadcastController implements BroadcastsApi {

  BroadcastService broadcastService;

  @Override
  public void sendBroadcast(BroadcastRequestDto broadcastRequestDto) {
    broadcastService.sendBroadcast(broadcastRequestDto);
  }
}
