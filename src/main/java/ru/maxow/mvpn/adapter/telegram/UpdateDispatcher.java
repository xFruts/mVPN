package ru.maxow.mvpn.adapter.telegram;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Component
public class UpdateDispatcher {
  private final List<CommandHandler> handlers;

  public UpdateDispatcher(List<CommandHandler> handlers) {
    this.handlers = handlers;
  }

  public List<SendMessage> dispatch(Update update) {
    return handlers.stream()
        .filter(h -> h.supports(update))
        .findFirst()
        .map(h -> h.handle(update))
        .orElse(List.of());
  }
}
