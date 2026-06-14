package ru.maxow.mvpn.settings;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.RestController;
import ru.maxow.mvpn.api.SettingsApi;
import ru.maxow.mvpn.model.ThemeDto;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SettingsController implements SettingsApi {
  UserSettingsService userSettingsService;

  @Override
  public ThemeDto getTheme() {
    return userSettingsService.getTheme();
  }

  @Override
  public void updateTheme(ThemeDto themeDto) {
    userSettingsService.updateTheme(themeDto);
  }
}
