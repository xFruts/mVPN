package ru.maxow.mvpn.settings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import ru.maxow.mvpn.model.ThemeDto;
import ru.maxow.mvpn.util.exception.UnauthorizedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceImplTest {

    private static final String USER_ID = "c23f47e8-2c68-425e-92e8-7f355bfb7189";

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @InjectMocks
    private UserSettingsServiceImpl userSettingsService;

    @Captor
    private ArgumentCaptor<UserSettings> settingsCaptor;

    @BeforeEach
    void setUp() {
        mockSecurityContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("updateTheme: Должен создать новые настройки, если пользователя нет в БД")
    void shouldCreateNewSettingsWhenNotFound() {
        ThemeDto requestDto = new ThemeDto();
        requestDto.setTheme(ThemeDto.ThemeEnum.DARK);

        given(userSettingsRepository.findById(USER_ID)).willReturn(Optional.empty());

        userSettingsService.updateTheme(requestDto);

        verify(userSettingsRepository).save(settingsCaptor.capture());
        UserSettings savedSettings = settingsCaptor.getValue();

        assertThat(savedSettings.getKeycloakUserId()).isEqualTo(USER_ID);
        assertThat(savedSettings.getTheme()).isEqualTo("DARK");
    }

    @Test
    @DisplayName("updateTheme: Должен обновить существующие настройки, если они есть в БД")
    void shouldUpdateExistingSettingsWhenFound() {
        ThemeDto requestDto = new ThemeDto();
        requestDto.setTheme(ThemeDto.ThemeEnum.LIGHT);

        UserSettings existingSettings = new UserSettings();
        existingSettings.setKeycloakUserId(USER_ID);
        existingSettings.setTheme("DARK");

        given(userSettingsRepository.findById(USER_ID)).willReturn(Optional.of(existingSettings));

        userSettingsService.updateTheme(requestDto);

        verify(userSettingsRepository).save(settingsCaptor.capture());
        UserSettings savedSettings = settingsCaptor.getValue();

        assertThat(savedSettings.getKeycloakUserId()).isEqualTo(USER_ID);
        assertThat(savedSettings.getTheme()).isEqualTo("LIGHT");
    }

    @Test
    @DisplayName("getTheme: Должен вернуть тему SYSTEM, если настроек нет в БД")
    void shouldReturnDefaultThemeWhenNotFound() {
        given(userSettingsRepository.findById(USER_ID)).willReturn(Optional.empty());

        ThemeDto result = userSettingsService.getTheme();

        assertThat(result).isNotNull();
        assertThat(result.getTheme()).isEqualTo(ThemeDto.ThemeEnum.SYSTEM);
        verify(userSettingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("getTheme: Должен вернуть сохраненную тему из БД")
    void shouldReturnSavedThemeWhenFound() {
        UserSettings existingSettings = new UserSettings();
        existingSettings.setKeycloakUserId(USER_ID);
        existingSettings.setTheme("DARK");

        given(userSettingsRepository.findById(USER_ID)).willReturn(Optional.of(existingSettings));

        ThemeDto result = userSettingsService.getTheme();

        assertThat(result).isNotNull();
        assertThat(result.getTheme()).isEqualTo(ThemeDto.ThemeEnum.DARK);
    }

    @Test
    @DisplayName("getCurrentUserId: Должен выбросить UnauthorizedException, если токена нет")
    void shouldThrowExceptionWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();
        ThemeDto requestDto = new ThemeDto();
        requestDto.setTheme(ThemeDto.ThemeEnum.DARK);

        assertThatThrownBy(() -> userSettingsService.updateTheme(requestDto))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Authentication is null");

        verify(userSettingsRepository, never()).findById(any());
    }

    private void mockSecurityContext() {
        OAuth2User oAuth2User = mock(OAuth2User.class);
        lenient().when(oAuth2User.getAttribute("sub"))
            .thenReturn(UserSettingsServiceImplTest.USER_ID);

        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(oAuth2User);

        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }
}
