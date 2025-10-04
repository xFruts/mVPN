package ru.maxow.mvpn.subscription;

public record SubscriptionRequestDto (
    Long userId,
    String configType,
    Integer durationInDays,
    String filePath,
    String configUri
) {
}
