import FilterAllUsers from "./component/FilterAllUsers/FilterAllUsers.tsx";
import TableAllUsers from "./component/TableAllUsers/TableAllUsers.tsx";
import styles from "./AllUsers.module.css";
import { Plus, RefreshCw } from "lucide-react";
import { NavLink } from "react-router";
import useUsersStore from "../../store/useUsersStore.ts";
import {useEffect} from "react";
import {SubscriptionService} from "@api/services/subscriptionService.ts";

export default function MainAllUsers() {
    const { totalElements, fetchUsers, selectedIds, isInitialized, error, isLoading } = useUsersStore();
    useEffect(() => {
        fetchUsers({});
    }, [fetchUsers]);
    
    const handleExtendSubscription = async () => {
        try {
            await SubscriptionService.extendSubscription(selectedIds)
            await fetchUsers({})
        }
        catch (error)  {
            console.error(error);
        }
    }

    const handleRefresh = () => {
        fetchUsers({});
    }

    if (!isInitialized) {
        return <div className={"loadingText"}>Загрузка пользователей...</div>;
    }

    if (error && error.statusCode >= 500) {
        return (
            <div className={styles.errorText}>
                Ошибка сервера ({error.statusCode}): {error.message}
            </div>
        );
    }

    return (
        <div className={styles.allUsersContent}>
            <div className={`${styles.allUsersHeader} padding`}>
                <span className={styles.countText}>Найдено: {totalElements}</span>
                <div className={styles.usersButton}>
                    <button className={styles.updateButton} onClick={handleRefresh}>
                        <RefreshCw size={16} className={isLoading ? styles.spinning  : ""}/>
                    </button>
                    {selectedIds.length > 0 && (
                        <button
                            className={styles.extendButton}
                            onClick={handleExtendSubscription}
                        >
                            Продлить ({selectedIds.length})
                        </button>
                    )}
                    <NavLink to={"/users/add"} className="navLink">
                        <div className={styles.addButton}>
                            <Plus size={16} />
                            <span>
                                Добавить <span className={"PC"}>пользователя</span>
                            </span>
                        </div>
                    </NavLink>
                </div>
            </div>
            <FilterAllUsers />
            <TableAllUsers />
        </div>
    );
}