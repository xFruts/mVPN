import FilterAllUsers from "./component/FilterAllUsers/FilterAllUsers.tsx";
import TableAllUsers from "./component/TableAllUsers/TableAllUsers.tsx";
import styles from "./AllUsers.module.css";
import { Plus } from "lucide-react";
import { NavLink } from "react-router";
import useUsersStore from "../../store/useUsersStore.ts";

export default function MainAllUsers() {
    const { users } = useUsersStore();
    return (
        <div className={styles.allusersContent}>
            <div className={`${styles.allusersHeader} padding`}>
                <div className={styles.allusersHeaderText}>
                    <p className={styles.usersText}>Пользователи</p>
                    <p className={styles.countText}>
                        {users.length} пользовател
                        {users.length > 5 || users.length === 0
                            ? "ей"
                            : users.length === 1
                                ? "ь"
                                : "я"}{" "}
                        найдено
                    </p>
                </div>
                <div className={styles.addusersButton}>
                    <NavLink to={"/users/add"} className="navLink">
                        <div className={styles.addusersButtonBorder}>
                            <Plus size={20} />
                            <span>Добавить пользователя</span>
                        </div>
                    </NavLink>
                </div>
            </div>
            <FilterAllUsers />
            <TableAllUsers />
        </div>
    );
}