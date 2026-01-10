import { useRef, useEffect } from "react";
import {
    MoveVertical,
    EllipsisVertical,
    Settings,
    Trash2,
    RefreshCw,
    Wrench,
    ClipboardList,
} from "lucide-react";
import { ArrowUp, ArrowDown } from "react-feather";
import styles from "./TableVpnServers.module.css";
import { NavLink } from "react-router";
import useServersStore from "../../../../store/useServersStore";

const titles = [
    "Сервер",
    "Статус",
    "Загрузка",
    "Пользователи",
    "Пинг",
    "Uptime",
    "Пропускная способность",
];

export default function TableVpnServers() {
    const { servers, sort, isOpen, setIsOpen, setSort } = useServersStore();
    const menuRef = useRef<HTMLDivElement>(null);

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
                setIsOpen(-1);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () =>
            document.removeEventListener("mousedown", handleClickOutside);
    }, [setIsOpen]);

    return (
        <div className={styles.vpnserversTable}>
            <table className={styles.tableServers}>
                <tbody>
                <tr>
                    {titles.map((title) => (
                        <td key={title}>
                            <div className={`${styles.tableTitle} ${styles[title]}`}>
                                <span>{title}</span>
                                {title !== "Сервер" ? (
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
                                ) : null}
                            </div>
                        </td>
                    ))}
                </tr>

                {servers.map((server) => (
                    <tr key={server.id}>
                        <td>
                            <div className={styles.serverName}>
                                <span className={styles.name}>{server.name}</span>
                                <span className={styles.location}>{server.location}</span>
                                <span className={styles.ip}>{server.ip}</span>
                            </div>
                        </td>
                        <td>
                            <div
                                className={`${styles.status} ${
                                    server.status === "ONLINE"
                                        ? styles.online
                                        : server.status === "OFFLINE"
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
                                        {server.usage === null ? `0` : `${server.usage}`} / {server.maxUsers}
                                    </span>
                            </div>
                        </td>
                        <td>
                            <div
                                className={`${styles.ping} ${
                                    server.ping < 50
                                        ? styles.colorGreen
                                        : server.ping < 100
                                            ? styles.colorOrange
                                            : styles.colorRed
                                }`}
                            >
                                <span>{server.ping}ms</span>
                            </div>
                        </td>
                        <td>
                            <div className={styles.uptime}>
                                <span>{server.uptime}%</span>
                            </div>
                        </td>
                        <td>
                            <div className={styles.maxtrafficDot}>
                                    <span className={styles.maxtraffic}>
                                        {server.maxTraffic < 1024
                                            ? `${server.maxTraffic} Mbps`
                                            : `${(server.maxTraffic / 1024).toFixed(0)} Gbps`}
                                    </span>
                                <EllipsisVertical
                                    className={styles.ellipsis}
                                    size={20}
                                    onClick={
                                        isOpen === server.id
                                            ? () => setIsOpen(-1)
                                            : () => setIsOpen(server.id)
                                    }
                                />
                            </div>

                            {isOpen === server.id && (
                                <div className={styles.vpnserversModal} ref={menuRef}>
                                    <div className={styles.vpnserversModalName}>
                                        <span className={styles.name}>{server.name}</span>
                                        <span className={styles.location}>{server.location}</span>
                                        <span className={styles.ip}>{server.ip}</span>
                                    </div>
                                    <div className={styles.vpnserversModalChange}>
                                        <div className={styles.vpnserversModalChangeMain}>
                                            <NavLink to={"/servers/edit"} className={"navLink"}>
                                                <div>
                                                    <Settings size={17} />
                                                    Настройки сервера
                                                </div>
                                            </NavLink>
                                            <div>
                                                <RefreshCw size={17} />
                                                Перезагрузить
                                            </div>
                                            <div>
                                                <Wrench size={17} />
                                                Режим обслуживания
                                            </div>
                                            <NavLink to={"/servers/log"} className={"navLink"}>
                                                <div>
                                                    <ClipboardList size={17} />
                                                    Просмотр логов
                                                </div>
                                            </NavLink>
                                        </div>
                                        <div className={styles.delete}>
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