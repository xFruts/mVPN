package ru.maxow.mvpn.xui;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/config")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigController {

  ConfigFacade configFacade;

  @GetMapping(value = "/{verificationCode}", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> getConfig(@PathVariable UUID verificationCode) {
    String config = configFacade.getSubscriptionConfig(verificationCode);
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_PLAIN)
        .body(config);
  }
}
