package ru.maxow.mvpn.settings;

import ru.maxow.mvpn.model.ThemeDto;

public interface UserSettingsService {
  ThemeDto getTheme();

  void updateTheme(ThemeDto themeDto);
}
