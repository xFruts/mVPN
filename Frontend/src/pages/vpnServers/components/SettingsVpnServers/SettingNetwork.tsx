export default function SettingNetwork() {
    return(
        <>
            <div className="setting-body">
                <div className={"setting-body-input"}>
                    <span>Порт сервера</span>
                    <input
                        type={"text"}
                        className="editServer-input"
                        value={"51820"}
                        name={"port"}/>
                </div>
                <div className={"setting-body-input"}>
                    <span>Keep Alive (секунды)</span>
                    <input
                        type={"text"}
                        className="editServer-input"
                        value={"25"}
                        name={"keepAlive"}/>
                </div>
                <div className={"setting-body-input"}>
                    <span>Первичный DNS</span>
                    <input
                        type={"text"}
                        className="editServer-input"
                        value={"1.1.1.1"}
                        name={"firstDNS"}/>
                </div>
                <div className={"setting-body-input"}>
                    <span>Вторичный DNS</span>
                    <input
                        type={"text"}
                        className="editServer-input"
                        value={"400"}
                        name={"secondDNS"}/>
                </div>
                <div className={"setting-body-input"}>
                    <span>Разрешенные IP адреса</span>
                    <input
                        type={"text"}
                        className="editServer-input"
                        value={"0.0.0.0/0"}
                        name={"IP"}/>
                </div>
            </div>
        </>
    )
}