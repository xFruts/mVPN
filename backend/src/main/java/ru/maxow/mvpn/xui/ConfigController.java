package ru.maxow.mvpn.xui;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.maxow.mvpn.api.ConfigApi;
import ru.maxow.mvpn.util.exception.NotFoundException;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigController implements ConfigApi {

  ConfigFacade configFacade;

  public String getConfigByVerificationCode(UUID verificationCode) {
    return configFacade.getSubscriptionConfig(verificationCode);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<String> handleNotFoundForConfig(NotFoundException ex) {
  return ResponseEntity.status(HttpStatus.NOT_FOUND)
      .contentType(MediaType.TEXT_PLAIN)
      .body("Config not found");
  }
}
