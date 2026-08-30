import styles from "./Subscribe.module.css"
import { Crown, MessageCircle } from "lucide-react";
import { useScrollReveal } from "@pages/shared/useScrollReveal.tsx";
import { useState } from "react";

export default function SubscribeUser() {
    const [ref, isVisible] = useScrollReveal();
    const [vpn, setVps] = useState(true);
    const [whiteList, setWhiteList] = useState(false);
    const [devices, setDevices] = useState(5);

    const vpnPrice = vpn ? whiteList ? "100" : "125" : "+100"
    const listPrice = whiteList ? (vpn ? "125" : "150") : "+125";

    const price = (vpn ? (whiteList ? 225 : 125) : 150) + (devices === 10 ? 50 : 0);

    const handleChangeVpn = () => {
        if (vpn && !whiteList) {
            return;
        }
        setVps(!vpn);
    }

    const handleChangeList = () => {
        if (whiteList && !vpn) {
            return;
        }
        setWhiteList(!whiteList);
    };

    return (
        <div
            ref={ref}
            id="subscribe-section"
            className={`${styles.subscribe} ${isVisible ? styles.visible : ""}`}
        >
            <div className={styles.header}>
                <span className={styles.headerText}>
                    Гибкие и Доступные планы для Людей и Компаний
                </span>
                <span className={styles.headerDesc}>
                    Независимо от того, являетесь ли вы обычным пользователем
                    или нуждаетесь в защите корпоративной сети, выберите тариф,
                    который подходит вам лучше всего — безопасный, быстрый и
                    надежный.
                </span>
            </div>
            <div className={styles.content}>
                <div className={styles.settings}>
                    <div className={styles.settingsHeader}>
                        <span className={styles.settingsText}>
                            Настройте свой доступ
                        </span>
                        <span className={styles.settingsDesc}>
                            Раскройте весь потенциал mVPN и обеспечьте себе
                            безопасный и свободный интернет с лучшей
                            производительностью.
                        </span>
                    </div>
                    <div className={styles.settingsLocation}>
                        <span className={styles.locationNumber}>50+</span>
                        <div
                            style={{ display: "flex", flexDirection: "column" }}
                        >
                            <span className={styles.locationText}>
                                ЛОКАЦИЙ СЕРВЕРОВ
                            </span>
                            <span className={styles.locationText}>
                                ПО ВСЕМУ МИРУ
                            </span>
                        </div>
                    </div>
                    <div className={styles.settingsOption}>
                        <span className={styles.optionText}>
                            Выберите опции:
                        </span>
                        <label className={styles.option}>
                            <input
                                type="checkbox"
                                name="vpn-option"
                                className={styles.realRadio}
                                checked={vpn}
                                onChange={handleChangeVpn}
                            />
                            <span className={styles.customRadio}></span>
                            <span
                                className={`${styles.optionDesc} ${!vpn ? `${styles.inActive}` : ""}`}
                            >
                                Глобальный VPN доступ — {vpnPrice} ₽ /мес
                            </span>
                        </label>
                        <label className={styles.option}>
                            <input
                                type="checkbox"
                                name="white-list"
                                className={styles.realRadio}
                                checked={whiteList}
                                onChange={handleChangeList}
                            />
                            <span className={styles.customRadio}></span>
                            <span
                                className={`${styles.optionDesc} ${!whiteList ? `${styles.inActive}` : ""}`}
                            >
                                Умные обходы (Белые списки) — {listPrice} ₽ /мес
                            </span>
                        </label>
                    </div>
                    <div className={styles.settingsFooter}>
                        <span>Количество устройств</span>
                        <div className={styles.footerCount}>
                            <div
                                className={`${styles.count} ${devices === 5 ? `${styles.active}` : ""}`}
                                onClick={() => setDevices(5)}
                            >
                                5 <span className={styles.dev}>устройств</span>
                            </div>
                            <div
                                className={`${styles.count} ${devices === 10 ? styles.active : ""}`}
                                onClick={() => setDevices(10)}
                            >
                                10 <span className={styles.dev}>устройств</span>
                            </div>
                        </div>
                    </div>
                </div>
                <div className={styles.prepare}>
                    <div className={styles.prepareHeader}>
                        <Crown size={40} color={"#06b6d4"} />
                        <div className={styles.priceMonth}>
                            <span className={styles.price}>{price} ₽</span>
                            <span className={styles.month}>В МЕСЯЦ</span>
                        </div>
                    </div>
                    <div className={styles.prepareContent}>
                        <div className={styles.contentType}>
                            <span>Premium</span>
                            <span className={styles.whiteList}>
                                {whiteList
                                    ? vpn
                                        ? "+ Белые списки"
                                        : "Только Белые списки"
                                    : ""}
                            </span>
                        </div>
                        <a
                            href="https://t.me/xFruts"
                            target="_blank"
                            rel="noopener noreferrer"
                            className={styles.preparebutton}
                        >
                            <MessageCircle size={16} /> Оформить через Telegram
                        </a>
                        <ul className={styles.prepareDesc}>
                            <li>
                                Высокоскоростные серверы до 1 Гбит/с на
                                современных протоколах (VLESS, Trojan)
                            </li>
                            <li>
                                Безлимитный трафик для стриминга, скачивания и
                                игр без задержек
                            </li>
                            <li>
                                Поддержка до {devices} устройств одновременно на
                                один аккаунт
                            </li>
                            <li>
                                {vpn
                                    ? whiteList
                                        ? "Умная маршрутизация или глобальный VPN для всего трафика"
                                        : "Глобальная маршрутизация всего трафика через защищенный туннель"
                                    : "Умная маршрутизация: VPN применяется только к недоступным ресурсам"}
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    );
}