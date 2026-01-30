package ru.maxow.mvpn.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.maxow.mvpn.telegram.adapter.VpnBot;

/**
 * Configuration class for setting up the Telegram Bots API.
 */
@Configuration
@ConditionalOnProperty(prefix = "bot", name = "enabled", havingValue = "true")
public class TelegramConfiguration {
  /**
   * Creates and configures the TelegramBotsApi bean.
   *
   * @param telegramBot the VPNBot instance to be registered with the API
   * @return a configured instance of TelegramBotsApi
   * @throws TelegramApiException if there is an error during bot registration
   */
  @Bean
  public TelegramBotsApi telegramBotsApi(VpnBot telegramBot) throws TelegramApiException {
    var api = new TelegramBotsApi(DefaultBotSession.class);
    api.registerBot(telegramBot);
    return api;
  }
}
