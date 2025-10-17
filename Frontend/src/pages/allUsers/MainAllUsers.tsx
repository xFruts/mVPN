import FilterAllUsers from "./component/FilterAllUsers/FilterAllUsers.tsx";
import TableAllUsers from "./component/TableAllUsers/TableAllUsers.tsx";
import './AllUsers.css';
import {Plus} from "lucide-react"
import {NavLink} from "react-router";



export default function MainAllUsers(){
    return(
        <div className="allusers-content">
            <div className="allusers-header padding">
                <div className="allusers-header-text">
                    <p className="users-text">Пользователи</p>
                    <p className="count-text">27 пользователей найдено</p>
                </div>
                <div className="addusers-button">
                    <NavLink to={"/addUsers"} className={`nav-link`}>
                        <div className="addusers-button-border">
                            <Plus size={20}/>
                            <span>Добавить пользователя</span>
                        </div>
                    </NavLink>
                </div>
            </div>
            <FilterAllUsers/>
            <TableAllUsers/>
        </div>
    );
}