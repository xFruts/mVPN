import {useState} from "react";
import {NavLink} from "react-router";
import './Sidebar.css';
import Logo from "../../assets/images/Logo.png";
import { LayoutDashboard, ShieldCheck, Ticket, 	LineChart, MessageSquare, Users, Settings } from "lucide-react";


export default function Sidebar() {
    const [state, setState] = useState("panel");
    function handleStateChange(text: string) {
        setState(text);
    }
    return (
        <div className="sidebar-content border-right">
            <div className="border-bottom">
                <div className="sidebar-logo">
                    <div className="logo">
                        <img src={Logo} alt={"Logo"}></img>
                        <label>mVPN</label>
                    </div>
                    <label>Панель управления</label>
                </div>
            </div>
            <div className="sidebar-body">
                <div className="padding">
                    <NavLink to={"/"} className={`nav-link sidebar-users ${state == "panel" ? "active" : ""}`} onClick={() => handleStateChange("panel")}>
                        <div className="users">
                            <LayoutDashboard size={20}/>
                            <label>Панель управления</label>
                        </div>
                    </NavLink>
                </div>
                <div className="padding">
                    <NavLink to={"/users"} className={`nav-link sidebar-users ${state == "all" ? "active" : ""}`} onClick={() => handleStateChange("all")}>
                        <div className="users">
                            <Users size={20}/>
                            <label>Пользователи</label>
                        </div>
                    </NavLink>
                </div>
                <div className="padding">
                    <NavLink to={"/sendMessages"} className={`nav-link sidebar-users ${state == "send" ? "active" : ""}`} onClick={() => handleStateChange("send")}>
                        <div className="users">
                            <MessageSquare size={20}/>
                            <label>Отправка сообщений</label>
                        </div>
                    </NavLink>
                </div>
                <div className="padding">
                    <NavLink to={"/servers"} className={`nav-link sidebar-users ${state == "vpn" ? "active" : ""}`} onClick={() => handleStateChange("vpn")}>
                        <div className="users">
                            <ShieldCheck size={20}/>
                            <label>VPN серверы</label>
                        </div>
                    </NavLink>
                </div>
                <div className="padding">
                    <NavLink to={"/promotional"} className={`nav-link sidebar-users ${state == "promotional" ? "active" : ""}`} onClick={() => handleStateChange("promotional")}>
                        <div className="users">
                            <Ticket size={20}/>
                            <label>Промокоды</label>
                        </div>
                    </NavLink>
                </div>
                <div className="padding">
                    <NavLink to={"/analytics"} className={`nav-link sidebar-users ${state == "analytics" ? "active" : ""}`} onClick={() => handleStateChange("analytics")}>
                        <div className="users">
                            <LineChart size={20}/>
                            <label>Аналитика</label>
                        </div>
                    </NavLink>
                </div>
                <div className="padding">
                    <NavLink to={"/settings"} className={`nav-link sidebar-users ${state == "settings" ? "active" : ""}`} onClick={() => handleStateChange("settings")}>
                        <div className="users">
                            <Settings size={20}/>
                            <label>Настройки</label>
                        </div>
                    </NavLink>
                </div>
            </div>
        </div>
    );
}