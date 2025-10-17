import { useState, useRef, useEffect } from "react";
import {MoveVertical, EllipsisVertical, Key, Pencil, Trash2, Timer} from 'lucide-react';
import {ArrowUp, ArrowDown} from 'react-feather'
import './TableAllUsers.css'
import ReactDOM from "react-dom";
import {NavLink} from "react-router";
import Countries from "../Countries/Countries.tsx";

const Users = [
    {
        id: 1,
        firstname: "Алексей",
        lastname: "Петров",
        telegramId: "",
        role: "SPECIAL",
        type: "TRIAL",
        status: "CANCELLED",
        countries: ["FI", "US", "RU", "NL", "DE", "SG"],
        dateStart: "2021-02-11",
        dateFinish: "2021-05-02"
    },
    {
        id: 2,
        firstname: "Алексей",
        lastname: "Смирнов",
        telegramId: "@her",
        role: "BASIC",
        type: "BASIC",
        status: "EXPIRED",
        countries: ["RU", "FI"],
        dateStart: "2021-03-13",
        dateFinish: "2021-08-11"
    },
    {
        id: 3,
        firstname: "Максим",
        lastname: "Петров",
        telegramId: "",
        role: "ADMIN",
        type: "PREMIUM",
        status: "ACTIVE",
        countries: ["RU"],
        dateStart: "2021-01-06",
        dateFinish: "2021-05-02"
    },
    {
        id: 4,
        firstname: "Максим",
        lastname: "Петров",
        telegramId: "@pebb",
        role: "BASIC",
        type: "NONE",
        status: "NONE",
        countries: ["RU"],
        dateStart: "2021-03-20",
        dateFinish: "2021-05-02"
    }
]

const ApiKey = "tg_api_1_570528";

const titles = ["ID", "Пользователь", "Роль", "Тип подписки", "Статус", "Страна", "Срок действия"]

export default function TableAllUsers() {
    const [isOpen, setIsOpen] = useState(-1);
    const [selectedUser, setSelectedUser] = useState<null | { id: number; country: string}>(null);
    const [apiOpen, setApiOpen] = useState(false);
    const [sort, setSort] = useState<string[]>(["", ""]);
    const menuRef = useRef<HTMLDivElement>(null);

    const handleOpen = (userId: number, country: string) => {
        setSelectedUser({ id: userId, country });
    };

    const handleClose = () => setSelectedUser(null);

    const handleSort = (title: string) => {
        sort[1] === "" ? setSort([title, "down"]) :
        sort[1] === "down" ? setSort([title, "up"]) :
        setSort(["", ""])
    }

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (!(event.target instanceof Node)) return;
            if (menuRef.current && !menuRef.current.contains(event.target)) {
                setIsOpen(-1);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    return (
        <div className="allusers-table" >
            <table className="table-users">
                <tbody>
                <tr>
                    {titles.map((title) => (
                        <td key={title}>
                            <div className={`table-title ${title}`}>
                                <span>{title}</span>
                                <div onClick={() => handleSort(title)}>
                                    {sort[0] === title ?
                                        sort[1] === "down" ? (<ArrowDown size={20}/>) :
                                        sort[1] === "up" ? (<ArrowUp size={20}/>) :
                                        <MoveVertical size={20}/> :
                                        <MoveVertical size={20}/>}
                                </div>
                            </div>
                        </td>
                    ))}
                </tr>

                {Users.map((user) => (
                    <tr key={user.id}>
                        <td><div className="role basic ID">{user.id}</div></td>
                        <td>
                            <div className="name">
                                <span>{user.firstname} {user.lastname}</span>
                            </div>
                        </td>
                        <td>
                            <div className={`role ${user.role === "ADMIN" ? "admin" : user.role === "SPECIAL" ? "special" : "basic"}`}>
                                {user.role}
                            </div>
                        </td>
                        <td>
                            <div className={`role ${user.type === "PREMIUM" ? "special" : user.type === "TRIAL" ? "dynamic" : user.type === "BASIC" ? "basic" : "none"}`}>
                                {user.type}
                            </div>
                        </td>
                        <td>
                            <div className={`role ${user.status === "ACTIVE" ? "dynamic" : user.status === "EXPIRED" ? "expired" : user.status === "CANCELLED" ? "basic" : "none"}`}>
                                {user.status}
                            </div>
                        </td>

                        <td>
                            <div className="countries">
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
                            <div className="date">
                                <span style={{ color: "gray" }}>{user.dateFinish}</span>
                                <EllipsisVertical
                                    className="ellipsis"
                                    size={20}
                                    onClick={
                                        isOpen === user.id
                                            ? () => setIsOpen(-1)
                                            : () => setIsOpen(user.id)
                                    }
                                />
                            </div>

                            {isOpen === user.id && (
                                <div className="modal" ref={menuRef}>
                                    <div className="modal-name">
                                        <span>{user.firstname} {user.lastname}</span>
                                        <span style={{ color: "gray" }}>ID: {user.id}</span>
                                    </div>
                                    <div className="modal-change">
                                        {apiOpen ? (
                                            <>
                                                <div className={"modal-change-header"}>
                                                    <span style={{ color: "gray" }}>API ключ для Telegram:</span>
                                                    <div style={{ color: "orange" }}>
                                                        <Timer size={20}/>
                                                        <span>Дествителен 30 минут</span>
                                                    </div>
                                                </div>
                                                <div className="apiKey"><span>{ApiKey}</span></div>
                                                <div className="apiButton">
                                                    <button onClick={() => navigator.clipboard.writeText(ApiKey)} className="copy">Копировать</button>
                                                    <button className="hide" onClick={() => setApiOpen(false)}>Скрыть</button>
                                                </div>
                                            </>
                                        ) : (
                                            <div onClick={() => setApiOpen(true)}>
                                                <Key size={17}/>Получить API ключ
                                            </div>
                                        )}
                                        <NavLink
                                            className="nav-link"
                                            to="/changeUsers"
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
                                            <div><Pencil size={17}/>Редактировать</div>
                                        </NavLink>
                                        <div><Trash2 size={17}/>Удалить</div>
                                    </div>
                                </div>
                            )}
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>

            {selectedUser && (() => {
                const user = Users.find(u => u.id === selectedUser.id);
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
                    document.body
                );
            })()}
        </div>
    );
}
