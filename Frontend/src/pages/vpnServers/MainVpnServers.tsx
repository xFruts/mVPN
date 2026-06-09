import { Plus } from "lucide-react";
import FilterVpnServers from "./components/FilterVpnServers/FilterVpnServers.tsx";
import TableVpnServers from "./components/TableVpnServers/TableVpnServers.tsx";
import styles from "./MainVpnServers.module.css";
import { useEffect } from "react";
import useServersStore from "@store/useServersStore.ts";
import ChangeVpnServers from "@pages/vpnServers/components/ChangeVpnServers/ChangeVpnServers.tsx";

export default function MainVpnServers() {
    const { fetchServers, setIsChangeOpen, isChangeOpen, setIsOpen } =
        useServersStore();
    useEffect(() => {
        fetchServers({});
    }, [fetchServers]);
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
                    <div
                        className={styles.vpnserversButtonBorder}
                        onClick={() => {
                            setIsChangeOpen()
                            setIsOpen(-1)
                        }}
                    >
                        <Plus size={20} />
                        <span>Добавить сервер</span>
                    </div>
                </div>
            </div>
            <FilterVpnServers />
            <TableVpnServers />
            {isChangeOpen && <ChangeVpnServers />}
        </div>
    );
}