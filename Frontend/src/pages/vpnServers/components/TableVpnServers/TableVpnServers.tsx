import { useRef, useEffect, useState } from "react";
import {
    EllipsisVertical,
    Settings,
    Trash2,
    RefreshCw,
    Wrench,
    ClipboardList,
} from "lucide-react";
import styles from "./TableVpnServers.module.css";
import { NavLink } from "react-router";
import useServersStore from "../../../../store/useServersStore";
import { ServerService } from "@api/services/serverService.ts";

const titles = [
    "СЕРВЕР",
    "Статус",
    "Нагрузка",
    "Трафик",
    "Пинг",
    "Uptime",
];

export default function TableVpnServers() {
    const [windowOpen, setWindowOpen] = useState(false);
    const {
        servers,
        fetchServers,
        isOpen,
        setIsOpen,
        setIsChangeOpen,
    } = useServersStore();
    const menuRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (!(event.target instanceof Node)) return;
            if (menuRef.current && !menuRef.current.contains(event.target)) {
                setWindowOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () =>
            document.removeEventListener("mousedown", handleClickOutside);
    }, [setWindowOpen]);

    const handleDeleteServer = async () => {
        try {
            await ServerService.deleteServer(isOpen);
            await fetchServers({});
        } catch (error) {
            console.error(error);
        }
    };
    
    return (
        <div className={styles.vpnserversTable}>
            <table className={styles.tableServers}>
                <tbody>
                    <tr>
                        {titles.map((title) => (
                            <td key={title}>
                                <div
                                    className={`${styles.tableTitle} ${styles[title]}`}
                                >
                                    <span>{title}</span>
                                    {/*title !== "Сервер" ? (
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
                                ) : null*/}
                                </div>
                            </td>
                        ))}
                    </tr>

                    {servers.map((server) => (
                        <tr key={server.id}>
                            <td>
                                <div className={styles.serverName}>
                                    <span className={styles.name}>
                                        {server.name}
                                    </span>
                                    <span className={styles.location}>
                                        {server.location}
                                    </span>
                                    <span className={styles.ip}>
                                        {server.ip}
                                    </span>
                                </div>
                            </td>
                            <td>
                                <div
                                    className={`${styles.status} ${
                                        server.status === "ACTIVE"
                                            ? styles.online
                                            : server.status === "INACTIVE"
                                              ? styles.offline
                                              : styles.maintenance
                                    }`}
                                >
                                    {server.status}
                                </div>
                            </td>
                            <td>
                                <div className={styles.progressContainer}>
                                    <div className={styles.progressBar}>
                                        <div
                                            className={`${styles.progressBarFill} ${
                                                server.load < 50
                                                    ? styles.backgroundcolorGreen
                                                    : server.load < 80
                                                      ? styles.backgroundcolorOrange
                                                      : styles.backgroundcolorRed
                                            }`}
                                            style={{ width: `${server.load}%` }}
                                        ></div>
                                    </div>
                                    <span className={styles.progressLabel}>
                                        {server.load}%
                                    </span>
                                </div>
                            </td>
                            <td>
                                <div className={styles.users}>
                                    <span>
                                        {server.usage === null
                                            ? `0`
                                            : `${server.usage}`}{" "}
                                        / {server.maxTraffic}
                                    </span>
                                </div>
                            </td>
                            <td>
                                <div>
                                    <span>{server.ping}</span>
                                </div>
                            </td>
                            <td>
                                <div className={styles.uptime}>
                                    <span>{server.uptime}%</span>
                                    <EllipsisVertical
                                        className={styles.ellipsis}
                                        size={20}
                                        onClick={
                                            ()=> {
                                                setWindowOpen(true)
                                                setIsOpen(server.id)
                                            }
                                        }
                                    />
                                </div>

                                {(windowOpen && isOpen === server.id) && (
                                    <div
                                        className={styles.vpnserversModal}
                                        ref={menuRef}
                                    >
                                        <div
                                            className={
                                                styles.vpnserversModalName
                                            }
                                        >
                                            <span className={styles.name}>
                                                {server.name}
                                            </span>
                                            <span className={styles.location}>
                                                {server.location}
                                            </span>
                                            <span className={styles.ip}>
                                                {server.ip}
                                            </span>
                                        </div>
                                        <div
                                            className={
                                                styles.vpnserversModalChange
                                            }
                                        >
                                            <div
                                                className={
                                                    styles.vpnserversModalChangeMain
                                                }
                                            >
                                                <div
                                                    onClick={() => {
                                                        setIsOpen(server.id);
                                                        setIsChangeOpen();
                                                    }}
                                                >
                                                    <Settings size={17} />
                                                    Настройки сервера
                                                </div>
                                                <div>
                                                    <RefreshCw size={17} />
                                                    Перезагрузить
                                                </div>
                                                <div>
                                                    <Wrench size={17} />
                                                    Режим обслуживания
                                                </div>
                                                <NavLink
                                                    to={"/servers/log"}
                                                    className={"navLink"}
                                                >
                                                    <div>
                                                        <ClipboardList
                                                            size={17}
                                                        />
                                                        Просмотр логов
                                                    </div>
                                                </NavLink>
                                            </div>
                                            <div
                                                className={styles.delete}
                                                onClick={() =>
                                                    handleDeleteServer()
                                                }
                                            >
                                                <Trash2 size={17} />
                                                Удалить сервер
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