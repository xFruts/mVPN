package ru.maxow.mvpn.xui.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.maxow.mvpn.api.ConfigApi;
import ru.maxow.mvpn.subscription.SubscriptionService;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.xui.SubscriptionConfigPayload;
import java.util.UUID;

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

  public ConfigController(
      ConfigFacade configFacade,
      SubscriptionService subscriptionService,
      @Value("${vpn.config.profile-title}")
      String profileTitle,
      @Value("${vpn.config.profile-description}")
      String profileDescription,
      @Value("${vpn.config.support-url}")
      String supportUrl,
      @Value("${vpn.config.profile-web-page-url}")
      String profileWebPageUrl,
      @Value("${vpn.config.profile-update-interval}")
      String profileUpdateInterval
  ) {
    this.configFacade = configFacade;
    this.subscriptionService = subscriptionService;
    this.profileTitle = profileTitle;
    this.profileDescription = profileDescription;
    this.supportUrl = supportUrl;
    this.profileWebPageUrl = profileWebPageUrl;
    this.profileUpdateInterval = profileUpdateInterval;
  }

  public String getConfigByVerificationCode(UUID verificationCode) {
    SubscriptionConfigPayload config = configFacade.getSubscriptionConfig(verificationCode);
    String subscriptionInfo = subscriptionService
        .getSubscriptionInfoForUserByCode(verificationCode);

    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes != null) {
      HttpServletResponse response = attributes.getResponse();
      if (response != null) {
        response.setContentType(config.contentType() + ";charset=UTF-8");

        if (profileTitle != null) {
          response.setHeader(
              "profile-title",
              profileTitle
          );
        }
        if (profileDescription != null) {
          response.setHeader(
              "profile-description",
              profileDescription
          );
        }
        if (supportUrl != null) {
          response.setHeader("support-url", supportUrl);
        }
        if (profileWebPageUrl != null) {
          response.setHeader("profile-web-page-url",
              profileWebPageUrl);
        }
        if (profileUpdateInterval != null) {
          response.setHeader("profile-update-interval", profileUpdateInterval);
        }

        response.setHeader("subscription-userinfo", subscriptionInfo);

        response.setHeader("Cache-Control", "no-store");
      }
    }
    return config.body();
  }

  @SuppressWarnings("unused")
  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<String> handleNotFoundException(NotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.TEXT_PLAIN)
        .body(e.getMessage());
  }

  @SuppressWarnings("unused")
  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<String> handleBadRequestException(BadRequestException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.TEXT_PLAIN)
        .body(e.getLocalizedMessage());
  }

  @SuppressWarnings("unused")
  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleException(Exception e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.TEXT_PLAIN)
        .body("Server Error. Please try again later");
  }
}
