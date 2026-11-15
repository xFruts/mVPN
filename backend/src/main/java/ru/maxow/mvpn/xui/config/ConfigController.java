package ru.maxow.mvpn.xui.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.maxow.mvpn.api.ConfigApi;
import ru.maxow.mvpn.subscription.SubscriptionService;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;
import ru.maxow.mvpn.xui.dto.SubscriptionConfigPayload;

@Slf4j
@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigController implements ConfigApi {

  ConfigFacade configFacade;
  SubscriptionService subscriptionService;

  String profileTitle;
  String profileDescription;
  String supportUrl;
  String profileWebPageUrl;
  String profileUpdateInterval;

  @Autowired
  public ConfigController(
      ConfigFacade configFacade,
      SubscriptionService subscriptionService,
      @Value("${vpn.config.profile-title:}") String profileTitle,
      @Value("${vpn.config.profile-description:}") String profileDescription,
      @Value("${vpn.config.support-url:}") String supportUrl,
      @Value("${vpn.config.profile-web-page-url:}") String profileWebPageUrl,
      @Value("${vpn.config.profile-update-interval:24}") String profileUpdateInterval
  ) {
    this.configFacade = configFacade;
    this.subscriptionService = subscriptionService;
    this.profileTitle = profileTitle;
    this.profileDescription = profileDescription;
    this.supportUrl = supportUrl;
    this.profileWebPageUrl = profileWebPageUrl;
    this.profileUpdateInterval = profileUpdateInterval;
  }

  @Override
  public String getConfigByVerificationCode(UUID verificationCode) {
    SubscriptionConfigPayload config = configFacade.getSubscriptionConfig(verificationCode);
    String subscriptionInfo = config.subscriptionInfo() != null
        ? config.subscriptionInfo()
        : subscriptionService.getSubscriptionInfoForUserByCode(verificationCode);

    recordDeviceAccessSafely(verificationCode);

    HttpServletResponse response = getCurrentResponse();
    response.setContentType(config.contentType() + ";charset=UTF-8");

    setHeaderIfPresent(response, "profile-title", profileTitle);
    setHeaderIfPresent(response, "profile-description", profileDescription);
    setHeaderIfPresent(response, "support-url", supportUrl);
    setHeaderIfPresent(response, "profile-web-page-url", profileWebPageUrl);
    setHeaderIfPresent(response, "profile-update-interval", profileUpdateInterval);

    response.setHeader("subscription-userinfo", subscriptionInfo);
    response.setHeader("Cache-Control", "no-store");

    return config.body();
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<String> handleNotFoundException(NotFoundException e) {
    log.warn("Resource not found: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.TEXT_PLAIN)
        .body(e.getMessage());
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<String> handleBadRequestException(BadRequestException e) {
    log.warn("Bad request received: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.TEXT_PLAIN)
        .body(e.getLocalizedMessage());
  }

  @ExceptionHandler(XuiUnavailableException.class)
  public ResponseEntity<String> handleXuiUnavailableException(XuiUnavailableException e) {
    log.warn("XUI integration backend is temporarily unreachable: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .contentType(MediaType.TEXT_PLAIN)
        .body("VPN nodes are temporarily unreachable. Please try again later.");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleException(Exception e) {
    log.error("Unexpected internal server error occurred", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.TEXT_PLAIN)
        .body("Server Error. Please try again later");
  }

  private void recordDeviceAccessSafely(UUID verificationCode) {
    if (verificationCode == null) {
      return;
    }
    try {
      HttpServletRequest request = getCurrentRequest();
      String userAgent = request != null ? request.getHeader("User-Agent") : null;
      String headerHwid = request != null ? request.getHeader("X-Hwid") : null;
      String hwid = (headerHwid != null || request == null)
          ? headerHwid
          : request.getHeader("x-hwid");
      String clientIp = extractClientIp(request);

      configFacade.recordDeviceAccess(verificationCode, userAgent, clientIp, hwid);
    } catch (Exception e) {
      log.debug("Failed to record device access for code {}: {}",
          verificationCode, e.getMessage());
    }
  }

  private HttpServletResponse getCurrentResponse() {
    ServletRequestAttributes attrs =
        (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    return attrs.getResponse();
  }

  private HttpServletRequest getCurrentRequest() {
    try {
      ServletRequestAttributes attrs =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      return attrs != null ? attrs.getRequest() : null;
    } catch (Exception e) {
      return null;
    }
  }

  private String extractClientIp(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
      return xForwardedFor.split(",")[0].trim();
    }
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isBlank()) {
      return xRealIp.trim();
    }
    return request.getRemoteAddr();
  }

  private void setHeaderIfPresent(
      HttpServletResponse response,
      String headerName,
      String value
  ) {
    if (value != null && !value.isBlank()) {
      response.setHeader(headerName, value);
    }
  }
}
