import { useState } from "react";
import { NavLink } from "react-router";
import styles from "./Sidebar.module.css";
import Logo from "../../assets/images/Logo.png";
import {
    LayoutDashboard,
    ShieldCheck,
    Ticket,
    LineChart,
    MessageSquare,
    Users,
    Settings,
} from "lucide-react";

export default function Sidebar() {
    const [state, setState] = useState("panel");

    function handleStateChange(text: string) {
        setState(text);
    }

    return (
        <div className={`${styles.sidebarContent} ${styles.borderRight}`}>
            <div className={styles.borderBottom}>
                <div className={styles.sidebarLogo}>
                    <div className={styles.logo}>
                        <img src={Logo} alt="Logo" />
                        <label>mVPN</label>
                    </div>
                    <label>Панель управления</label>
                </div>
            </div>
            <div className={styles.sidebarBody}>
                <div className="padding">
                    <NavLink
                        to={"/"}
                        className={`navLink ${styles.sidebarUsers} ${state === "panel" ? styles.active : ""}`}
                        onClick={() => handleStateChange("panel")}
                    >
                        <div className={styles.users}>
                            <LayoutDashboard size={20} />
                            <label>Панель управления</label>
                        </div>
                    </NavLink>
                </div>
                <div className="padding">
                    <NavLink
                        to={"/users"}
                        className={`navLink ${styles.sidebarUsers} ${state === "all" ? styles.active : ""}`}
                        onClick={() => handleStateChange("all")}
                    >
                        <div className={styles.users}>
                            <Users size={20} />
                            <label>Пользователи</label>
                        </div>
                    </NavLink>
                </div>
                <div className="padding">
                    <NavLink
                        to={"/sendMessages"}
                        className={`navLink ${styles.sidebarUsers} ${state === "send" ? styles.active : ""}`}
                        onClick={() => handleStateChange("send")}
                    >
                        <div className={styles.users}>
                            <MessageSquare size={20} />
                            <label>Отправка сообщений</label>
                        </div>
                    </NavLink>
                </div>
                <div className="padding">
                    <NavLink
                        to={"/servers"}
                        className={`navLink ${styles.sidebarUsers} ${state === "vpn" ? styles.active : ""}`}
                        onClick={() => handleStateChange("vpn")}
                    >
                        <div className={styles.users}>
                            <ShieldCheck size={20} />
                            <label>VPN серверы</label>
                        </div>
                    </NavLink>
                </div>
                <div className="padding">
                    <NavLink
                        to={"/promotional"}
                        className={`navLink ${styles.sidebarUsers} ${state === "promotional" ? styles.active : ""}`}
                        onClick={() => handleStateChange("promotional")}
                    >
                        <div className={styles.users}>
                            <Ticket size={20} />
                            <label>Промокоды</label>
                        </div>
                    </NavLink>
                </div>
                <div className="padding">
                    <NavLink
                        to={"/analytics"}
                        className={`navLink ${styles.sidebarUsers} ${state === "analytics" ? styles.active : ""}`}
                        onClick={() => handleStateChange("analytics")}
                    >
                        <div className={styles.users}>
                            <LineChart size={20} />
                            <label>Аналитика</label>
                        </div>
                    </NavLink>
                </div>
                <div className="padding">
                    <NavLink
                        to={"/settings"}
                        className={`navLink ${styles.sidebarUsers} ${state === "settings" ? styles.active : ""}`}
                        onClick={() => handleStateChange("settings")}
                    >
                        <div className={styles.users}>
                            <Settings size={20} />
                            <label>Настройки</label>
                        </div>
                    </NavLink>
                </div>
            </div>
        </div>
    );
}