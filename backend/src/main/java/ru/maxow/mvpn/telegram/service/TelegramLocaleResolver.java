package ru.maxow.mvpn.telegram.service;

import java.util.Locale;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

/**
 * Resolves the user's preferred locale based on incoming Telegram update metadata.
 * Defaults to Russian if the user's language code is absent, Russian, or a CIS locale.
 */
public final class TelegramLocaleResolver {

  public static final Locale LOCALE_RU = Locale.forLanguageTag("ru");
  public static final Locale LOCALE_EN = Locale.ENGLISH;

  private TelegramLocaleResolver() {}

  /**
   * Resolves the Locale from an incoming Telegram update.
   *
   * @param update the incoming update
   * @return resolved Locale
   */
  public static Locale resolveLocale(Update update) {
    if (update == null) {
      return LOCALE_RU;
    }

    User from = null;
    if (update.hasMessage() && update.getMessage().getFrom() != null) {
      from = update.getMessage().getFrom();
    } else if (update.hasCallbackQuery() && update.getCallbackQuery().getFrom() != null) {
      from = update.getCallbackQuery().getFrom();
    }

    if (from != null && from.getLanguageCode() != null && !from.getLanguageCode().isBlank()) {
      String code = from.getLanguageCode().trim().toLowerCase(Locale.ROOT);
      if (code.startsWith("ru") || code.startsWith("be")
          || code.startsWith("kk") || code.startsWith("uk")) {
        return LOCALE_RU;
      }
      if (code.startsWith("en")) {
        return LOCALE_EN;
      }
      return Locale.forLanguageTag(code.contains("-") ? code.split("-")[0] : code);
    }

    return LOCALE_RU;
  }
}
