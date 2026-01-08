export default function SettingMonitoring() {
    return(
        <>
            <div className={"setting-checkbox"}>
                <div className={"setting-checkbox-item"}>
                    <input type={"checkbox"}/>
                    <div className={"setting-checkbox-text"}>
                        <span style={{color: "black"}}>Включить мониторинг</span>
                        <span>Собирать метрики производительности сервера</span>
                    </div>
                </div>
                <div className={"setting-checkbox-item"}>
                    <input type={"checkbox"}/>
                    <div className={"setting-checkbox-text"}>
                        <span style={{color: "black"}}>Автоматические резервные копии</span>
                        <span>Создавать резервные копии конфигураций каждые 24 часа</span>
                    </div>
                </div>
            </div>
            <div className={"setting-metrics"}>
                <div className={"setting-metrics-item"}>
                    <span className={"setting-metrics-item-header"}>Использование CPU</span>
                    <span className={"setting-metrics-item-text"}>23%</span>
                </div>
                <div className={"setting-metrics-item"}>
                    <span className={"setting-metrics-item-header"}>Использование RAM</span>
                    <span className={"setting-metrics-item-text"}>45%</span>
                </div>
                <div className={"setting-metrics-item"}>
                    <span className={"setting-metrics-item-header"}>Использование диска</span>
                    <span className={"setting-metrics-item-text"}>12%</span>
                </div>
            </div>
        </>
    )
}