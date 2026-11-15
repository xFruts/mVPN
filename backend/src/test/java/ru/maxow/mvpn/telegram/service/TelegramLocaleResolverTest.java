package ru.maxow.mvpn.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@DisplayName("TelegramLocaleResolver - Unit Tests")
class TelegramLocaleResolverTest {

  @Test
  @DisplayName("Returns Russian locale for null update or empty user")
  void shouldReturnRussianForNullOrEmptyUser() {
    assertThat(TelegramLocaleResolver.resolveLocale(null))
        .isEqualTo(TelegramLocaleResolver.LOCALE_RU);

    Update emptyUpdate = new Update();
    assertThat(TelegramLocaleResolver.resolveLocale(emptyUpdate))
        .isEqualTo(TelegramLocaleResolver.LOCALE_RU);

    Message msgWithoutFrom = new Message();
    emptyUpdate.setMessage(msgWithoutFrom);
    assertThat(TelegramLocaleResolver.resolveLocale(emptyUpdate))
        .isEqualTo(TelegramLocaleResolver.LOCALE_RU);
  }

  @Test
  @DisplayName("Returns Russian locale for ru, be, kk, uk language codes")
  void shouldReturnRussianForCisLanguageCodes() {
    assertThat(resolveForCode("ru")).isEqualTo(TelegramLocaleResolver.LOCALE_RU);
    assertThat(resolveForCode("ru-RU")).isEqualTo(TelegramLocaleResolver.LOCALE_RU);
    assertThat(resolveForCode("be")).isEqualTo(TelegramLocaleResolver.LOCALE_RU);
    assertThat(resolveForCode("uk")).isEqualTo(TelegramLocaleResolver.LOCALE_RU);
    assertThat(resolveForCode("kk")).isEqualTo(TelegramLocaleResolver.LOCALE_RU);
  }

  @Test
  @DisplayName("Returns English locale for en, en-US language codes")
  void shouldReturnEnglishForEnglishLanguageCodes() {
    assertThat(resolveForCode("en")).isEqualTo(Locale.ENGLISH);
    assertThat(resolveForCode("en-US")).isEqualTo(Locale.ENGLISH);
    assertThat(resolveForCode("en-GB")).isEqualTo(Locale.ENGLISH);
  }

  @Test
  @DisplayName("Resolves locale correctly from CallbackQuery")
  void shouldResolveLocaleFromCallbackQuery() {
    Update update = new Update();
    CallbackQuery query = new CallbackQuery();
    User user = new User();
    user.setLanguageCode("en");
    query.setFrom(user);
    update.setCallbackQuery(query);

    assertThat(TelegramLocaleResolver.resolveLocale(update)).isEqualTo(Locale.ENGLISH);
  }

  private Locale resolveForCode(String langCode) {
    Update update = new Update();
    Message message = new Message();
    User user = new User();
    user.setLanguageCode(langCode);
    message.setFrom(user);
    update.setMessage(message);
    return TelegramLocaleResolver.resolveLocale(update);
  }
}
