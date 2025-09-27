import {NavLink} from "react-router";
import './Sidebar.css';

export default function Sidebar() {
    return (
        <div className="sidebar-content">
            <div className="sidebar-header">
                <label>Страницы</label>
            </div>
            <div className="sidebar-body">
                <div className="sidebar-allusers">
                    <NavLink to={"/allUsers"} className="nav-link">
                        Все пользователи
                    </NavLink>
                </div>
                <div className="sidebar-addusers">
                    <NavLink to={"/addUsers"} className="nav-link">
                        Добавить пользователя
                    </NavLink>
                </div>
            </div>
        </div>
    );
}