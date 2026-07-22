import { Search } from "lucide-react";
import styles from './FilterVpnServers.module.css'
import { SERVER_STATUS } from "@/constant.ts";
import useServersStore from "@store/useServersStore.ts";


export default function FilterVpnServers() {
    const { setParams } = useServersStore();
    return (
        <div className={"padding"}>
            <div className={styles.filterTable}>
                <div className={styles.input}>
                    <Search size={16} className={styles.search} />
                    <input
                        type="text"
                        name="search"
                        placeholder="Поиск по IP или названию..."
                        onChange={(e) => setParams({ search: e.target.value })}
                    />
                </div>
                <div className={styles.text}>
                    <select
                        className={styles.filterInput}
                        onChange={(e) => {
                            e.target.blur();
                            setParams({ status: e.target.value });
                        }}
                    >
                        <option value="">Все статусы</option>
                        {SERVER_STATUS.map((role) => {
                            return (
                                <option key={role} value={role}>
                                    {role}
                                </option>
                            );
                        })}
                    </select>
                </div>
            </div>
        </div>
    );
}
