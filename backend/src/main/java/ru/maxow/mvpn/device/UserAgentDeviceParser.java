package ru.maxow.mvpn.device;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Парсер заголовка User-Agent для распознавания клиента и версии операционной системы.
 */
@Component
public class UserAgentDeviceParser {

  private static final Pattern HAPP_PATTERN = Pattern.compile(
      "^Happ/([\\d.]+)(?:/([^/]+))?(?:/([^/]+))?(?:/([^/]+))?",
      Pattern.CASE_INSENSITIVE
  );
  private static final Pattern V2RAY_PATTERN = Pattern.compile(
      "(v2ray[A-Za-z]+)/([\\d.]+)(?:\\s*\\(([^)]+)\\))?",
      Pattern.CASE_INSENSITIVE
  );
  private static final Pattern STREISAND_PATTERN = Pattern.compile(
      "Streisand/([\\d.]+)(?:\\s*\\(([^)]+)\\))?",
      Pattern.CASE_INSENSITIVE
  );
  private static final Pattern CLASH_PATTERN = Pattern.compile(
      "(Clash[A-Za-z0-9_-]*)/([\\d.]+)",
      Pattern.CASE_INSENSITIVE
  );
  private static final Pattern SHADOWROCKET_PATTERN = Pattern.compile(
      "Shadowrocket/([\\d.]+)",
      Pattern.CASE_INSENSITIVE
  );
  private static final Pattern HIDDIFY_PATTERN = Pattern.compile(
      "Hiddify[A-Za-z]*/([\\d.]+)",
      Pattern.CASE_INSENSITIVE
  );
  private static final Pattern SINGBOX_PATTERN = Pattern.compile(
      "(sing-box|nekobox)[A-Za-z0-9_-]*/([\\d.]+)",
      Pattern.CASE_INSENSITIVE
  );

  private static final List<String> KNOWN_CLIENT_PREFIXES = List.of(
      "happ", "v2ray", "streisand", "clash", "shadowrocket",
      "hiddify", "sing-box", "nekobox"
  );
  private static final List<String> BROWSER_KEYWORDS = List.of(
      "chrome", "firefox", "safari", "edge", "opera", "curl"
  );

  private static final String DEFAULT_CLIENT = "VPN Client";
  private static final String DEFAULT_OS = "Unknown";

  /**
   * Разбирает строку User-Agent клиента и извлекает информацию об ОС и модели устройства.
   *
   * @param rawUserAgent исходная строка HTTP-заголовка User-Agent
   * @return распарсенные данные устройства
   */
  public ParsedDeviceInfo parse(String rawUserAgent) {
    if (rawUserAgent == null || rawUserAgent.isBlank()) {
      return new ParsedDeviceInfo(DEFAULT_CLIENT, DEFAULT_OS, DEFAULT_OS, "");
    }
    String ua = rawUserAgent.trim();

    Matcher happMatcher = HAPP_PATTERN.matcher(ua);
    if (happMatcher.find()) {
      return parseHapp(happMatcher, ua);
    }
    Matcher v2rayMatcher = V2RAY_PATTERN.matcher(ua);
    if (v2rayMatcher.find()) {
      return parseV2ray(v2rayMatcher, ua);
    }
    Matcher streisandMatcher = STREISAND_PATTERN.matcher(ua);
    if (streisandMatcher.find()) {
      return parseStreisand(streisandMatcher, ua);
    }
    Matcher clashMatcher = CLASH_PATTERN.matcher(ua);
    if (clashMatcher.find()) {
      String os = detectOsFromBrowser(ua);
      return new ParsedDeviceInfo(
          clashMatcher.group(1) + " " + clashMatcher.group(2),
          os, os, ua
      );
    }
    Matcher shadowrocketMatcher = SHADOWROCKET_PATTERN.matcher(ua);
    if (shadowrocketMatcher.find()) {
      return new ParsedDeviceInfo(
          "Shadowrocket " + shadowrocketMatcher.group(1),
          "iOS", "iOS", ua
      );
    }
    Matcher hiddifyMatcher = HIDDIFY_PATTERN.matcher(ua);
    if (hiddifyMatcher.find()) {
      String os = detectOsFromBrowser(ua);
      return new ParsedDeviceInfo(
          "Hiddify " + hiddifyMatcher.group(1),
          os, os, ua
      );
    }
    Matcher singboxMatcher = SINGBOX_PATTERN.matcher(ua);
    if (singboxMatcher.find()) {
      String os = detectOsFromBrowser(ua);
      return new ParsedDeviceInfo(
          singboxMatcher.group(1) + " " + singboxMatcher.group(2),
          os, os, ua
      );
    }
    return parseBrowserOrGeneric(ua);
  }

