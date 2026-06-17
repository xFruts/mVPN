import { Search } from "lucide-react";
import { USER_ROLES, USER_STATUS } from "@/constant.ts";
import styles from "./FilterAllUsers.module.css";
import useUsersStore from "../../../../store/useUsersStore.ts";

export default function FilterAllUsers() {
    const { setParams } = useUsersStore();
    return (
        <div className={`${styles.allusersFilter} padding`}>
            <div className={styles.filterTable}>
                <div className={styles.input}>
                    <Search size={20} className={styles.search} />
                    <input
                        type="text"
                        name="search"
                        placeholder="Поиск по имени..."
                        onChange={(e) => setParams({ search: e.target.value })}
                    />
                </div>
                <div className={styles.text}>
                    <select
                        className={styles.filterInput}
                        onChange={(e) => {
                            e.target.blur();
                            setParams({ role: e.target.value });
                        }}
                    >
                        <option value="">Все роли</option>
                        {USER_ROLES.map((role) => {
                            return (
                                <option key={role} value={role}>
                                    {role}
                                </option>
                            );
                        })}
                    </select>
                </div>
                <div className={styles.text}>
                    <select
                        className={styles.filterInput}
                        onChange={(e) => {
                            e.target.blur();
                            setParams({ subStatus: e.target.value });
                        }}
                    >
                        <option value="">Все статусы</option>
                        {USER_STATUS.map((status) => {
                            return (
                                <option key={status} value={status}>
                                    {status}
                                </option>
                            );
                        })}
                    </select>
                </div>
            </div>
        </div>
    );
}