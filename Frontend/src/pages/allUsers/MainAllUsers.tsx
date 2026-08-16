import FilterAllUsers from "./component/FilterAllUsers/FilterAllUsers.tsx";
import TableAllUsers from "./component/TableAllUsers/TableAllUsers.tsx";
import styles from "./AllUsers.module.css";
import { Plus } from "lucide-react";
import { NavLink } from "react-router";
import useUsersStore from "../../store/useUsersStore.ts";
import {useEffect} from "react";
import {SubscriptionService} from "@api/services/subscriptionService.ts";
import UpdateButton from "@pages/shared/update/update.tsx";
import type { GetUserData } from "@/types/general.ts";

export default function MainAllUsers() {
    const { totalElements, fetchUsers, selectedIds, isInitialized, isLoading } = useUsersStore();
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

    if (!isInitialized) {
        return <div className={"loadingText"}>Загрузка пользователей...</div>;
    }

    return (
        <div className={"component"}>
            <div className={`componentHeader padding`}>
                <span className={"componentText"}>Найдено: {totalElements}</span>
                <div className={styles.usersButton}>
                    <UpdateButton<GetUserData>
                        isLoading={isLoading}
                        fetchData={fetchUsers}
                    />
                    {selectedIds.length > 0 && (
                        <button
                            className={styles.extendButton}
                            onClick={handleExtendSubscription}
                        >
                            Продлить ({selectedIds.length})
                        </button>
                    )}
                    <NavLink to={"/users/add"} className="navLink">
                        <div className={"addButton"}>
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