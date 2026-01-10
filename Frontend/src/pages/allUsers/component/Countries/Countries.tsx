import { useState } from "react";
import { Link, CheckSquare, Smartphone } from "lucide-react";
import styles from "./Countries.module.css";

const NameCountries = [
    "Finland",
    "USA",
    "Russia",
    "Netherlands",
    "Germany",
    "Singapore",
];

const universalLink = "https://vpn.example.com/subscription/1";
const vlessLink =
    "vless://11223344-5566-7788-9900-aabbccddeeff@newyork-01.vpn.us:443?encryption=none&security=tls&sni=newyork-01.vpn.us&type=ws&host=newyork-01.vpn.us&path=%2Fvless#USA";
const subscriptionLink =
    "https://newyork-01.vpn.us/sub/11223344-5566-7788-9900-aabbccddeeff";

interface CountriesProps {
    userId: number;
    specificCountry: string;
    firstName: string;
    lastName: string;
    countries: string[];
    onClose?: () => void;
}

const Countries: React.FC<CountriesProps> = ({
                                                 userId,
                                                 specificCountry,
                                                 firstName,
                                                 lastName,
                                                 countries,
                                                 onClose,
                                             }) => {
    const [selectedCountry, setSelectedCountry] = useState<string>(specificCountry);
    return (
        <div className={styles.countriesOverlay} onClick={onClose}>
            <div
                className={styles.countriesModal}
                onClick={(e) => e.stopPropagation()}
            >
                <div className={styles.countriesHeader}>
                    <span>VLESS конфигурации</span>
                    <span style={{ color: "gray", fontSize: "15px" }}>
                        {firstName} {lastName} (ID: {userId})
                    </span>
                </div>

                <div className={styles.countriesContent}>
                    <div className={styles.countriesContentChoose}>
                        <span style={{ fontSize: "18px" }}>
                            Выберите страну
                        </span>
                        <div className={styles.countriesContentChooseCountry}>
                            {countries.map((country, index) => (
                                <div
                                    key={country}
                                    className={`${styles.countriesContentCountry} ${
                                        selectedCountry === country ? styles.countryActive : ""
                                    }`}
                                    onClick={() => setSelectedCountry(country)}
                                >
                                    <p style={{ fontSize: "22px", margin: "0" }}>
                                        {country}
                                    </p>
                                    <p>{NameCountries[index]}</p>
                                </div>
                            ))}
                        </div>
                    </div>
                    <div className={styles.countriesContentSubscription}>
                        <span style={{ fontSize: "18px" }}>
                            Единая подписка пользователя
                        </span>
                        <div className={styles.countriesContentSubscriptionLink}>
                            <span>Универсальная ссылка на подписку</span>
                            <span style={{ fontSize: "13px" }}>
                                Одна ссылка для всех доступных стран.
                                Автоматически обновляется при изменении
                                подписки.
                            </span>
                            <div className={styles.countriesContentSubscriptionInput}>
                                <input value={universalLink} readOnly />
                                <div
                                    onClick={() =>
                                        navigator.clipboard.writeText(universalLink)
                                    }
                                >
                                    <Link size={20} />
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className={styles.countriesCustomConf}>
                        <span>
                            <span style={{ fontSize: "13px" }}>
                                {selectedCountry}
                            </span>{" "}
                            Индивидуальная конфигурация для{" "}
                            {NameCountries[countries.indexOf(selectedCountry)]}
                        </span>
                        <span>
                            Конфигурация для подключения к конкретному серверу в
                            выбранной стране.
                        </span>
                    </div>
                    <div className={styles.countriesConf}>
                        <div className={styles.countriesConfHeader}>
                            <div className={styles.vless}>VLESS</div>
                            <div className={styles.countriesConfHeaderActive}>
                                <CheckSquare size={20} /> Активна
                            </div>
                        </div>
                        <div className={styles.countriesConfVless}>
                            <span style={{ color: "gray" }}>
                                VLESS конфигурация
                            </span>
                            <div className={styles.countriesContentSubscriptionInput}>
                                <input value={vlessLink} readOnly />
                                <div
                                    onClick={() =>
                                        navigator.clipboard.writeText(vlessLink)
                                    }
                                >
                                    <Link size={20} />
                                </div>
                            </div>
                        </div>
                        <div className={styles.countriesConfVless}>
                            <span style={{ color: "gray" }}>
                                Ссылка на подписку
                            </span>
                            <div className={styles.countriesContentSubscriptionInput}>
                                <input value={subscriptionLink} readOnly />
                                <div
                                    onClick={() =>
                                        navigator.clipboard.writeText(subscriptionLink)
                                    }
                                >
                                    <Link size={20} />
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className={styles.countriesInstructions}>
                        <div>
                            <Smartphone size={20} />
                            <span>Инструкции по подключению</span>
                        </div>
                        <span style={{ color: "gray" }}>
                            <strong>Для мобильных устройств:</strong> Скопируйте
                            VLESS конфигурацию и вставьте в приложение v2rayNG
                            (Android) или Shadowrocket (iOS).
                        </span>
                        <span style={{ color: "gray" }}>
                            <strong>Для компьютера:</strong> Используйте ссылку
                            на подписку в клиентах Clash, v2rayN или
                            аналогичных.
                        </span>
                        <span style={{ color: "gray" }}>
                            <strong>Сервер:</strong> newyork-01.vpn.us
                        </span>
                    </div>
                </div>
                <div className={styles.countriesButtons}>
                    <button onClick={onClose}>Закрыть</button>
                </div>
            </div>
        </div>
    );
};

export default Countries;