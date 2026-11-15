package ru.maxow.mvpn.telegram.util;

import java.util.Set;
import lombok.experimental.UtilityClass;

/**
 * Общие константы команд и разметки для подсистемы Telegram-бота.
 */
@UtilityClass
public class TelegramConstants {

  public static final String PARSE_MODE_MARKDOWN = "Markdown";

  // Основные команды
  public static final String CMD_START = "/start";
  public static final String CMD_HELP = "/help";
  public static final String CMD_AUTH = "/auth";

  // Команды подписки и статуса
  public static final String CMD_STATUS = "/status";
  public static final String CMD_SUBSCRIPTION = "/subscription";
  public static final String CMD_SUB = "/sub";
  public static final String CMD_MY = "/my";

  public static final Set<String> SUBSCRIPTION_COMMANDS = Set.of(
      CMD_STATUS, CMD_SUBSCRIPTION, CMD_SUB, CMD_MY
  );

  // Команды серверов
  public static final String CMD_SERVERS = "/servers";
  public static final String CMD_NODES = "/nodes";

  public static final Set<String> SERVERS_COMMANDS = Set.of(
      CMD_SERVERS, CMD_NODES
  );

  // Команды устройств
  public static final String CMD_DEVICES = "/devices";
  public static final String CMD_DEVICE = "/device";
  public static final String CMD_IPS = "/ips";
  public static final String CMD_IP = "/ip";

  public static final Set<String> DEVICES_COMMANDS = Set.of(
      CMD_DEVICES, CMD_DEVICE, CMD_IPS, CMD_IP
  );

  // Команды подключения и получения конфигураций
  public static final String CMD_CONNECT = "/connect";
  public static final String CMD_CONFIG = "/config";
  public static final String CMD_LINK = "/link";
  public static final String CMD_KEY = "/key";
  public static final String CMD_GETKEY = "/getkey";
  public static final String CMD_VLESS = "/vless";
  public static final String CMD_HAPP = "/happ";
  public static final String CMD_V2RAYTUN = "/v2raytun";

  public static final Set<String> CONNECTION_COMMANDS = Set.of(
      CMD_CONNECT, CMD_CONFIG, CMD_LINK, CMD_KEY,
      CMD_GETKEY, CMD_VLESS, CMD_HAPP, CMD_V2RAYTUN
  );
}
