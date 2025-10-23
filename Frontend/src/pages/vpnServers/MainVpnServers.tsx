import {NavLink} from "react-router";
import {Plus} from "lucide-react";
import FilterVpnServers from "./components/FilterVpnServers/FilterVpnServers.tsx";
import TableVpnServers from "./components/TableVpnServers/TableVpnServers.tsx";
import "./MainVpnServers.css"

export default function MainVpnServers() {
    return(
        <div className="vpnservers-content">
            <div className="vpnservers-header padding">
                <div className="vpnservers-header-text">
                    <p className="users-text">VPN Серверы</p>
                    <p className="count-text">Управление серверами и мониторинг состояния</p>
                </div>
                <div className="vpnservers-button">
                    <NavLink to={"/servers/add"} className={`nav-link`}>
                        <div className="vpnservers-button-border">
                            <Plus size={20}/>
                            <span>Добавить сервер</span>
                        </div>
                    </NavLink>
                </div>
            </div>
            <FilterVpnServers/>
            <TableVpnServers/>
        </div>
    )
}