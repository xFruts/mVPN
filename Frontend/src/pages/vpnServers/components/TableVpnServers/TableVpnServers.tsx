import { useEffect, useState } from "react";
import {
    EllipsisVertical,
    ChevronDown,
    ChevronUp,
    ChevronsUpDown,
} from "lucide-react";
import styles from "./TableVpnServers.module.css";
import useServersStore from "../../../../store/useServersStore";
import { ServerService } from "@api/services/serverService.ts";
import type {
    GetServerData,
    SortDirection,
    SortServerData,
    SortServerFields,
    TitlesServer,
} from "@/types/general.ts";
import {Pagination} from "@pages/shared/pagination/Pagination.tsx";


const titles: TitlesServer[] = [
    {
        name: "СЕРВЕР",
        api: "name",
        side: "flex-start",
    },
    {
        name: "СТАТУС",
        api: "status",
        side: "center",
    },
    {
        name: "НАГРУЗКА",
        api: "load",
        side: "center",
    },
    {
        name: "ПОЛЬЗОВАТЕЛИ",
        api: "maxUsers",
        side: "center",
    },
    {
        name: "ПИНГ",
        api: "ping",
        side: "center",
    },
    {
        name: "UPTIME",
        api: "uptime",
        side: "center",
    },
];

export default function TableVpnServers() {
    const [windowOpen, setWindowOpen] = useState(false);
    const {
        servers,
        filters,
        totalPages,
        totalElements,
        error,
        fetchServers,
        isOpen,
        setIsOpen,
        setIsChangeOpen,
    } = useServersStore();
    const [currentField, currentDir] = (filters.sort || "id,asc").split(
        ",",
    ) as [SortServerFields, SortDirection];

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (!(event.target instanceof Element)) return;

            const isEllipsis = event.target.closest(`.${styles.ellipsis}`);
            const isModal = event.target.closest(`.${styles.vpnserversModal}`);

            if (!isEllipsis && !isModal) {
                setWindowOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () =>
            document.removeEventListener("mousedown", handleClickOutside);
    }, [setWindowOpen]);

    const handleSort = (title: SortServerFields) => {
        let newSort: SortServerData;

        if (currentField !== title) {
            newSort = `${title},asc`;
        } else if (currentDir === "asc") {
            newSort = `${title},desc`;
        } else {
            newSort = "id,asc";
        }

        void fetchServers({ sort: newSort });
    };

    const handleDeleteServer = async () => {
        try {
            await ServerService.deleteServer(isOpen);
            await fetchServers({});
        } catch (error) {
            console.error(error);
        }
    };

    const getCountryName = (code: string) => {
        try {
            const regionNames = new Intl.DisplayNames(["ru"], {
                type: "region",
            });
            return regionNames.of(code.toUpperCase());
        } catch (error) {
            console.error(error);
            return code;
        }
    };

    const Flag = ({ code }: { code: string }) => (
        <img
            src={`https://purecatamphetamine.github.io/country-flag-icons/3x2/${code.toUpperCase()}.svg`}
            width="24"
            alt={code}
            style={{ display: "inline-block", verticalAlign: "middle", borderRadius: "5px" }}
        />
    );

    return (
        <div className={styles.vpnserversTable}>
            <table className={`${styles.tableServers} PC`}>
                <tbody>
                    {totalElements > 0 && (
                        <tr className={styles.trTitle}>
                            {titles.map((title) => (
                                <td key={title.api}>
                                    <div
                                        className={`${styles.tableTitle}`}
                                        onClick={() => handleSort(title.api)}
                                        style={{ justifyContent: title.side }}
                                    >
                                        <span>{title.name}</span>
                                        <div className={styles.chevron}>
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
                        servers.map((server) => (
                            <tr key={server.id}>
                                <td>
                                    <div className={styles.serverName}>
                                        <div className={styles.planet}>
                                            <Flag code={server.location} />
                                        </div>
                                        <div className={styles.info}>
                                            <span className={styles.name}>
                                                {server.name}
                                            </span>
                                            <span className={styles.ipLocation}>
                                                {server.ip} • {server.location}{" "}
                                                {getCountryName(
                                                    server.location,
                                                )}
                                            </span>
                                        </div>
                                    </div>
                                </td>
                                <td className={styles.center}>
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
                                <td className={styles.center}>
                                    <div className={styles.progressContainer}>
                                        <span className={styles.progressLabel}>
                                            {server.load}%
                                        </span>
                                        <div className={styles.progressBar}>
                                            <div
                                                className={`${styles.progressBarFill} ${styles.bar}`}
                                                style={{
                                                    width: `${server.load}%`,
                                                }}
                                            ></div>
                                        </div>
                                    </div>
                                </td>
                                <td>
                                    <div className={styles.users}>
                                        <span className={styles.usage}>
                                            {server.usage === null
                                                ? `0`
                                                : `${server.usage}`}
                                        </span>
                                        / {server.maxUsers}
                                    </div>
                                </td>
                                <td className={styles.center}>
                                    <span className={styles.ping}>
                                        {server.ping}
                                    </span>
                                </td>
                                <td className={styles.center}>
                                    <div className={styles.uptime}>
                                        <span>{server.uptime}%</span>
                                    </div>
                                </td>
                                <td className={styles.end}>
                                    <EllipsisVertical
                                        className={styles.ellipsis}
                                        size={16}
                                        onClick={() => {
                                            setWindowOpen(true);
                                            setIsOpen(server.id);
                                        }}
                                    />
                                    {windowOpen && isOpen === server.id && (
                                        <div className={styles.vpnserversModal}>
                                            <div
                                                className={styles.settings}
                                                onClick={() => {
                                                    setIsOpen(server.id);
                                                    setIsChangeOpen(true);
                                                }}
                                            >
                                                Настройки
                                            </div>
                                            <div
                                                className={styles.delete}
                                                onClick={() =>
                                                    handleDeleteServer()
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
            <div className={`${styles.listServers} Mobile`}>
                {servers.map((server) => (
                    <div key={server.id} className={styles.server}>
                        <div className={styles.serverName}>
                            <div className={styles.planet}>
                                <Flag code={server.location} />
                            </div>
                            <div className={styles.nameIp}>
                                <span className={styles.name}>
                                    {server.name}
                                </span>
                                <span className={styles.ipLocation}>
                                    {server.ip}
                                </span>
                            </div>
                        </div>
                        <div className={styles.info}>
                            <div
                                className={`${styles.ping} ${styles.mobilePing}`}
                            >
                                <span>{server.ping}</span>
                            </div>
                            <div>
                                <EllipsisVertical
                                    className={styles.ellipsis}
                                    size={16}
                                    onClick={() => {
                                        setWindowOpen(true);
                                        setIsOpen(server.id);
                                    }}
                                />
                                {windowOpen && isOpen === server.id && (
                                    <div className={styles.vpnserversModal}>
                                        <div
                                            className={styles.settings}
                                            onClick={() => {
                                                setIsOpen(server.id);
                                                setIsChangeOpen(true);
                                            }}
                                        >
                                            Настройки
                                        </div>
                                        <div
                                            className={styles.delete}
                                            onClick={() => handleDeleteServer()}
                                        >
                                            Удалить
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                ))}
            </div>
            {totalPages > 0 && (
                <Pagination
                    filters={filters as GetServerData}
                    fetchData={fetchServers}
                    totalPages={totalPages}
                    totalElements={totalElements}
                />
            )}
            {totalElements === 0 && (
                <div className={styles.trMessage}>
                    <div className={styles.tableMessage}>
                        {error ? (
                            <span className={styles.error}>
                                Ошибка {error.statusCode}: {error.message}
                            </span>
                        ) : (
                            <span>Сервера не найдены</span>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}