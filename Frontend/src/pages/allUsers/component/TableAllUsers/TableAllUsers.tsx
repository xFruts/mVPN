import { useEffect } from "react";
import {
    EllipsisVertical,
    ChevronsUpDown,
    ChevronUp,
    ChevronDown,
} from "lucide-react";
import styles from "./TableAllUsers.module.css";
import { NavLink } from "react-router";
import useUsersStore from "../../../../store/useUsersStore.ts";
import type {
    SortUserFields,
    SortDirection,
    SortUserData,
    GetUserData,
} from "@/types/general.ts";
import {UserService} from "@api/services/userService.ts";
import Configuration from "@pages/allUsers/component/Configuration/Configuration.tsx";
import type {TitlesUser} from "@/types/general.ts";
import { Pagination } from "@pages/shared/pagination/Pagination.tsx"


const titles: TitlesUser[] = [
    {
        name: "ID",
        api: "id",
        side: "flex-start",
    },
    {
        name: "ПОЛЬЗОВАТЕЛЬ",
        api: "fullName",
        side: "flex-start",
    },
    {
        name: "РОЛЬ",
        api: "role",
        side: "center",
    },
    {
        name: "ТАРИФ",
        api: "tariffName",
        side: "center",
    },
    {
        name: "СТАТУС",
        api: "subStatus",
        side: "center",
    },
    {
        name: "СРОК ДЕЙСТВИЯ",
        api: "subEndDate",
        side: "flex-start",
    },
];

