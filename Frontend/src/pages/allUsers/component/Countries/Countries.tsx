import {useState} from "react";
import { Link, CheckSquare, Smartphone } from "lucide-react"
import "./Countries.css";

const NameCountries = [
    "Finland",
    "USA",
    "Russia",
    "Netherlands",
    "Germany",
    "Singapore"
]

const universalLink = "https://vpn.example.com/subscription/1"
const vlessLink = "vless://11223344-5566-7788-9900-aabbccddeeff@newyork-01.vpn.us:443?encryption=none&security=tls&sni=newyork-01.vpn.us&type=ws&host=newyork-01.vpn.us&path=%2Fvless#USA";
const subscriptionLink = "https://newyork-01.vpn.us/sub/11223344-5566-7788-9900-aabbccddeeff"

interface CountriesProps {
    userId: number;
    specificCountry: string;
    firstName: string;
    lastName: string;
    countries: string[];
    onClose?: () => void;
}

const Countries: React.FC<CountriesProps> = ({ userId, specificCountry, firstName, lastName, countries, onClose }) => {
    const [ selectedCountry, setSelectedCountry ] = useState<string>(specificCountry);
    return (
        <div className="countries-overlay" onClick={onClose}>
            <div className="countries-modal" onClick={(e) => e.stopPropagation()}>
                <div className="countries-header">
                    <span>VLESS конфигурации</span>
                    <span style = {{color: "gray", fontSize: "15px"}}>{firstName} {lastName} (ID: {userId})</span>
                </div>

                <div className="countries-content">
                    <div className="countries-content-choose">
                        <span style = {{fontSize: "18px"}}>Выберите страну</span>
                        <div className="countries-content-choose-country">
                            {countries.map((country, index) => (
                                <div className={`countries-content-country ${selectedCountry === country ? `countryActive` : ``}`} onClick = {() => setSelectedCountry(country)}>
                                    <p style={{fontSize: "22px", margin: "0"}}>{country}</p>
                                    <p>{NameCountries[index]}</p>
                                </div>
                            ))}
                        </div>
                    </div>
                    <div className="countries-content-subscription">
                        <span style={{fontSize: "18px"}}>Единая подписка пользователя</span>
                        <div className="countries-content-subscription-link">
                            <span>Универсальная ссылка на подписку</span>
                            <span style={{fontSize: "13px"}}>Одна ссылка для всех доступных стран. Автоматически обновляется при изменении подписки.</span>
                            <div className="countries-content-subscription-input">
                                <input value={universalLink}></input>
                                <div onClick={() => navigator.clipboard.writeText(universalLink)}><Link size={20}/></div>
                            </div>
                        </div>
                    </div>
                    <div className="countries-custom-conf">
                        <span><span style={{fontSize: "13px"}}>{selectedCountry}</span> Индивидуальная конфигурация для {NameCountries[countries.indexOf(selectedCountry)]}</span>
                        <span>Конфигурация для подключения к конкретному серверу в выбранной стране.</span>
                    </div>
                    <div className="countries-conf">
                        <div className="countries-conf-header">
                            <div className={"vless"}>VLESS</div>
                            <div className={"countries-conf-header-active"}><CheckSquare size={20}/> Активна</div>
                        </div>
                        <div className="countries-conf-vless">
                            <span style={{color: "gray"}}>VLESS конфигурация</span>
                            <div className="countries-content-subscription-input">
                                <input value={vlessLink}></input>
                                <div onClick={() => navigator.clipboard.writeText(vlessLink)}><Link size={20}/></div>
                            </div>
                        </div>
                        <div className="countries-conf-vless">
                            <span style={{color: "gray"}}>Ссылка на подписку</span>
                            <div className="countries-content-subscription-input">
                                <input value={subscriptionLink}></input>
                                <div onClick={() => navigator.clipboard.writeText(subscriptionLink)}><Link size={20}/></div>
                            </div>
                        </div>
                    </div>
                    <div className="countries-instructions">
                        <div>
                            <Smartphone size={20}/>
                            <span>Инструкции по подключению</span>
                        </div>
                        <span style={{color: "gray"}}><strong>Для мобильных устройств:</strong> Скопируйте VLESS конфигурацию и вставьте в приложение v2rayNG (Android) или Shadowrocket (iOS).</span>
                        <span style={{color: "gray"}}><strong>Для компьютера:</strong> Используйте ссылку на подписку в клиентах Clash, v2rayN или аналогичных.</span>
                        <span style={{color: "gray"}}><strong>Сервер:</strong> newyork-01.vpn.us</span>
                    </div>
                </div>
                <div className="countries-buttons">
                    <button onClick={onClose}>Закрыть</button>
                </div>
            </div>
        </div>
    );
};

export default Countries;