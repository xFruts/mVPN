package ru.maxow.mvpn.device;

/**
 * Результат регистрации обращения устройства.
 *
 * @param device      запись устройства
 * @param isNewDevice true, если устройство зарегистрировано впервые
 */
public record DeviceRecordResult(
    UserDevice device,
    boolean isNewDevice
) {
}
