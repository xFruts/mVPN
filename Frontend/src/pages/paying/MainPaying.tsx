import styles from "./MainPaying.module.css"
import { Plus, Wallet, Settings} from "lucide-react";
import { Navigate, NavLink, Route, Routes } from "react-router";
import PayingTransactions from "./components/PayingTransactions/PayingTransactions.tsx";
import PayingSettings from "./components/PayingSettings/PayingSettings.tsx";
import { useEffect } from "react";
import { usePayingStore } from "@store/usePayingStore.ts";
import AddPaying from "@pages/paying/components/AddPaying/AddPaying.tsx";
import type { GetPayingData } from "@/types/general.ts";
import UpdateButton from "@pages/shared/update/update.tsx";

export default function MainPaying() {
    const { fetchPaying, fetchStats, setIsOpen, isOpen, isLoading, isInitialized } = usePayingStore();
    const currentPath = location.pathname.split("/").filter(Boolean)[1] || "";
    useEffect(() => {
        void fetchPaying({});
        void fetchStats();
    }, [fetchPaying, fetchStats]);

    if (!isInitialized) {
        return <div className={"loadingText"}>Загрузка платежей...</div>;
    }

    return (
        <div className={"component"}>
            <div className={"componentHeader"}>
                <span className={"componentText"}>
                    Реквизиты и ручное подтверждение платежей
                </span>
                <div className={styles.updateButton}>
                    <UpdateButton<GetPayingData>
                        isLoading={isLoading}
                        fetchData={fetchPaying}
                    />
                    <div
                        className={"addButton"}
                        onClick={() => {
                            setIsOpen(true);
                        }}
                    >
                        <Plus size={20} />
                        <span>
                            Создать <span className={"PC"}>заявку</span>
                        </span>
                    </div>
                </div>
            </div>
            <div className={`${styles.payingContent} ${styles.padding}`}>
                <div className={styles.contentChoice}>
                    <NavLink to="/paying/transactions" className="navLink">
                        <div
                            className={`${styles.choice} ${currentPath === "transactions" ? styles.active : ""}`}
                        >
                            <Wallet size={16} />
                            <span>Транзакции</span>
                        </div>
                    </NavLink>
                    <NavLink to="/paying/settings" className="navLink">
                        <div
                            className={`${styles.choice} ${currentPath === "settings" ? styles.active : ""}`}
                        >
                            <Settings size={16} />
                            <span>Настройки</span>
                        </div>
                    </NavLink>
                </div>
                <>
                    <Routes>
                        <Route
                            path="/"
                            element={<Navigate to="transactions" replace />}
                        />

                        <Route
                            path="transactions"
                            element={<PayingTransactions />}
                        />
                        <Route path="settings" element={<PayingSettings />} />
                    </Routes>
                </>
            </div>
            {isOpen && <AddPaying />}
        </div>
    );
}
