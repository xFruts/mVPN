package ru.maxow.mvpn.handlers;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public interface CommandHandler {
  boolean supports(Update update);
  List<SendMessage> handle(Update update);
}
