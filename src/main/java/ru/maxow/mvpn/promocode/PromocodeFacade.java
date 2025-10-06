package ru.maxow.mvpn.promocode;

public interface PromocodeFacade {
  void applyPromocode(Long userId, String code);
}
