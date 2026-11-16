package ru.maxow.mvpn.device;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * Сервис определения геолокации и провайдера (ISP) по IP-адресу.
 *
 * <p>Включает локальный LRU-кэш Caffeine, Circuit Breaker для изоляции сбоев
 * внешнего API (ip-api.com) и Rate Limiter (не более 40 запросов в минуту).
 */
@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeoIpService {

  private static final int MAX_CACHE_ENTRIES = 5000;
  private static final int CACHE_EXPIRE_HOURS = 24;
  private static final int DEFAULT_TIMEOUT_MS = 2500;
  private static final int FAILURE_THRESHOLD = 5;
  private static final long RESET_TIMEOUT_MILLIS = 60_000L;
  private static final int RATE_LIMIT_MAX_REQUESTS = 40;
  private static final long RATE_LIMIT_WINDOW_MILLIS = 60_000L;

  final String apiUrl;
  final Cache<String, Optional<GeoIpInfo>> cache;
  final RestClient restClient;

  final Object circuitBreakerLock = new Object();
  final Object rateLimiterLock = new Object();

  int consecutiveFailures = 0;
  long lastFailureTime = 0L;
  int requestCount = 0;
  long windowStartTime = 0L;

  /**
   * Основной конструктор сервиса геолокации с поддержкой таймаутов RestClient.
   *
   * @param restClientBuilder билдер HTTP-клиента
   * @param apiUrl            базовый URL сервиса геоданных
   * @param timeoutMillis     таймаут соединения и чтения в миллисекундах
   */
  @Autowired
  public GeoIpService(
      RestClient.Builder restClientBuilder,
      @Value("${geo.api-url:https://ip-api.com/json/}") String apiUrl,
      @Value("${geo.timeout-ms:2500}") int timeoutMillis
  ) {
    this.apiUrl = apiUrl;
    int effectiveTimeout = (timeoutMillis > 0) ? timeoutMillis : DEFAULT_TIMEOUT_MS;
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofMillis(effectiveTimeout));
    factory.setReadTimeout(Duration.ofMillis(effectiveTimeout));
    this.restClient = restClientBuilder
        .requestFactory(factory)
        .build();
    this.cache = Caffeine.newBuilder()
        .maximumSize(MAX_CACHE_ENTRIES)
        .expireAfterWrite(CACHE_EXPIRE_HOURS, TimeUnit.HOURS)
        .build();
  }

  GeoIpService(String apiUrl, RestClient restClient) {
    this.apiUrl = apiUrl;
    this.restClient = restClient;
    this.cache = Caffeine.newBuilder()
        .maximumSize(MAX_CACHE_ENTRIES)
        .expireAfterWrite(CACHE_EXPIRE_HOURS, TimeUnit.HOURS)
        .build();
  }

  /**
   * Выполняет поиск информации о локации и провайдере для указанного IP-адреса.
   *
   * @param ip строковое представление IPv4/IPv6-адреса
   * @return информация о геолокации или пустой Optional
   */
  public Optional<GeoIpInfo> lookup(String ip) {
    if (ip == null || ip.isBlank() || isPrivateOrLocal(ip)) {
      return Optional.empty();
    }
    String cleanIp = ip.trim();
    return cache.get(cleanIp, this::fetchFromApi);
  }

  private Optional<GeoIpInfo> fetchFromApi(String ip) {
    if (isCircuitOpen()) {
      log.warn("GeoIP circuit breaker is OPEN, fast-failing for: {}", maskIp(ip));
      return Optional.empty();
    }
    if (isRateLimited()) {
      log.warn("GeoIP rate limit exceeded (max {}/min), skipping: {}",
          RATE_LIMIT_MAX_REQUESTS, maskIp(ip));
      return Optional.empty();
    }

    try {
      String baseUrl = apiUrl.endsWith("/") ? apiUrl : apiUrl + "/";
      String query = "?lang=ru&fields=status,country,countryCode,city,isp,org,as";
      JsonNode node = restClient.get()
          .uri(baseUrl + ip + query)
          .retrieve()
          .body(JsonNode.class);

      if (node != null && "success".equalsIgnoreCase(node.path("status").asString())) {
        recordSuccess();
        String rawIsp = node.path("isp").asString("");
        String org = node.path("org").asString("");
        String isp = resolveFriendlyIsp(rawIsp, org);
        String city = node.path("city").asString("");
        String country = node.path("country").asString("");
        String countryCode = node.path("countryCode").asString("");
        return Optional.of(new GeoIpInfo(ip, isp, city, country, countryCode));
      }
      recordFailure();
    } catch (Exception e) {
      recordFailure();
      log.warn("GeoIP lookup failed for {}: {}", maskIp(ip), e.getMessage());
    }
    return Optional.empty();
  }

  private boolean isCircuitOpen() {
    synchronized (circuitBreakerLock) {
      if (consecutiveFailures < FAILURE_THRESHOLD) {
        return false;
      }
      long elapsed = System.currentTimeMillis() - lastFailureTime;
      return elapsed <= RESET_TIMEOUT_MILLIS;
    }
  }

  private void recordSuccess() {
    synchronized (circuitBreakerLock) {
      consecutiveFailures = 0;
    }
  }

  private void recordFailure() {
    synchronized (circuitBreakerLock) {
      consecutiveFailures++;
      lastFailureTime = System.currentTimeMillis();
    }
  }

  private boolean isRateLimited() {
    synchronized (rateLimiterLock) {
      long now = System.currentTimeMillis();
      if (now - windowStartTime > RATE_LIMIT_WINDOW_MILLIS) {
        windowStartTime = now;
        requestCount = 0;
      }
      if (requestCount >= RATE_LIMIT_MAX_REQUESTS) {
        return true;
      }
      requestCount++;
      return false;
    }
  }

  /**
   * Маскирует IP-адрес для предотвращения утечки PII в логи.
   *
   * @param ip исходный IP-адрес
   * @return маскированный IP-адрес (например, 192.168.1.***)
   */
  public static String maskIp(String ip) {
    if (ip == null || ip.isBlank()) {
      return "***";
    }
    int lastDot = ip.lastIndexOf('.');
    if (lastDot > 0) {
      return ip.substring(0, lastDot) + ".***";
    }
    int lastColon = ip.lastIndexOf(':');
    if (lastColon > 0) {
      return ip.substring(0, lastColon) + ":****";
    }
    return "***";
  }

  private String resolveFriendlyIsp(String rawIsp, String org) {
    String candidate = (rawIsp != null && !rawIsp.isBlank())
        ? rawIsp
        : (org != null ? org : "");
    return candidate
        .replace("Public Joint Stock Company ", "")
        .replace("PJSC ", "")
        .replace("LLC ", "")
        .replace("JSC ", "")
        .trim();
  }

  private boolean isPrivateOrLocal(String ip) {
    return ip.equals("127.0.0.1")
        || ip.equals("localhost")
        || ip.equals("::1")
        || ip.startsWith("10.")
        || ip.startsWith("192.168.")
        || (ip.startsWith("172.") && isPrivate172(ip));
  }

  /**
   * Проверяет, входит ли IP-адрес в приватный диапазон по RFC 1918 (172.16.0.0/12).
   *
   * @param ip IPv4-адрес, начинающийся на "172."
   * @return true, если второй октет находится в диапазоне 16..31
   */
  private boolean isPrivate172(String ip) {
    try {
      String[] parts = ip.split("\\.");
      int second = Integer.parseInt(parts[1]);
      return second >= 16 && second <= 31;
    } catch (Exception e) {
      return false;
    }
  }
}
