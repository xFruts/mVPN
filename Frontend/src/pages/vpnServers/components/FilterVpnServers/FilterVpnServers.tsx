import {Search} from "lucide-react";

export default function FilterVpnServers() {
    return(
        <div className="vpnservers-filter padding">
            <div className="filter-table">
                <div className="input">
                    <Search size={20} className="search-input"/>
                    <input type="text" name="search" placeholder="Поиск по имени..." />
                </div>
                <div className="text">
                    <select className="search-input"
                            onChange={(e) => {
                                e.target.blur();
                            }}>
                        <option value={"allRole"}>Все статусы</option>
                        <option value={"user"}>ONLINE</option>
                        <option value={"special"}>MAINTENANCE</option>
                        <option value={"admin"}>OFFLINE</option>
                    </select>
                </div>
            </div>
        </div>
    )
}