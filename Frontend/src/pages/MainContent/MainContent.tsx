import { Route, Routes } from "react-router";
import styles from "./MainContent.module.css"
import MainAllUsers from "../allUsers/MainAllUsers.tsx";
import MainSendMessages from "../sendMessages/MainSendMessages.tsx";
import MainVpnServers from "../vpnServers/MainVpnServers.tsx";
import MainPromocodes from "@pages/promocodes/MainPromocodes.tsx";
import MainTariffs from "@pages/tariffs/MainTariffs.tsx";
import MainSettings from "../settings/MainSettings.tsx";
import MainPayment from "@pages/payment/MainPayment.tsx";
import ChangeUsers from "../allUsers/component/ChangeUsers/ChangeUsers.tsx";
import MainPaying from "../paying/MainPaying.tsx"
import MainSubscription from "@pages/subscription/MainSubscription.tsx";
import MainLanding from "@pages/landing/MainLanding.tsx";
import { useErrorStore } from "@store/useErrorStore.ts";
import { useState } from "react";
import Sidebar from "@pages/Sidebar/Sidebar.tsx";
import MainHeader from "@pages/header/MainHeader.tsx";

export default function MainContent() {
    const { criticalError } = useErrorStore();
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

    if (criticalError) {
        return (
            <div className={styles.errorText}>
                {criticalError === 500
                    ? "Ошибка сервера"
                    : "Проблемы с интернетом"}
            </div>
        );
    }

    return (
        <div className={styles.mainPage}>
            <Sidebar
                isMobileOpen={isMobileMenuOpen}
                closeMobile={() => setIsMobileMenuOpen(false)}
            />
            <div className={styles.main}>
                <MainHeader openMobile={() => setIsMobileMenuOpen(true)} />
                <div className={styles.mainContent}>
                    <Routes>
                        <Route path="/payment" Component={MainPayment} />
                        <Route path="/users" Component={MainAllUsers} />
                        <Route path="/sendMessages" Component={MainSendMessages} />
                        <Route path="/servers" Component={MainVpnServers} />
                        <Route path="/promotional" Component={MainPromocodes} />
                        <Route path="/tariffs" Component={MainTariffs} />
                        <Route path="/paying/*" Component={MainPaying} />
                        <Route path="/settings" Component={MainSettings} />
                        <Route path="/subscription" Component={MainSubscription} />
                        <Route path="/landing" Component={MainLanding} />
                        <Route path="/users/add" Component={ChangeUsers} />
                        <Route path="/users/edit/:id" Component={ChangeUsers} />
                    </Routes>
                </div>
            </div>
        </div>
    );
}
