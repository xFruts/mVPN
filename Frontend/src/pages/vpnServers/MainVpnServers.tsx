import { Plus, RefreshCw } from "lucide-react";
import FilterVpnServers from "./components/FilterVpnServers/FilterVpnServers.tsx";
import TableVpnServers from "./components/TableVpnServers/TableVpnServers.tsx";
import styles from "./MainVpnServers.module.css";
import { useEffect } from "react";
import useServersStore from "@store/useServersStore.ts";
import ChangeVpnServers from "@pages/vpnServers/components/ChangeVpnServers/ChangeVpnServers.tsx";

export default function MainVpnServers() {
    const {
        fetchServers,
        setIsChangeOpen,
        isChangeOpen,
        setIsOpen,
        totalElements,
        isLoading,
        isInitialized,
        error,
    } = useServersStore();

    useEffect(() => {
        fetchServers({});
    }, [fetchServers]);

    const handleRefresh = () => {
        fetchServers({});
    }

    if (!isInitialized) {
        return <div className={"loadingText"}>Загрузка серверов...</div>;
    }

    if (error && error.statusCode >= 500) {
        return (
            <div className={styles.errorText}>
                Ошибка сервера ({error.statusCode}): {error.message}
            </div>
        );
    }

    return (
        <div className={styles.serversContent}>
            <div className={`${styles.serversHeader} ${styles.padding}`}>
                <span className={styles.headerText}>
                    Всего серверов: {totalElements}
                </span>
                <div className={styles.serversButton}>
                    <div
                        className={styles.updateButton}
                        onClick={handleRefresh}
                    >
                        <RefreshCw
                            size={16}
                            className={isLoading ? styles.spinning : ""}
                        />
                    </div>
                    <div
                        className={styles.addButton}
                        onClick={() => {
                            setIsChangeOpen(true);
                            setIsOpen(-1);
                        }}
                    >
                        <Plus size={16} />
                        <span>
                            Добавить <span className={"PC"}>сервер</span>
                        </span>
                    </div>
                </div>
            </div>
            <FilterVpnServers />
            <TableVpnServers />
            {isChangeOpen && <ChangeVpnServers />}
        </div>
    );
}