import styles from "./Main.module.css"
import { Crown, Shield, LayoutGrid } from "lucide-react";
import { useAdmin } from "@/AdminContext.tsx";

export default function Main({ mode }: { mode?: boolean }) {
    const { login } = useAdmin();

    const scrollTo = (id: string) => {
        const element = document.getElementById(id);
        if (element) {
            element.scrollIntoView({ behavior: "smooth" });
        }
    };

    return (
        <div className={styles.main}>
            <div className={styles.header}>
                {mode ? (
                    <>
                        <Crown size={16} />
                        <span>№1 Панель управления VPN инфраструктурой</span>
                    </>
                ) : (
                    <>
                        <Shield size={16} />
                        <span>Ваш надежный проводник в свободный интернет</span>
                    </>
                )}
            </div>
            <div className={styles.mainContent}>
                {mode ? (
                    <>
                        <span>Эффективно.</span>
                        <span>Мощно.</span>
                    </>
                ) : (
                    <span>Безопасно. Быстро.</span>
                )}
                <span className={styles.blueText}>mVPN</span>
            </div>
            <div className={styles.mainFooter}>
                {mode ? (
                    <>
                        <span>
                            Непревзойденная автоматизация, безопасность и
                            контроль.
                        </span>
                        <span>
                            Используйте лучшую панель для биллинга и управления
                            серверами.
                        </span>
                    </>
                ) : (
                    <span>
                        Оставайтесь анонимными и получайте доступ к любимым
                        сервисам без ограничений скорости. Никаких логов, только
                        свобода.
                    </span>
                )}
            </div>
            <div className={styles.mainButtons}>
                <button
                    className={styles.moreButton}
                    onClick={() => {if (mode) scrollTo("technology-section"); else scrollTo("subscribe-section");}}
                >
                    {mode ? "Узнать больше" : "Выбрать тариф"}
                </button>
                <button
                    className={styles.loginButton}
                    onClick={() => {
                        if (mode) {
                            scrollTo("technology-section");
                        } else {
                            login?.();
                        }
                    }}
                >
                    {!mode && <LayoutGrid size={16} />}
                    {mode ? "Подробнее" : "Войти"}
                </button>
            </div>
        </div>
    );
}