import styles from "./Header.module.css"
import Logo from "@/assets/images/Logo.tsx";
import { useAdmin } from "@/AdminContext.tsx";
import { useUserMode } from "@pages/landing/UserContext.ts";
import { User, House, LogIn } from "lucide-react";

export default function Header({ mode }: { mode?: boolean }) {
    const { login } = useAdmin();
    const { isUserMode, setIsUserMode } = useUserMode();

    const handleUserClick = () => {
        const nextMode = !isUserMode;
        setIsUserMode(nextMode);
        localStorage.setItem("userMode", nextMode.toString());
    };

    const scrollTo = (id: string) => {
        const element = document.getElementById(id);
        if (element) {
            element.scrollIntoView({ behavior: "smooth" });
        }
    };

    return (
        <div className={styles.header}>
            <div className={styles.icon}>
                <Logo />
                <span className={styles.iconText} data-text="mVPN">
                    mVPN
                </span>
            </div>
            <div className={styles.headerButton}>
                <span
                    className={styles.buttonText}
                    onClick={() => scrollTo("technology-section")}
                >
                    Технология
                </span>
                <span
                    className={styles.buttonText}
                    onClick={() => scrollTo("advantages-section")}
                >
                    Преимущества
                </span>
                {mode && <span className={styles.buttonText}>Цены</span>}
                <button
                    className={`${styles.changeLanding} Mobile`}
                    onClick={handleUserClick}
                >
                    {mode ? (
                        <House className={styles.changeIcon} />
                    ) : (
                        <User className={styles.changeIcon} />
                    )}
                </button>
                <button
                    className={`${styles.buttonLogin} Mobile`}
                    onClick={() => login?.()}
                >
                    <LogIn className={styles.changeIcon} />
                </button>
                <button
                    className={`${styles.changeLanding} PC`}
                    onClick={handleUserClick}
                >
                    {mode ? "Для бизнеса" : "Для пользователя"}
                </button>
                <button
                    className={`${styles.buttonLogin} PC`}
                    onClick={() => login?.()}
                >
                    {mode ? "Личный кабинет" : "Войти в панель"}
                </button>
            </div>
        </div>
    );
}