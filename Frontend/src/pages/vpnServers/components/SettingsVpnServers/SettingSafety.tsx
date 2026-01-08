import {Lock} from "lucide-react";

export default function SettingSafety() {
    return(
        <>
            <div className={"setting-safety"}>
                <div className={"setting-safety-title"}>
                    <Lock size={20}/>
                    <span>Безопасность</span>
                </div>
                <span>Настройки безопасности влияют на все подключения к серверу. Изменяйте их осторожно.</span>
            </div>
            <div className={"setting-checkbox"}>
                <div className={"setting-checkbox-header"}>
                    <span>Методы шифрования</span>
                </div>
                <div className={"setting-checkbox-item"}>
                    <input type={"checkbox"}/>
                    <div className={"setting-checkbox-text"}>
                        <span>ChaCha20Poly1305</span>
                    </div>
                </div>
                <div className={"setting-checkbox-item"}>
                    <input type={"checkbox"}/>
                    <div className={"setting-checkbox-text"}>
                        <span>AES256-GCM</span>
                    </div>
                </div>
            </div>
            <div className={"setting-checkbox"}>
                <div className={"setting-checkbox-header"}>
                    <span>Дополнительные настройки</span>
                </div>
                <div className={"setting-checkbox-item"}>
                    <input type={"checkbox"}/>
                    <div className={"setting-checkbox-text"}>
                        <span>Блокировка подозрительного трафика</span>
                    </div>
                </div>
                <div className={"setting-checkbox-item"}>
                    <input type={"checkbox"}/>
                    <div className={"setting-checkbox-text"}>
                        <span>Ограничение скорости подключения</span>
                    </div>

                </div>
            </div>
        </>
    )
}