  /**
   * Проверяет, является ли запрос обращением из браузера или превью-бота.
   */
  public boolean isBrowserOrCrawler(String clientApp, String rawUa) {
    if (rawUa != null) {
      String lowerUa = rawUa.toLowerCase();
      if (lowerUa.contains("telegrambot")
          || lowerUa.contains("twitterbot")
          || lowerUa.contains("facebookexternalhit")
          || lowerUa.contains("curl/")
          || lowerUa.contains("wget/")) {
        return true;
      }
    }
    if (clientApp == null) {
      return false;
    }
    String lower = clientApp.toLowerCase();
    for (String browser : BROWSER_KEYWORDS) {
      if (lower.contains(browser)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Извлекает семейство клиента из строки приложения для сопоставления.
   *
   * @param clientApp название клиентского приложения
   * @return нормализованный строковый идентификатор семейства
   */
  public String extractClientFamily(String clientApp) {
    if (clientApp == null || clientApp.isBlank()) {
      return "vpn-client";
    }
    String lower = clientApp.trim().toLowerCase();
    for (String prefix : KNOWN_CLIENT_PREFIXES) {
      if (lower.startsWith(prefix)) {
        return prefix;
      }
    }
    for (String browser : BROWSER_KEYWORDS) {
      if (lower.contains(browser)) {
        return browser;
      }
    }
    int spaceIdx = clientApp.indexOf(' ');
    if (spaceIdx > 0) {
      return clientApp.substring(0, spaceIdx).toLowerCase();
    }
    return lower;
  }

  private ParsedDeviceInfo parseHapp(Matcher matcher, String ua) {
    String version = matcher.group(1);
    String targetOs = matcher.group(2);
    String modelOrBuild = matcher.group(3);
    String extra = matcher.group(4);

    String clientApp = "Happ " + version;
    if (targetOs == null) {
      String os = detectOsFromBrowser(ua);
      return new ParsedDeviceInfo(clientApp, os, os, ua);
    }

    String osLower = targetOs.toLowerCase();
    if (osLower.equals("windows")) {
      String os = resolveWindowsVersion(modelOrBuild);
      return new ParsedDeviceInfo(clientApp, os, os, ua);
    }
    if (osLower.startsWith("ios")) {
      String os = (extra != null && !extra.isBlank()) ? "iOS " + extra : "iOS";
      return new ParsedDeviceInfo(clientApp, os, os, ua);
    }
    if (osLower.equals("android")) {
      String os = (modelOrBuild != null && !modelOrBuild.isBlank())
          ? "Android " + modelOrBuild
          : "Android";
      return new ParsedDeviceInfo(clientApp, os, os, ua);
    }
    if (osLower.equals("macos")) {
      String os = (modelOrBuild != null && !modelOrBuild.isBlank())
          ? "macOS " + modelOrBuild
          : "macOS";
      return new ParsedDeviceInfo(clientApp, os, os, ua);
    }

    String os = detectOsFromBrowser(ua);
    if (DEFAULT_OS.equals(os)) {
      os = targetOs;
    }
    return new ParsedDeviceInfo(clientApp, os, os, ua);
  }

  private ParsedDeviceInfo parseV2ray(Matcher matcher, String ua) {
    String clientName = matcher.group(1);
    String version = matcher.group(2);
    String inner = matcher.group(3);

    String osName = detectOsFromBrowser(ua);
    if (osName.equals(DEFAULT_OS)) {
      osName = "Android";
    }

    if (inner != null) {
      for (String token : inner.split(";")) {
        String t = token.trim();
        String lowerT = t.toLowerCase();
        if (lowerT.contains("android")) {
          osName = t;
        } else if (lowerT.contains("windows")) {
          osName = "Windows";
        } else if (lowerT.contains("ios")) {
          osName = "iOS";
        }
      }
    }
    return new ParsedDeviceInfo(clientName + " " + version, osName, osName, ua);
  }

  private ParsedDeviceInfo parseStreisand(Matcher matcher, String ua) {
    String version = matcher.group(1);
    String inner = matcher.group(2);
    String osName = "iOS";

    if (inner != null) {
      for (String token : inner.split(";")) {
        String t = token.trim();
        if (t.toLowerCase().contains("ios")) {
          osName = t;
        }
      }
    }
    return new ParsedDeviceInfo("Streisand " + version, osName, osName, ua);
  }

  private ParsedDeviceInfo parseBrowserOrGeneric(String ua) {
    String osName = detectOsFromBrowser(ua);
    String client = detectBrowserName(ua);
    return new ParsedDeviceInfo(client, osName, osName, ua);
  }

  private String detectOsFromBrowser(String ua) {
    if (ua.contains("Windows NT 10.0") || ua.contains("Windows")) {
      return "Windows";
    }
    if (ua.contains("iPhone") || ua.contains("iPad")) {
      return "iOS";
    }
    if (ua.contains("Android")) {
      return "Android";
    }
    if (ua.contains("Macintosh") || ua.contains("Mac OS X")) {
      return "macOS";
    }
    if (ua.contains("Linux")) {
      return "Linux";
    }
    return DEFAULT_OS;
  }

  private String detectBrowserName(String ua) {
    if (ua.contains("Chrome/")) {
      return "Google Chrome";
    }
    if (ua.contains("Firefox/")) {
      return "Mozilla Firefox";
    }
    if (ua.contains("Safari/") && !ua.contains("Chrome/")) {
      return "Safari";
    }
    if (ua.contains("curl/")) {
      return "curl";
    }
    return DEFAULT_CLIENT;
  }

  private String resolveWindowsVersion(String buildOrVersion) {
    if (buildOrVersion == null) {
      return "Windows 11";
    }
    try {
      long build = Long.parseLong(buildOrVersion.replaceAll("[^0-9]", ""));
      if (build >= 2610000000000L || (build >= 26000 && build < 1000000)) {
        return "Windows 11 (24H2)";
      }
      if (build >= 22000) {
        return "Windows 11";
      }
      if (build >= 10240) {
        return "Windows 10";
      }
    } catch (Exception ignored) {
      // Игнорируем нечисловые суффиксы сборки
    }
    return "Windows 11";
  }
}
