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
import ReactDOM from "react-dom";
import { NavLink } from "react-router";
import Countries from "../Countries/Countries.tsx";
import useUsersStore from "../../../../store/useUsersStore.ts";

const ApiKey = "tg_api_1_570528";

const titles = [
    "ID",
    "Пользователь",
    "Роль",
    "Тип подписки",
    "Статус",
    "Страна",
    "Срок действия",
];

export default function TableAllUsers() {
    const {
        users,
        sort,
        selectedUser,
        apiOpen,
        isOpen,
        setSort,
        setSelectedUser,
        setApiOpen,
        toggleMenu,
        resetMenu,
    } = useUsersStore();

    const menuRef = useRef<HTMLDivElement>(null);

    const handleOpen = (userId: number, country: string) => {
        setSelectedUser({ id: userId, country });
    };

    const handleClose = () => resetMenu();

    const handleSort = (title: string) => {
        let newDirection: "up" | "down" | "" = "";
        if (sort.field === title) {
            if (sort.direction === "down") newDirection = "up";
            else if (sort.direction === "up") newDirection = "";
        } else {
            newDirection = "down";
        }
        setSort(title, newDirection);
    };

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

    const sortedUsers = [...users].sort((a, b) => {
        if (!sort.field) return 0;

        const aValue = a[sort.field as keyof typeof a];
        const bValue = b[sort.field as keyof typeof b];

        if (sort.direction === "up") {
            return String(aValue).localeCompare(String(bValue));
        } else if (sort.direction === "down") {
            return String(bValue).localeCompare(String(aValue));
        }
        return 0;
    });

    return (
        <div className={styles.allusersTable}>
            <table className={styles.tableUsers}>
                <tbody>
                <tr>
                    {titles.map((title) => (
                        <td key={title}>
                            <div className={`${styles.tableTitle} ${styles[title]}`}>
                                <span>{title}</span>
                                <div onClick={() => handleSort(title)}>
                                    {sort.field === title ? (
                                        sort.direction === "down" ? (
                                            <ArrowDown size={20} />
                                        ) : sort.direction === "up" ? (
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

                {sortedUsers.map((user) => (
                    <tr key={user.id}>
                        <td>
                            <div className={`${styles.role} ${styles.basic} ${styles.ID}`}>
                                {user.id}
                            </div>
                        </td>
                        <td>
                            <div className={styles.name}>
                                    <span>
                                        {user.firstname} {user.lastname}
                                    </span>
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
                                    user.type === "PREMIUM"
                                        ? styles.special
                                        : user.type === "TRIAL"
                                            ? styles.dynamic
                                            : user.type === "BASIC"
                                                ? styles.basic
                                                : styles.none
                                }`}
                            >
                                {user.type}
                            </div>
                        </td>
                        <td>
                            <div
                                className={`${styles.role} ${
                                    user.status === "ACTIVE"
                                        ? styles.dynamic
                                        : user.status === "EXPIRED"
                                            ? styles.expired
                                            : user.status === "CANCELLED"
                                                ? styles.basic
                                                : styles.none
                                }`}
                            >
                                {user.status}
                            </div>
                        </td>

                        <td>
                            <div className={"countries"}>
                                {user.countries.map((country) => (
                                    <span
                                        key={`${user.id}-${country}`}
                                        onClick={() => handleOpen(user.id, country)}
                                        style={{ cursor: "pointer" }}
                                    >
                                            {country}
                                        </span>
                                ))}
                            </div>
                        </td>

                        <td>
                            <div className={styles.date}>
                                    <span style={{ color: "gray" }}>
                                        {user.dateFinish}
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
                                            <span>
                                                {user.firstname} {user.lastname}
                                            </span>
                                        <span style={{ color: "gray" }}>
                                                ID: {user.id}
                                            </span>
                                    </div>
                                    <div className={styles.modalChange}>
                                        {apiOpen ? (
                                            <>
                                                <div className={styles.modalChangeHeader}>
                                                        <span style={{ color: "gray" }}>
                                                            API ключ для Telegram:
                                                        </span>
                                                    <div style={{ color: "orange" }}>
                                                        <Timer size={20} />
                                                        <span>Действителен 30 минут</span>
                                                    </div>
                                                </div>
                                                <div className={styles.apiKey}>
                                                    <span>{ApiKey}</span>
                                                </div>
                                                <div className={styles.apiButton}>
                                                    <button
                                                        onClick={() =>
                                                            navigator.clipboard.writeText(ApiKey)
                                                        }
                                                        className="copy"
                                                    >
                                                        Копировать
                                                    </button>
                                                    <button
                                                        className="hide"
                                                        onClick={() => setApiOpen(false)}
                                                    >
                                                        Скрыть
                                                    </button>
                                                </div>
                                            </>
                                        ) : (
                                            <div onClick={() => setApiOpen(true)}>
                                                <Key size={17} />
                                                Получить API ключ
                                            </div>
                                        )}
                                        <NavLink
                                            className="navLink"
                                            to="/users/edit"
                                            state={{
                                                props: {
                                                    id: user.id,
                                                    firstname: user.firstname,
                                                    lastname: user.lastname,
                                                    role: user.role,
                                                    type: user.type,
                                                    telegramId: user.telegramId,
                                                    dateFinish: user.dateFinish,
                                                    countries: user.countries,
                                                    status: user.status,
                                                    dateStart: user.dateStart,
                                                },
                                            }}
                                        >
                                            <div>
                                                <Pencil size={17} />
                                                Редактировать
                                            </div>
                                        </NavLink>
                                        <div>
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

            {selectedUser &&
                (() => {
                    const user = sortedUsers.find((u) => u.id === selectedUser.id);
                    if (!user) return null;
                    return ReactDOM.createPortal(
                        <Countries
                            userId={selectedUser.id}
                            specificCountry={selectedUser.country}
                            firstName={user.firstname}
                            lastName={user.lastname}
                            countries={user.countries}
                            onClose={handleClose}
                        />,
                        document.body,
                    );
                })()}
        </div>
    );
}