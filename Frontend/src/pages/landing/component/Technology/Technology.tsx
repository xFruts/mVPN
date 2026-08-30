import styles from "./Technology.module.css"
import AnimatedFlower from "@pages/landing/component/Animated/Animated.tsx";
import { useScrollReveal } from "@pages/shared/useScrollReveal.tsx";

export default function Technology() {
    const [ref, isVisible] = useScrollReveal();
    return (
        <div
            ref={ref}
            id="technology-section"
            className={`${styles.technology} ${isVisible ? styles.visible : ""}`}
        >
            <div className={styles.header}>
                <div className={styles.epigraph}>
                    <span>НЕКСТ ГЕН... АБСОЛЮТНАЯ СВОБОДА</span>
                </div>
                <span className={styles.headerText}>
                    Некст-Ген VPN Технология для Полной Свободы
                </span>
            </div>
            <div className={styles.content}>
                <AnimatedFlower />
                <div className={styles.contentText}>
                    <div className={styles.text}>
                        <span className={styles.textHeader}>
                            Непревзойденная связь
                        </span>
                        <span className={styles.textFooter}>
                            Независимо от вашего местоположения, наша система
                            обеспечивает стабильное соединение для
                            пользователей. Централизованное управление позволяет
                            в реальном времени обходить блокировки и
                            перенаправлять трафик.
                        </span>
                    </div>
                    <div className={styles.text}>
                        <span className={styles.textHeader}>
                            Молниеносная производительность
                        </span>
                        <span className={styles.textFooter}>
                            Автоматизированное распределение нагрузки и
                            мгновенная синхронизация с X-UI узлами. Ваши клиенты
                            получают максимальную скорость для 4K-стриминга и
                            игр без задержек.
                        </span>
                    </div>
                    <div className={styles.text}>
                        <span className={styles.textHeader}>
                            Конфиденциальность без компромиссов
                        </span>
                        <span className={styles.textFooter}>
                            Продвинутое шифрование и отсутствие логов на стороне
                            VPN. Мы обеспечиваем безопасное туннелирование
                            трафика, скрывая активность от провайдеров и систем
                            глубокого анализа.
                        </span>
                    </div>
                    <div className={styles.text}>
                        <span className={styles.textHeader}>
                            Современные VPN протоколы
                        </span>
                        <span className={styles.textFooter}>
                            Полная поддержка VLESS, VMess, Trojan и других
                            передовых транспортных протоколов на базе ядра Xray.
                            Гарантированный обход самых жестких ограничений и
                            файрволов.
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
};