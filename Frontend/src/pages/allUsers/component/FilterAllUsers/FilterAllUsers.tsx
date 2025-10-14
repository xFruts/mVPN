import { Search} from "lucide-react";
import './FilterAllUsers.css'

export default function FilterAllUsers() {

    return(
        <div className="allusers-filter padding">
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
                        <option value={"allRole"}>Все роли</option>
                        <option value={"user"}>USER</option>
                        <option value={"special"}>SPECIAL</option>
                        <option value={"admin"}>ADMIN</option>
                    </select>
                </div>
                <div className="text">
                    <select className="search-input"
                            onChange={(e) => {
                                e.target.blur();
                            }}>
                        <option value={"allSubscription"}>Все подписки</option>
                        <option value={"basic"}>BASIC</option>
                        <option value={"family"}>FAMILY</option>
                        <option value={"premium"}>PREMIUM</option>
                        <option value={"admin"}>ADMIN</option>
                    </select>
                </div>
                <div className="text">
                    <select className="search-input"
                            onChange={(e) => {
                                e.target.blur();
                            }}>
                        <option value={"allStatus"}>Все статусы</option>
                        <option value={"active"}>ACTIVE</option>
                        <option value={"expired"}>EXPIRED</option>
                        <option value={"canceled"}>CANCELED</option>
                    </select>
                </div>
            </div>
        </div>
    );
}