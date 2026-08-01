import { Plus } from "lucide-react";
import FilterVpnServers from "./components/FilterVpnServers/FilterVpnServers.tsx";
import TableVpnServers from "./components/TableVpnServers/TableVpnServers.tsx";
import styles from "./MainVpnServers.module.css";
import { useEffect } from "react";
import useServersStore from "@store/useServersStore.ts";
import ChangeVpnServers from "@pages/vpnServers/components/ChangeVpnServers/ChangeVpnServers.tsx";
import UpdateButton from "@pages/shared/update/update.tsx";
import type { GetServerData } from "@/types/general.ts";

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
                    <UpdateButton<GetServerData> isLoading={isLoading} fetchData={fetchServers}/>
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