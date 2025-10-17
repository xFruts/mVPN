import { useState, useRef, useEffect } from "react";
import {MoveVertical, EllipsisVertical, Settings, Trash2, RefreshCw, Wrench, ClipboardList} from 'lucide-react';
import {ArrowUp, ArrowDown} from 'react-feather'
import './TableVpnServers.css'
import {NavLink} from "react-router";

const Servers = [
    {
        id: 1,
        name: "server1",
        location: "Finland",
        ip: "94.183.190.23",
        status: "ONLINE",
        load: 67,
        usage: 20,
        maxUsers: 50,
        maxTraffic: 300,
        ping: 3,
        uptime: 66.67
    },
    {
        id: 2,
        name: "server2",
        location: "Russia",
        ip: "94.183.190.23",
        status: "OFFLINE",
        load: 50,
        usage: null,
        maxUsers: 100,
        maxTraffic: 1024,
        ping: 17,
        uptime: 50.00
    },
    {
        id: 3,
        name: "server3",
        location: "USA",
        ip: "94.183.190.23",
        status: "MAINTENANCE",
        load: 90,
        usage: 100,
        maxUsers: 150,
        maxTraffic: 1024,
        ping: 57,
        uptime: 99.90
    },
    {
        id: 4,
        name: "server4",
        location: "Germany",
        ip: "94.183.190.23",
        status: "ONLINE",
        load: 30,
        usage: 10,
        maxUsers: 25,
        maxTraffic: 500,
        ping: 103,
        uptime: 90.00
    }
]

const titles = ["Сервер", "Статус", "Загрузка", "Пользователи", "Пинг", "Uptime", "Пропускная способность"]

export default function TableVpnServers() {
    const [isOpen, setIsOpen] = useState(-1);
    const [sort, setSort] = useState<string[]>(["", ""]);
    const menuRef = useRef<HTMLDivElement>(null);


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
        <div className="vpnservers-table">
            <table className="table-servers">
                <tbody>
                <tr>
                    {titles.map((title) => (
                        <td key={title}>
                            <div className={`table-title ${title}`}>
                                <span>{title}</span>
                                {
                                    title !== "Сервер" ? (
                                        <div onClick={() => handleSort(title)}>
                                            {sort[0] === title ?
                                                sort[1] === "down" ? (<ArrowDown size={20}/>) :
                                                    sort[1] === "up" ? (<ArrowUp size={20}/>) :
                                                        <MoveVertical size={20}/> :
                                                <MoveVertical size={20}/>}
                                        </div>
                                    ) : null
                                }
                            </div>
                        </td>
                    ))}
                </tr>

                {Servers.map((server) => (
                    <tr key={server.id}>
                        <td>
                            <div className="server-name">
                                <span className="name">{server.name}</span>
                                <span className="location">{server.location}</span>
                                <span className="ip">{server.ip}</span>
                            </div>
                        </td>
                        <td>
                            <div className={`status ${server.status === "ONLINE" ? "online" : server.status === "OFFLINE" ? "offline" : "maintenance"}`}>
                                {server.status}
                            </div>
                        </td>
                        <td>
                            <div className="progress-container">
                                <div className="progress-bar">
                                    <div className={`progress-bar-fill ${server.load < 50 ? `backgroundcolor-green` : server.load < 80 ? `backgroundcolor-orange` : `backgroundcolor-red`}`} style={{width: `${server.load}%`}}></div>
                                </div>
                                <span className="progress-label">{server.load}%</span>
                            </div>
                        </td>
                        <td>
                            <div className={"users"}>
                                <span>{server.usage === null ? `0` : `${server.usage}`} / {server.maxUsers}</span>
                            </div>
                        </td>

                        <td>
                            <div className={`ping ${server.ping < 50 ? `color-green` : server.ping < 100 ? `color-orange` : `color-red`}`}>
                                <span>{server.ping}ms</span>
                            </div>
                        </td>
                        <td>
                            <div className="uptime">
                                <span>{server.uptime}%</span>
                            </div>
                        </td>
                        <td>
                            <div className={"maxtraffic-dot"}>
                                <span className={"maxtraffic"}>
                                    {server.maxTraffic < 1024
                                        ? `${server.maxTraffic} Mbps`
                                        : `${(server.maxTraffic / 1024).toFixed(0)} Gbps`
                                    }
                                </span>
                                <EllipsisVertical
                                    className="ellipsis"
                                    size={20}
                                    onClick={
                                        isOpen === server.id
                                            ? () => setIsOpen(-1)
                                            : () => setIsOpen(server.id)
                                    }
                                />
                            </div>

                            {isOpen === server.id && (
                                <div className="vpnservers-modal" ref={menuRef}>
                                    <div className="vpnservers-modal-name">
                                        <span className="name">{server.name}</span>
                                        <span className="location">{server.location}</span>
                                        <span className="ip">{server.ip}</span>
                                    </div>
                                    <div className="vpnservers-modal-change">
                                        <div className={"vpnservers-modal-change-main"}>
                                            <NavLink to={"/settingsServer"} className="nav-link">
                                                <div><Settings size={17}/>Настройки сервера</div>
                                            </NavLink>
                                            <div><RefreshCw size={17}/>Перезагрузить</div>
                                            <div><Wrench size={17}/>Режим обслуживания</div>
                                            <NavLink to={"/logsServer"} className="nav-link">
                                                <div><ClipboardList size={17}/>Просмотр логов</div>
                                            </NavLink>

                                        </div>
                                        <div className={"delete"}><Trash2 size={17}/>Удалить сервер</div>
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
