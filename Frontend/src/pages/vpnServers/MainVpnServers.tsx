import { NavLink } from "react-router";
import { Plus } from "lucide-react";
import FilterVpnServers from "./components/FilterVpnServers/FilterVpnServers.tsx";
import TableVpnServers from "./components/TableVpnServers/TableVpnServers.tsx";
import styles from "./MainVpnServers.module.css";

export default function MainVpnServers() {
    return (
        <div className={styles.vpnserversContent}>
            <div className={`${styles.vpnserversHeader} ${styles.padding}`}>
                <div className={styles.vpnserversHeaderText}>
                    <p className={styles.usersText}>VPN Серверы</p>
                    <p className={styles.countText}>
                        Управление серверами и мониторинг состояния
                    </p>
                </div>
                <div className={styles.vpnserversButton}>
                    <NavLink to={"/servers/add"} className={"navLink"}>
                        <div className={styles.vpnserversButtonBorder}>
                            <Plus size={20} />
                            <span>Добавить сервер</span>
                        </div>
                    </NavLink>
                </div>
            </div>
            <FilterVpnServers />
            <TableVpnServers />
        </div>
    );
}