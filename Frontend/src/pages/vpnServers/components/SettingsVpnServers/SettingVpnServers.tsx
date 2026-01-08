import {useState} from "react";
import {NavLink} from "react-router";
import {MoveLeft, RotateCw, Wrench, Settings, Globe, Lock, BarChart, Save} from "lucide-react";
import "./SettingVpnServers.css";
import SettingMain from "./SettingMain.tsx";
import SettingNetwork from "./SettingNetwork.tsx";
import SettingSafety from "./SettingSafety.tsx";
import SettingMonitoring from "./SettingMonitoring.tsx";

export default function SettingsVpnServers(){
    const [state, setState] = useState("main");
    return(
        <div className="editServer">
            <div className={"editServer-header"}>
                <div className="editServer-header-title">
                    <span style={{fontSize: "23px"}}>Настройки сервера</span>
                    <span style={{color: "gray"}}>Helsinki-01 (ID: 1)</span>
                </div>
                <NavLink to={'/servers'} end className="editServer-header-back">
                    <MoveLeft size={13}/>
                    <span>Назад к списку</span>
                </NavLink>
            </div>
            <div className={"editServer-content"}>
                <div className={"editServer-content-fast"}>
                    <div className={"editServer-content-fast-title"}>
                        <span>Быстрые действия</span>
                    </div>
                    <div className={"editServer-content-fast-body"}>
                        <button className={"editServer-content-fast-body-reload"}>
                            <RotateCw size={20}/>
                            <span>Перезагрузить сервер</span>
                        </button>
                        <button className={"editServer-content-fast-body-setting"}>
                            <Wrench size={20}/>
                            <span>Режим обслуживания</span>
                        </button>
                    </div>
                </div>
                <div className={"editServer-content-settings"}>
                    <div className={"editServer-content-settings-title"}>
                        <div className={`editServer-content-settings-title-item ${state == "main" ? "settingActive" : ""}`} onClick={() => setState("main")}>
                            <Settings size={20}/>
                            <span>Основное</span>
                        </div>
                        <div className={`editServer-content-settings-title-item ${state == "network" ? "settingActive" : ""}`} onClick={() => setState("network")}>
                            <Globe size={20}/>
                            <span>Сеть</span>
                        </div>
                        <div className={`editServer-content-settings-title-item ${state == "safety" ? "settingActive" : ""}`} onClick={() => setState("safety")}>
                            <Lock size={20}/>
                            <span>Безопасность</span>
                        </div>
                        <div className={`editServer-content-settings-title-item ${state == "monitoring" ? "settingActive" : ""}`} onClick={() => setState("monitoring")}>
                            <BarChart size={20}/>
                            <span>Мониторинг</span>
                        </div>
                    </div>
                    <div className={"editServer-content-settings-body"}>
                        {
                            state == "main" ? (
                                <SettingMain/>
                            ) : state == "network" ? (
                                <SettingNetwork/>
                            ) : state == "safety" ? (
                                <SettingSafety/>
                            ) : <SettingMonitoring/>
                        }
                    </div>
                </div>
                <div className={"editServer-content-button"}>
                    <NavLink to={'/servers'} className="editServer-header-back">
                        <button className={"button-create"}><Save size={20}/>Сохранить настройи</button>
                    </NavLink>
                    <NavLink to={'/servers'} className="editServer-header-back">
                        <button className={"button-cancel"}>Назад к серверам</button>
                    </NavLink>
                </div>
            </div>
        </div>
    )
}