package ru.maxow.mvpn.xui;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import ru.maxow.mvpn.api.ConfigApi;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigController implements ConfigApi {

  ConfigFacade configFacade;

  public String getConfigByVerificationCode(UUID verificationCode) {
    return configFacade.getSubscriptionConfig(verificationCode);
  }
}
