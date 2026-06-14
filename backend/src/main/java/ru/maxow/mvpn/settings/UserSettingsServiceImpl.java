package ru.maxow.mvpn.settings;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.ThemeDto;
import ru.maxow.mvpn.util.exception.UnauthorizedException;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserSettingsServiceImpl implements UserSettingsService {
  UserSettingsRepository userSettingsRepository;

  @Override
  public ThemeDto getTheme() {
    String userId = getCurrentUserId();
    UserSettings settings = userSettingsRepository.findById(userId)
        .orElseGet(() -> {
          UserSettings newSettings = new UserSettings();
          newSettings.setKeycloakUserId(userId);
          newSettings.setTheme(ThemeDto.ThemeEnum.SYSTEM.name());

          return newSettings;
        });

    ThemeDto response = new ThemeDto();
    response.setTheme(ThemeDto.ThemeEnum.valueOf(settings.getTheme()));

    return response;
  }

  @Override
  @Transactional
  public void updateTheme(ThemeDto themeDto) {
    String userId = getCurrentUserId();
    UserSettings settings = userSettingsRepository.findById(userId)
            .orElseGet(() -> {
                UserSettings newSettings = new UserSettings();
                newSettings.setKeycloakUserId(userId);
                return newSettings;
            });
    settings.setTheme(themeDto.getTheme().name());
    userSettingsRepository.save(settings);
  }

  private String getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null) {
        throw new UnauthorizedException("Authentication is null");
    }

    if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
        String sub = oauth2User.getAttribute("sub");
        if (sub != null) {
            return sub;
        }
    }

    throw new UnauthorizedException("Unauthorized or unsupported principal type");
  }
}
