package ru.maxow.mvpn.adapter.telegram;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public interface CommandHandler {
  boolean supports(Update update);
  List<SendMessage> handle(Update update);
}
