export default function SettingMain() {
    return (
        <>
            <div className="setting-body">
                <div className={"setting-body-input"}>
                    <span>Максимум пользователей</span>
                    <input
                        type={"number"}
                        className="editServer-input"
                        value={"400"}
                        name={"maxUsers"}/>
                </div>
                <div className={"setting-body-input"}>
                    <span>Пропускная способность</span>
                    <div className="editServer-input">
                        <select>
                            <option value={"100 Mbps"}>100 Mbps</option>
                            <option value={"500 Mbps"}>500 Mbps</option>
                            <option value={"1 Gbps"}>1 Gbps</option>
                            <option value={"10 Gbps"}>10 Gbps</option>
                        </select>
                    </div>
                </div>
                <div className={"setting-body-input"}>
                    <span>Уровень логирования</span>
                    <div className="editServer-input">
                        <select>
                            <option value={"Debug"}>Debug</option>
                            <option value={"Info"}>Info</option>
                            <option value={"Warning"}>Warning</option>
                            <option value={"Error"}>Error</option>
                        </select>
                    </div>
                </div>
                <div className={"setting-body-input"}></div>
            </div>
            <div className={"setting-checkbox"}>
                <div className={"setting-checkbox-item"}>
                    <input type={"checkbox"}/>
                    <div className={"setting-checkbox-text"}>
                        <span style={{color: "black"}}>Автоматическая перезагрузка</span>
                        <span>Перезагружать сервер при критических ошибках</span>
                    </div>
                </div>
            </div>
        </>
    )
}