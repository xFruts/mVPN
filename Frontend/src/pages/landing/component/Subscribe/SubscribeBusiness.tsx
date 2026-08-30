import styles from "./Subscribe.module.css"
import { Crown, MessageCircle } from "lucide-react";
import { useScrollReveal } from "@pages/shared/useScrollReveal.tsx";
import { useState } from "react";

type UserOption = 5000 | 7500 | 10000 | 15000;

const usersDict = {
    5000 : 0,
    7500 : 2000,
    10000 : 4000,
    15000: 8000
}

export default function SubscribeBusiness() {
    const [ref, isVisible] = useScrollReveal();
    const [users, setUsers] = useState<UserOption>(5000);
    const [support, setSupport] = useState(false);
    const [branding, setBranding] = useState(false);
    const [chat, setChat] = useState(false);

    const price = 4990 + usersDict[users] + (support ? 2000 : 0) + (branding ? 1500 : 0) + (chat ? 1000 : 0);

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
                    Запустите свой VPN-сервис с нашей мощной панелью. Выберите
                    подходящие лимиты и опции для вашей инфраструктуры.
                </span>
            </div>
            <div className={styles.content}>
                <div className={styles.settings}>
                    <div className={styles.settingsHeader}>
                        <span className={styles.settingsText}>
                            Создайте свой тарифный план
                        </span>
                        <span className={styles.settingsDesc}>
                            Раскройте весь потенциал mVPN и начните свой
                            собственный VPN-бизнес с лучшей в отрасли
                            производительностью и безопасностью.
                        </span>
                    </div>
                    <div className={styles.settingsUsers}>
                        <div className={styles.usersNumber}>
                            <span className={styles.locationNumber}>
                                {users}
                            </span>
                            <span className={styles.locationText}>
                                ПОЛЬЗОВАТЕЛЕЙ ВАШЕГО VPN
                            </span>
                        </div>
                        <div className={styles.usersChange}>
                            <span>Выберите количество пользователей:</span>
                            <div className={styles.usersButton}>
                                <div
                                    className={`${styles.count} ${users === 5000 ? `${styles.active}` : ""}`}
                                    onClick={() => setUsers(5000)}
                                >
                                    5000
                                </div>
                                <div
                                    className={`${styles.count} ${users === 7500 ? `${styles.active}` : ""}`}
                                    onClick={() => setUsers(7500)}
                                >
                                    7500
                                </div>
                                <div
                                    className={`${styles.count} ${users === 10000 ? `${styles.active}` : ""}`}
                                    onClick={() => setUsers(10000)}
                                >
                                    10000
                                </div>
                                <div
                                    className={`${styles.count} ${users === 15000 ? `${styles.active}` : ""}`}
                                    onClick={() => setUsers(15000)}
                                >
                                    15000
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className={styles.settingsOption}>
                        <span className={styles.optionText}>
                            Выберите опции (за дополнительную плату):
                        </span>
                        <label className={styles.option}>
                            <input
                                type="checkbox"
                                name="vpn-option"
                                className={styles.realRadio}
                                checked={true}
                                onChange={() => {}}
                            />
                            <span className={styles.customRadio}></span>
                            <span className={`${styles.inActive}`}>
                                Уникальная система защиты от запретов — 0 ₽ /мес
                            </span>
                        </label>
                        <label className={styles.option}>
                            <input
                                type="checkbox"
                                name="white-list"
                                className={styles.realRadio}
                                checked={true}
                                onChange={() => {}}
                            />
                            <span className={styles.customRadio}></span>
                            <span className={`${styles.inActive}`}>
                                Telegram-бот для управления вашим VPN-сервисом —
                                0 ₽ /мес
                            </span>
                        </label>
                        <label className={styles.option}>
                            <input
                                type="checkbox"
                                name="white-list"
                                className={styles.realRadio}
                                checked={support}
                                onChange={() => {
                                    setSupport(!support);
                                }}
                            />
                            <span className={styles.customRadio}></span>
                            <span
                                className={`${styles.optionDesc} ${!support ? `${styles.inActive}` : ""}`}
                            >
                                Приоритетная техническая поддержка 24/7 — +2 000
                                ₽ /мес
                            </span>
                        </label>
                        <label className={styles.option}>
                            <input
                                type="checkbox"
                                name="white-list"
                                className={styles.realRadio}
                                checked={branding}
                                onChange={() => {
                                    setBranding(!branding);
                                }}
                            />
                            <span className={styles.customRadio}></span>
                            <span
                                className={`${styles.optionDesc} ${!branding ? `${styles.inActive}` : ""}`}
                            >
                                Возможность брендирования вашего VPN-сервиса
                                (White-label) — +1 500 ₽ /мес
                            </span>
                        </label>
                        <label className={styles.option}>
                            <input
                                type="checkbox"
                                name="white-list"
                                className={styles.realRadio}
                                checked={chat}
                                onChange={() => {
                                    setChat(!chat);
                                }}
                            />
                            <span className={styles.customRadio}></span>
                            <span
                                className={`${styles.optionDesc} ${!chat ? `${styles.inActive}` : ""}`}
                            >
                                Приватный чат в Telegram для владельцев бизнеса
                                — +1 000 ₽ /мес
                            </span>
                        </label>
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
                            <span>Business Pro</span>
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
                                Полный контроль над пользователями и аналитикой
                                сессий
                            </li>
                            <li>
                                Создайте собственную страницу с информацией для
                                клиентов
                            </li>
                            <li>
                                Управляйте своим VPN-сервисом в одном
                                Telegram-боте
                            </li>
                            <li>
                                Уникальная система защиты от блокировок и DPI
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    );
}