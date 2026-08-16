import { useEffect, useState } from "react";
import { NavLink, useLocation } from "react-router";
import styles from "./Sidebar.module.css";
import Logo from "../../assets/images/Logo.tsx"
import {
    LayoutGrid,
    ShieldCheck,
    Tag,
    CreditCard,
    Wallet,
    MessageSquare,
    Users,
    Settings,
    ChevronLeft,
    ChevronRight,
    X
} from "lucide-react";

const serviceItems = [
    { state: "payment", icon: <LayoutGrid size={20} />, text: "Оплата" },
    { state: "users", icon: <Users size={20} />, text: "Пользователи" },
    {
        state: "sendMessages",
        icon: <MessageSquare size={20} />,
        text: "Отправка сообщений",
    },
    { state: "servers", icon: <ShieldCheck size={20} />, text: "VPN серверы" },
    { state: "promotional", icon: <Tag size={20} />, text: "Промокоды" },
    { state: "tariffs", icon: <CreditCard size={20} />, text: "Тарифы" },
    { state: "paying/transactions", icon: <Wallet size={20} />, text: "Платежи" },
    { state: "settings", icon: <Settings size={20} />, text: "Настройки" },
];

interface SidebarProps {
    isMobileOpen: boolean;
    closeMobile: () => void;
}

export default function Sidebar({ isMobileOpen, closeMobile }: SidebarProps) {
    const location = useLocation();
    const currentPath = location.pathname.split("/").filter(Boolean)[0] || "";

    const [hideBar, setHideBar] = useState(false);

    function handleHideBar() {
        setHideBar(!hideBar);
        console.log(hideBar);
    }

    useEffect(() => {
        if (isMobileOpen) {
            document.body.style.overflow = "hidden";
        } else {
            document.body.style.overflow = "unset";
        }
    }, [isMobileOpen]);

    useEffect(() => {
        const handleResize = () => {
            if (window.innerWidth <= 768) {
                setHideBar(false);
            }
        };
        window.addEventListener("resize", handleResize);
        handleResize();
        return () => window.removeEventListener("resize", handleResize);
    }, []);

    return (
        <>
            <div
                className={`${styles.backdrop} ${isMobileOpen ? styles.backdropVisible : ""}`}
                onClick={closeMobile}
            />

            <aside
                className={`${styles.sidebarWrapper} ${hideBar ? styles.hidden : ""} ${isMobileOpen ? styles.mobileOpen : ""}`}
            >
                <div className={styles.sidebarContent}>
                    <div className={styles.sidebarLogo}>
                        <div className={styles.logo}>
                            <Logo />
                            {!hideBar && <label>mVPN</label>}
                            <div
                                className={styles.mobileCloseBtn}
                                onClick={closeMobile}
                            >
                                <X size={24} strokeWidth={1.5} />
                            </div>
                        </div>
                        <div
                            className={styles.collapse}
                            onClick={handleHideBar}
                        >
                            {!hideBar ? (
                                <ChevronLeft size={14} />
                            ) : (
                                <ChevronRight size={14} />
                            )}
                        </div>
                    </div>
                    <div className={styles.sidebarBody}>
                        {serviceItems.map((item) => (
                            <div
                                key={item.state}
                                className={`${item.state.includes(currentPath) ? styles.border : ""}`}
                            >
                                <NavLink
                                    to={`/${item.state}`}
                                    className={`navLink ${styles.sidebarUsers} ${item.state.includes(currentPath) ? styles.active : ""}`}
                                    onClick={() => {
                                        closeMobile();
                                    }}
                                >
                                    <div className={styles.users}>
                                        {item.icon}
                                        {!hideBar && <span>{item.text}</span>}
                                    </div>
                                </NavLink>
                            </div>
                        ))}
                    </div>
                </div>
            </aside>
        </>
    );
}