import { Search } from "lucide-react";
import styles from './FilterVpnServers.module.css'

export default function FilterVpnServers() {
    return (
        <div className={"padding"}>
            <div className={styles.filterTable}>
                <div className={styles.input}>
                    <Search size={20} className={styles.searchInput} />
                    <input
                        type="text"
                        name="search"
                        placeholder="Поиск по имени..."
                    />
                </div>
                <div className={styles.text}>
                    <select
                        className={styles.searchInput}
                        onChange={(e) => {
                            e.target.blur();
                        }}
                    >
                        <option value={"allRole"}>Все статусы</option>
                        <option value={"user"}>ONLINE</option>
                        <option value={"special"}>MAINTENANCE</option>
                        <option value={"admin"}>OFFLINE</option>
                    </select>
                </div>
            </div>
        </div>
    );
}