export default function TableAllUsers() {
    const {
        users,
        totalPages,
        totalElements,
        filters,
        fetchUsers,
        selectedIds,
        isOpen,
        toggleSelectId,
        toggleAll,
        toggleMenu,
        resetMenu,
        selectedConfigUserId,
        setConfiguration,
        error
    } = useUsersStore();
    const [currentField, currentDir] = (filters.sort || "id,asc").split(
        ",",
    ) as [SortUserFields, SortDirection];
    const allUserIds = users.map((u) => u.id);
    const handleSort = (title: SortUserFields) => {

        let newSort: SortUserData;

        if (currentField !== title) {
            newSort = `${title},asc`;
        } else if (currentDir === "asc") {
            newSort = `${title},desc`;
        } else {
            newSort = "id,asc";
        }

        void fetchUsers({ sort: newSort });
    };

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            const target = event.target as HTMLElement;

            if (target.closest(`.${styles.modal}`)) {
                return;
            }

            if (target.closest(`.${styles.ellipsis}`)) {
                return;
            }

            resetMenu();
        };

        document.addEventListener("click", handleClickOutside);
        return () => document.removeEventListener("click", handleClickOutside);
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
        <div className={styles.allUsersTable}>
            <table className={`${styles.tableUsers} ${"PC"}`}>
                <tbody>
                    {totalPages > 0 && (
                        <tr className={styles.trTitle}>
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
                                        className={`${styles.tableTitle}`}
                                        onClick={() => handleSort(title.api)}
                                        style={{ justifyContent: title.side }}
                                    >
                                        <span>{title.name}</span>
                                        <div>
                                            {currentField === title.api ? (
                                                currentDir === "asc" ? (
                                                    <ChevronDown size={12} />
                                                ) : currentDir === "desc" ? (
                                                    <ChevronUp size={12} />
                                                ) : (
                                                    <ChevronsUpDown size={12} />
                                                )
                                            ) : (
                                                <ChevronsUpDown size={12} />
                                            )}
                                        </div>
                                    </div>
                                </td>
                            ))}
                            <td></td>
                        </tr>
                    )}

                    {totalElements > 0 &&
                        users.map((user) => (
                            <tr
                                key={user.id}
                                className={`${styles.trLine} ${selectedIds.includes(user.id) ? styles.selectedRow : ""}`}
                                onClick={() => toggleSelectId(user.id)}
                            >
                                <td>
                                    <input
                                        type={"checkbox"}
                                        checked={selectedIds.includes(user.id)}
                                        className={styles.usersCheckbox}
                                        readOnly
                                    />
                                </td>
                                <td>
                                    <div className={`${styles.ID}`}>
                                        <span>{user.id}</span>
                                    </div>
                                </td>
                                <td>
                                    <div className={styles.name}>
                                        <span>{user.fullName}</span>
                                    </div>
                                </td>
                                <td className={styles.center}>
                                    <div
                                        className={`${styles.role} ${
                                            user.role === "ADMIN"
                                                ? styles.admin
                                                : user.role === "SPECIAL"
                                                  ? styles.special
                                                  : styles.regular
                                        } ${styles.center}`}
                                    >
                                        {user.role}
                                    </div>
                                </td>
                                <td className={styles.center}>
                                    <div
                                        className={`${styles.role} ${styles.regular}`}
                                    >
                                        {user.tariffName}
                                    </div>
                                </td>
                                <td className={styles.center}>
                                    <div
                                        className={`${styles.role} ${
                                            user.subscriptionStatus === "ACTIVE"
                                                ? styles.active
                                                : user.subscriptionStatus ===
                                                    "EXPIRED"
                                                  ? styles.expired
                                                  : styles.regular
                                        }`}
                                    >
                                        {user.subscriptionStatus}
                                    </div>
                                </td>
                                <td>
                                    <div className={styles.date}>
                                        <span>
                                            {user.endDate
                                                ?.toString()
                                                .slice(0, 10)}
                                        </span>
                                    </div>
                                </td>
                                <td className={styles.end}>
                                    <div>
                                        <EllipsisVertical
                                            className={styles.ellipsis}
                                            size={16}
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                toggleMenu(user.id);
                                            }}
                                        />
                                    </div>
                                    {isOpen === user.id && (
                                        <div
                                            className={styles.modal}
                                            onClick={(e) => e.stopPropagation()}
                                        >
                                            <div>
                                                <NavLink
                                                    className={`${"navLink"}`}
                                                    to={`/users/edit/${user.id}`}
                                                >
                                                    <span>Изменить</span>
                                                </NavLink>
                                            </div>
                                            <div
                                                onClick={() => {
                                                    setConfiguration(user.id);
                                                }}
                                            >
                                                <span>Конфиг</span>
                                            </div>
                                            <div
                                                className={styles.delete}
                                                onClick={() =>
                                                    handleDeleteUser()
                                                }
                                            >
                                                Удалить
                                            </div>
                                        </div>
                                    )}
                                </td>
                            </tr>
                        ))}
                </tbody>
            </table>
            <div className={`${styles.listUsers} ${"Mobile"}`}>
                {users.map((user) => (
                    <div className={styles.user} key={user.id}>
                        <div className={styles.info}>
                            <input
                                type={"checkbox"}
                                checked={selectedIds.includes(user.id)}
                                className={styles.usersCheckbox}
                                onChange={() => toggleSelectId(user.id)}
                            />
                            <div className={styles.name}>
                                <span>{user.fullName}</span>
                                <span className={styles.date}>
                                    Тариф до:{" "}
                                    {user.endDate?.toString().slice(0, 10)}
                                </span>
                            </div>
                        </div>
                        <div>
                            <EllipsisVertical
                                className={styles.ellipsis}
                                size={16}
                                onClick={(e) => {
                                    e.stopPropagation();
                                    toggleMenu(user.id);
                                }}
                            />
                        </div>
                        {isOpen === user.id && (
                            <div
                                className={styles.modal}
                                onClick={(e) => e.stopPropagation()}
                            >
                                <div>
                                    <NavLink
                                        className={`${"navLink"}`}
                                        to={`/users/edit/${user.id}`}
                                    >
                                        <span>Изменить</span>
                                    </NavLink>
                                </div>
                                <div
                                    onClick={() => {
                                        setConfiguration(user.id);
                                    }}
                                >
                                    <span>Конфиг</span>
                                </div>
                                <div
                                    className={styles.delete}
                                    onClick={() => handleDeleteUser()}
                                >
                                    Удалить
                                </div>
                            </div>
                        )}
                    </div>
                ))}
            </div>
            {totalElements > 0 && (
                <Pagination
                    filters={filters as GetUserData}
                    fetchData={fetchUsers}
                    totalElements={totalElements}
                    totalPages={totalPages}
                />
            )}
            {selectedConfigUserId && (
                <Configuration userId={selectedConfigUserId} />
            )}
            {totalElements === 0 && (
                <div className={styles.trMessage}>
                    <div className={styles.tableMessage}>
                        {error ? (
                            <span className={styles.error}>
                                Ошибка {error.statusCode}: {error.message}
                            </span>
                        ) : (
                            <span>Пользователи не найдены</span>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}