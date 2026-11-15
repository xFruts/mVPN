package ru.maxow.mvpn.device;

/**
 * Сведения о географическом положении и операторе связи (ISP) по IP-адресу.
 */
public record GeoIpInfo(
    String ip,
    String isp,
    String city,
    String country,
    String countryCode
) {

  /**
   * Форматирует читаемую строку локации и провайдера устройства.
   *
   * @return отформатированная строка локации
   */
  public String formatLocation() {
    StringBuilder sb = new StringBuilder();
    if (isp != null && !isp.isBlank()) {
      sb.append(isp);
    }
    if (city != null && !city.isBlank()) {
      if (!sb.isEmpty()) {
        sb.append(" (").append(city).append(")");
      } else {
        sb.append(city);
      }
    } else if (country != null && !country.isBlank()) {
      if (!sb.isEmpty()) {
        sb.append(" (").append(country).append(")");
      } else {
        sb.append(country);
      }
    }
    return sb.toString();
  }
}
