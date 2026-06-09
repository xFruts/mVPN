import { useRef, useEffect } from "react";
import {
    MoveVertical,
    EllipsisVertical,
    Key,
    Pencil,
    Trash2,
    Timer,
} from "lucide-react";
import { ArrowUp, ArrowDown } from "react-feather";
import styles from "./TableAllUsers.module.css";
import { NavLink } from "react-router";
import useUsersStore from "../../../../store/useUsersStore.ts";
import type { SortFields, SortDirection} from "@/types/general.ts";
import {UserService} from "@api/services/userService.ts";

const ApiKey = "tg_api_1_570528";

interface Titles {
    name: string;
    api: SortFields;
}

const titles: Titles[] = [
    {
        name: "ID",
        api: "id"
    },
    {
        name: "ПОЛЬЗОВАТЕЛЬ",
        api: "fullName"
    },
    {
        name: "РОЛЬ",
        api: "role"
    },
    {
        name: "ТАРИФ",
        api: "tariff"
    },
    {
        name: "СТАТУС",
        api: "subscriptionStatus"
    },
    {
        name: "СРОК ДЕЙСТВИЯ",
        api: "endDate"
    }
];

export default function TableAllUsers() {
    const {
        users,
        filters,
        apiOpen,
        fetchUsers,
        selectedIds,
        isOpen,
        setApiOpen,
        toggleSelectId,
        toggleAll,
        toggleMenu,
        resetMenu,
    } = useUsersStore();
    const [currentField, currentDir] = (filters.sort || "id,desc").split(
        ",",
    ) as [SortFields, SortDirection];
    const allUserIds = users.map((u) => u.id);
    const menuRef = useRef<HTMLDivElement>(null);

    /*const handleSort = (title: SortFields) => {
        // 1. Достаем текущее состояние из фильтров

        let newSort: SortData;

        if (currentField !== title) {
            newSort = `${title},desc`;
        } else if (currentDir === "desc") {
            newSort = `${title},asc`;
        } else {
            newSort = "id,desc";
        }

        fetchUsers({ sort: newSort, page: 0 });
    };*/

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (!(event.target instanceof Node)) return;
            if (menuRef.current && !menuRef.current.contains(event.target)) {
                resetMenu();
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () =>
            document.removeEventListener("mousedown", handleClickOutside);
    }, [resetMenu]);

    const handleDeleteUser = async () => {
        try {
            await UserService.deleteUser(isOpen)
            await fetchUsers({})
        }
        catch (error) {
            console.error(error);
        }
    }


    return (
        <div className={styles.allusersTable}>
            <table className={styles.tableUsers}>
                <tbody>
                    <tr>
                        <td>
                            <input
                                type={"checkbox"}
                                checked={
                                    selectedIds.length === allUserIds.length
                                }
                                onChange={toggleAll}
                                className={styles.usersCheckbox}
                            />
                        </td>
                        {titles.map((title) => (
                            <td key={title.api}>
                                <div
                                    className={`${styles.tableTitle} ${styles[title.name]}`}
                                >
                                    <span>{title.name}</span>
                                    <div /*onClick={() => handleSort(title.api)}*/
                                    >
                                        {currentField === title.api ? (
                                            currentDir === "desc" ? (
                                                <ArrowDown size={20} />
                                            ) : currentDir === "asc" ? (
                                                <ArrowUp size={20} />
                                            ) : (
                                                <MoveVertical size={20} />
                                            )
                                        ) : (
                                            <MoveVertical size={20} />
                                        )}
                                    </div>
                                </div>
                            </td>
                        ))}
                    </tr>

                    {users.map((user) => (
                        <tr key={user.id}>
                            <td>
                                <input
                                    type={"checkbox"}
                                    checked={selectedIds.includes(user.id)}
                                    onChange={() => toggleSelectId(user.id)}
                                    className={styles.usersCheckbox}
                                />
                            </td>
                            <td>
                                <div
                                    className={`${styles.role} ${styles.basic} ${styles.ID}`}
                                >
                                    {user.id}
                                </div>
                            </td>
                            <td>
                                <div className={styles.name}>
                                    <span>{user.fullName}</span>
                                </div>
                            </td>
                            <td>
                                <div
                                    className={`${styles.role} ${
                                        user.role === "ADMIN"
                                            ? styles.admin
                                            : user.role === "SPECIAL"
                                              ? styles.special
                                              : styles.basic
                                    }`}
                                >
                                    {user.role}
                                </div>
                            </td>
                            <td>
                                <div
                                    className={`${styles.role} ${
                                        user.tariffName === "PREMIUM"
                                            ? styles.special
                                            : user.tariffName === "TRIAL"
                                              ? styles.dynamic
                                              : user.tariffName === "BASIC"
                                                ? styles.basic
                                                : styles.none
                                    }`}
                                >
                                    {user.tariffName}
                                </div>
                            </td>
                            <td>
                                <div
                                    className={`${styles.role} ${
                                        user.subscriptionStatus === "ACTIVE"
                                            ? styles.dynamic
                                            : user.subscriptionStatus ===
                                                "EXPIRED"
                                              ? styles.expired
                                              : user.subscriptionStatus ===
                                                  "CANCELLED"
                                                ? styles.basic
                                                : styles.none
                                    }`}
                                >
                                    {user.subscriptionStatus}
                                </div>
                            </td>
                            <td>
                                <div className={styles.date}>
                                    <span style={{ color: "gray" }}>
                                        {user.endDate?.toString().slice(0, 10)}
                                    </span>
                                    <EllipsisVertical
                                        className={styles.ellipsis}
                                        size={20}
                                        onClick={() => toggleMenu(user.id)}
                                    />
                                </div>

                                {isOpen === user.id && (
                                    <div className={styles.modal} ref={menuRef}>
                                        <div className={styles.modalName}>
                                            <span>{user.fullName}</span>
                                            <span style={{ color: "gray" }}>
                                                ID: {user.id}
                                            </span>
                                        </div>
                                        <div className={styles.modalChange}>
                                            {apiOpen ? (
                                                <>
                                                    <div
                                                        className={
                                                            styles.modalChangeHeader
                                                        }
                                                    >
                                                        <span
                                                            style={{
                                                                color: "gray",
                                                            }}
                                                        >
                                                            API ключ для
                                                            Telegram:
                                                        </span>
                                                        <div
                                                            style={{
                                                                color: "orange",
                                                            }}
                                                        >
                                                            <Timer size={20} />
                                                            <span>
                                                                Действителен 30
                                                                минут
                                                            </span>
                                                        </div>
                                                    </div>
                                                    <div
                                                        className={
                                                            styles.apiKey
                                                        }
                                                    >
                                                        <span>{ApiKey}</span>
                                                    </div>
                                                    <div
                                                        className={
                                                            styles.apiButton
                                                        }
                                                    >
                                                        <button
                                                            onClick={() =>
                                                                navigator.clipboard.writeText(
                                                                    ApiKey,
                                                                )
                                                            }
                                                            className="copy"
                                                        >
                                                            Копировать
                                                        </button>
                                                        <button
                                                            className="hide"
                                                            onClick={() =>
                                                                setApiOpen(
                                                                    false,
                                                                )
                                                            }
                                                        >
                                                            Скрыть
                                                        </button>
                                                    </div>
                                                </>
                                            ) : (
                                                <div
                                                    onClick={() =>
                                                        setApiOpen(true)
                                                    }
                                                >
                                                    <Key size={17} />
                                                    Получить API ключ
                                                </div>
                                            )}
                                            <NavLink
                                                className="navLink"
                                                to={`/users/edit/${user.id}`}
                                            >
                                                <div>
                                                    <Pencil size={17} />
                                                    Редактировать
                                                </div>
                                            </NavLink>
                                            <div
                                                onClick={() =>
                                                    handleDeleteUser()
                                                }
                                            >
                                                <Trash2 size={17} />
                                                Удалить
                                            </div>
                                        </div>
                                    </div>
                                )}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}