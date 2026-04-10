import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useState } from "react";
import { NavLink } from "react-router";
import {
    MoveLeft,
    RotateCw,
    Wrench,
    Settings,
    Globe,
    Lock,
    BarChart,
    Save,
} from "lucide-react";
import styles from "./SettingVpnServers.module.css";
import SettingMain from "./SettingMain.tsx";
import SettingNetwork from "./SettingNetwork.tsx";
import SettingSafety from "./SettingSafety.tsx";
import SettingMonitoring from "./SettingMonitoring.tsx";
import { schema } from "./SettingSchema.tsx"

type FormData = z.infer<typeof schema>;

export default function SettingsVpnServers() {

    const form = useForm<FormData>({
        resolver: zodResolver(schema),
        defaultValues: {
            maxUsers: "100",
            bandwidth: "100 Mbps",
            logging: "Debug",
            automaticReboot: false,
            port: "51820",
            keepAlive: "25",
            dnsSettings: {
                primary: "1.1.1.1",
                secondary: "2.2.2.2"
            },
            allowedIPs: "0.0.0.0/0",
            chaCha: false,
            AES: false,
            blockingTraffic: false,
            speedLimit: false,
            monitoring: false,
            backups: false
        }
    });

    const onSubmit = (data: FormData) => {
        console.log(data);
    };

    const [state, setState] = useState("main");
    return (
        <div className={styles.editServer}>
            <div className={styles.editServerHeader}>
                <div className={styles.editServerHeaderTitle}>
                    <span style={{ fontSize: "23px" }}>Настройки сервера</span>
                    <span style={{ color: "gray" }}>Helsinki-01 (ID: 1)</span>
                </div>
                <NavLink to={"/servers"} end className={styles.editServerHeaderBack}>
                    <MoveLeft size={13} />
                    <span>Назад к списку</span>
                </NavLink>
            </div>
            <div className={styles.editServerContent}>
                <div className={styles.editServerContentFast}>
                    <div className={styles.editServerContentFastTitle}>
                        <span>Быстрые действия</span>
                    </div>
                    <div className={styles.editServerContentFastBody}>
                        <button
                            className={styles.editServerContentFastBodyReload}
                        >
                            <RotateCw size={20} />
                            <span>Перезагрузить сервер</span>
                        </button>
                        <button
                            className={styles.editServerContentFastBodySetting}
                        >
                            <Wrench size={20} />
                            <span>Режим обслуживания</span>
                        </button>
                    </div>
                </div>
                <div className={styles.editServerContentSettings}>
                    <div className={styles.editServerContentSettingsTitle}>
                        <div
                            className={`${styles.editServerContentSettingsTitleItem} ${state == "main" ? styles.settingActive : ""}`}
                            onClick={() => setState("main")}
                        >
                            <Settings size={20} />
                            <span>Основное</span>
                        </div>
                        <div
                            className={`${styles.editServerContentSettingsTitleItem} ${state == "network" ? styles.settingActive : ""}`}
                            onClick={() => setState("network")}
                        >
                            <Globe size={20} />
                            <span>Сеть</span>
                        </div>
                        <div
                            className={`${styles.editServerContentSettingsTitleItem} ${state == "safety" ? styles.settingActive : ""}`}
                            onClick={() => setState("safety")}
                        >
                            <Lock size={20} />
                            <span>Безопасность</span>
                        </div>
                        <div
                            className={`${styles.editServerContentSettingsTitleItem} ${state == "monitoring" ? styles.settingActive : ""}`}
                            onClick={() => setState("monitoring")}
                        >
                            <BarChart size={20} />
                            <span>Мониторинг</span>
                        </div>
                    </div>
                    <div className={styles.editServerContentSettingsBody}>
                        {state === "main" ? (
                            <SettingMain form={form}/>
                        ) : state === "network" ? (
                            <SettingNetwork form={form}/>
                        ) : state === "safety" ? (
                            <SettingSafety form={form}/>
                        ) : (
                            <SettingMonitoring form={form}/>
                        )}
                    </div>
                </div>
                <form onSubmit={form.handleSubmit(onSubmit)}>
                    <div className={styles.editServerContentButton}>
                        <button className={"buttonCreate"}>
                            <Save size={20} />
                            Сохранить настройки
                        </button>
                        <NavLink to={"/servers"} className={styles.editServerHeaderBack}>
                            <button className={"buttonCancel"}>
                                Назад к серверам
                            </button>
                        </NavLink>
                    </div>
                </form>
            </div>
        </div>
    );
}
