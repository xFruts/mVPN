import styles from "./MainHeader.module.css";
import { Sun, ChevronDown, User, Settings2, LogOut, Menu } from "lucide-react";
import { MoonIcon } from "@phosphor-icons/react";
import { useLocation } from "react-router";
import { useEffect, useRef, useState } from "react";
import { useAdmin } from "@/AdminContext.tsx";

const pageTitles: Record<string, string> = {
    payment: "Оплата",
    users: "Пользователи",
    sendMessages: "Отправка сообщений",
    servers: "VPN серверы",
    promotional: "Промокоды",
    tariffs: "Тарифы",
    paying: "Платежи",
    settings: "Настройки",
};

const getInitialTheme = (): string => {
    const savedTheme = localStorage.getItem("app-theme");
    if (savedTheme) {
        return savedTheme;
    }

    const prefersDark = window.matchMedia(
        "(prefers-color-scheme: dark)",
    ).matches;
    return prefersDark ? "dark" : "light";
};

export default function MainHeader({ openMobile }: { openMobile: () => void }) {
    const location = useLocation();
    const rootPath = location.pathname.split("/")[1];
    const admin = useAdmin();
    const [theme, setTheme] = useState(getInitialTheme);
    const [isOpen, setIsOpen] = useState<boolean>(false);
    const menuRef = useRef<HTMLDivElement>(null);
    useEffect(() => {
        localStorage.setItem("app-theme", theme);
        document.body.setAttribute("data-theme", theme);
    }, [theme]);

    const toggleTheme = () => {
        setTheme((prev) => (prev === "light" ? "dark" : "light"));
    };

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (
                menuRef.current &&
                !menuRef.current.contains(event.target as Node)
            ) {
                setIsOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () =>
            document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    return (
        <div className={styles.header}>
            <div className={styles.headerText}>
                <button className={styles.burgerBtn} onClick={openMobile}>
                    <Menu size={20} />
                </button>
                <span>{pageTitles[rootPath] || "Неизвестная страница"}</span>
            </div>
            <div className={styles.headerBody} ref={menuRef}>
                <div className={styles.theme} onClick={toggleTheme}>
                    {theme === "light" ? (
                        <MoonIcon size={20} />
                    ) : (
                        <Sun size={20} />
                    )}
                </div>
                <div
                    className={styles.account}
                    onClick={() => setIsOpen(!isOpen)}
                >
                    <div className={styles.user}>
                        <User size={20} />
                    </div>
                    <p className={styles.mobileNone}>
                        {admin.preferred_username}
                    </p>
                    <ChevronDown size={16} className={styles.chevron} />
                </div>
                {isOpen && (
                    <div className={styles.dropdown}>
                        <div className={styles.menuItem}>
                            <Settings2 size={18} strokeWidth={1.5} />
                            <span>Настройки</span>
                        </div>
                        <div className={`${styles.menuItem} ${styles.logout}`}>
                            <LogOut size={18} strokeWidth={1.5} />
                            <span>Выйти</span>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
