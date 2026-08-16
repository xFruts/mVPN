import styles from "./PayingTransactions.module.css"
import {
    X,
    Check,
    Search,
    Wallet,
    ChevronDown,
    ChevronUp,
    ChevronsUpDown,
} from "lucide-react";
import { usePayingStore } from "@store/usePayingStore.ts";
import { PayingService } from "@api/services/payingService.ts";
import { PAYING_STATUS } from "@/constant.ts";
import type {
    SortDirection,
    SortPayingData,
    SortPayingFields,
    TitlesPaying,
} from "@/types/general.ts";
import { useState } from "react";
import type { PayingsGet } from "@/types/paying.ts";

const Titles: TitlesPaying[] = [
    {
        name: "СУММА",
        api: "paidAmount",
    },
    {
        name: "ПЛАТЕЛЬЩИК",
        api: "payerFullName",
    },
    {
        name: "ОПЛАЧЕНО ДО",
        api: "paidUntilDate",
    },
    {
        name: "КОММЕНТАРИИ",
        api: "id",
    },
    {
        name: "СТАТУС",
        api: "status",
    },
    {
        name: "ДЕЙСТВИЯ",
        api: "id",
    },
];

export default function PayingTransactions() {
    const { payings, fetchPaying, fetchStats, pendingCount, pendingAmount, filters } =
        usePayingStore();
    const [currentField, currentDir] = (filters.sort || "id,asc").split(
        ",",
    ) as [SortPayingFields, SortDirection];
    const [isOpen, setIsOpen] = useState<PayingsGet | undefined>(undefined);

    const formatDate = (dateString: string | Date) => {
        if (!dateString) return "";

        return new Intl.DateTimeFormat("ru-RU", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
        }).format(new Date(dateString));
    };

    const handleSort = (title: SortPayingFields) => {
        let newSort: SortPayingData;

        if (currentField !== title) {
            newSort = `${title},asc`;
        } else if (currentDir === "asc") {
            newSort = `${title},desc`;
        } else {
            newSort = "id,asc";
        }

        void fetchPaying({ sort: newSort });
    };

    const handleCross = async (id: number, type: string) => {
        try {
            if (type === "reject") {
                await PayingService.rejectPaying(id)
            }
            else {
                await PayingService.approvePaying(id)
            }
            void fetchPaying({});
            void fetchStats();
        }
        catch (error) {
            console.error(error);
        }
    }

    return (
        <div className={styles.transactions}>
            <div className={styles.transactionsInfo}>
                <div className={styles.info}>
                    <div className={styles.icon}>
                        <Check size={24} />
                    </div>
                    <div className={styles.infoText}>
                        <span className={styles.infoHeader}>
                            СУММА ОЖИДАЮЩИХ
                        </span>
                        <span
                            className={`${styles.infoContent} ${styles.money}`}
                        >
                            {pendingAmount} ₽
                        </span>
                        <span className={styles.infoFooter}>За всё время</span>
                    </div>
                </div>
                <div className={styles.info}>
                    <div className={styles.icon}>
                        <Wallet size={24} />
                    </div>
                    <div className={styles.infoText}>
                        <span className={styles.infoHeader}>
                            ОЖИДАЮТ ПРОВЕРКИ
                        </span>
                        <span
                            className={`${styles.infoContent} ${styles.count}`}
                        >
                            {pendingCount} шт.
                        </span>
                        <span className={styles.infoFooter}>
                            Требуют внимания администратора
                        </span>
                    </div>
                </div>
            </div>
            <div className={styles.tableContent}>
                <div className={styles.filters}>
                    <div className={styles.input}>
                        <Search size={16} color={"#9ca3af"} />
                        <input
                            type={"text"}
                            placeholder={"Поиск по ФИО..."}
                            onChange={(e) =>
                                fetchPaying({ fullName: e.target.value })
                            }
                        />
                    </div>
                    <div className={`input ${styles.padding}`}>
                        <select
                            onChange={(e) =>
                                fetchPaying({ status: e.target.value })
                            }
                        >
                            <option value={""}>Все статусы</option>
                            {PAYING_STATUS.map((status) => (
                                <option key={status} value={status}>
                                    {status}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="input">
                        <input
                            type={"date"}
                            onChange={(e) => {
                                const dateVal = e.target.value;
                                const isoDate = new Date(dateVal).toISOString();
                                void fetchPaying({ createdFrom: isoDate });
                            }}
                        />
                    </div>
                    <div className="input">
                        <input
                            type={"date"}
                            onChange={(e) => {
                                const dateVal = e.target.value;
                                const isoDate = new Date(dateVal).toISOString();
                                void fetchPaying({ createdTo: isoDate });
                            }}
                        />
                    </div>
                </div>
                <table className={`${styles.table} PC`}>
                    <thead>
                        <tr>
                            {Titles.map((title) => (
                                <th key={title.name} className={styles.thTitle}>
                                    <div
                                        className={`${styles.divTitle} ${title.api !== "id" ? `${styles.hover}` : ""}`}
                                        onClick={() => {
                                            if (title.api !== "id")
                                                handleSort(title.api);
                                        }}
                                        style={{
                                            justifyContent:
                                                title.name === "ДЕЙСТВИЯ"
                                                    ? "right"
                                                    : "left",
                                            cursor:
                                                title.api === "id"
                                                    ? "default"
                                                    : "pointer",
                                        }}
                                    >
                                        <span>{title.name}</span>
                                        {title.api !== "id" && (
                                            <div className={styles.chevron}>
                                                {currentField === title.api ? (
                                                    currentDir === "asc" ? (
                                                        <ChevronDown
                                                            size={12}
                                                        />
                                                    ) : currentDir ===
                                                      "desc" ? (
                                                        <ChevronUp size={12} />
                                                    ) : (
                                                        <ChevronsUpDown
                                                            size={12}
                                                        />
                                                    )
                                                ) : (
                                                    <ChevronsUpDown size={12} />
                                                )}
                                            </div>
                                        )}
                                    </div>
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody className={styles.tbody}>
                        {payings.map((paying) => (
                            <tr key={paying.id} className={styles.tr}>
                                <td>
                                    <div className={styles.two}>
                                        <span className={styles.Count}>
                                            {paying.paidAmount} ₽
                                        </span>
                                        <span className={styles.Date}>
                                            {formatDate(paying.createdAt)}
                                        </span>
                                    </div>
                                </td>
                                <td>
                                    <div className={styles.two}>
                                        <span className={styles.Name}>
                                            {paying.payerFullName}
                                        </span>
                                        <span className={styles.UserId}>
                                            User ID: {paying.userId}
                                        </span>
                                    </div>
                                </td>
                                <td>
                                    <span className={styles.Name}>
                                        {paying.paidUntilDate}
                                    </span>
                                </td>
                                <td>
                                    <div className={styles.two}>
                                        <span className={styles.userComment}>
                                            Юзер: {paying.userComment || "—"}
                                        </span>
                                        {paying.adminComment && (
                                            <span
                                                className={styles.adminComment}
                                            >
                                                Админ: {paying.adminComment}
                                            </span>
                                        )}
                                    </div>
                                </td>
                                <td>
                                    <div
                                        className={`${styles[paying.status]} ${styles.status}`}
                                    >
                                        <span>{paying.status}</span>
                                    </div>
                                </td>
                                <td>
                                    {paying.status === "PENDING" && (
                                        <div className={styles.acting}>
                                            <div
                                                className={styles.cross}
                                                onClick={() =>
                                                    handleCross(
                                                        paying.id,
                                                        "reject",
                                                    )
                                                }
                                            >
                                                <X size={16} />
                                            </div>
                                            <div
                                                className={styles.check}
                                                onClick={() =>
                                                    handleCross(
                                                        paying.id,
                                                        "approve",
                                                    )
                                                }
                                            >
                                                <Check size={16} />
                                            </div>
                                        </div>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                <div className={`${styles.tableMobile} Mobile`}>
                    {payings.map((paying) => (
                        <div
                            key={paying.id}
                            className={styles.mobileLine}
                            onClick={() => setIsOpen(paying)}
                        >
                            <div className={styles.mobileInfo}>
                                <div className={styles.priceStatus}>
                                    <span className={styles.Count}>
                                        {paying.paidAmount} ₽
                                    </span>
                                    <div
                                        className={`${styles[paying.status]} ${styles.status}`}
                                    >
                                        <span>{paying.status}</span>
                                    </div>
                                </div>
                                <span className={styles.Name}>
                                    {paying.payerFullName}
                                </span>
                            </div>
                            {paying.status === "PENDING" && (
                                <div className={styles.acting}>
                                    <div
                                        className={styles.cross}
                                        onClick={() =>
                                            handleCross(paying.id, "reject")
                                        }
                                    >
                                        <X size={16} />
                                    </div>
                                    <div
                                        className={styles.check}
                                        onClick={() =>
                                            handleCross(paying.id, "approve")
                                        }
                                    >
                                        <Check size={16} />
                                    </div>
                                </div>
                            )}
                        </div>
                    ))}
                </div>
                {isOpen && (
                    <div
                        className={"overlay"}
                        onClick={() => {
                            setIsOpen(undefined);
                        }}
                    >
                        <div
                            className={styles.modal}
                            onClick={(e) => e.stopPropagation()}
                        >
                            <div className={styles.modalHeader}>
                                <span>Детали платежа</span>
                                <div
                                    className={styles.close}
                                    onClick={() => {
                                        setIsOpen(undefined);
                                    }}
                                >
                                    <X size={20} />
                                </div>
                            </div>
                            <div className={styles.modalContent}>
                                <div className={styles.infoSpaceBetween}>
                                    <span className={styles.header}>СУММА</span>
                                    <span className={styles.Count}>
                                        {isOpen.paidAmount} ₽
                                    </span>
                                </div>
                                <div className={styles.infoSpaceBetween}>
                                    <span className={styles.header}>
                                        СТАТУС
                                    </span>
                                    <div
                                        className={`${styles[isOpen.status]} ${styles.status}`}
                                    >
                                        <span>{isOpen.status}</span>
                                    </div>
                                </div>
                                <div className={styles.infoColumn}>
                                    <span className={styles.header}>
                                        ПЛАТЕЛЬЩИК
                                    </span>
                                    <span className={styles.Name}>
                                        {isOpen.payerFullName}
                                    </span>
                                    <span className={styles.UserId}>
                                        ID: {isOpen.userId}
                                    </span>
                                </div>
                                <div className={styles.infoColumn}>
                                    <span className={styles.header}>
                                        СОЗДАНО
                                    </span>
                                    <span className={styles.Name}>
                                        {formatDate(isOpen.createdAt)}
                                    </span>
                                </div>
                                <div className={styles.infoColumn}>
                                    <span className={styles.header}>
                                        ОПЛАЧЕНО ДО
                                    </span>
                                    <span className={styles.Name}>
                                        {isOpen.paidUntilDate}
                                    </span>
                                </div>
                                <div className={styles.infoColumn}>
                                    <span className={styles.header}>
                                        КОММЕНТАРИЙ ЮЗЕРА
                                    </span>
                                    <span className={styles.userComment}>
                                        Юзер: {isOpen.userComment || "—"}
                                    </span>
                                    {isOpen.adminComment && (
                                        <>
                                            <span
                                                className={styles.header}
                                            >КОММЕНТАРИЙ АДМИНА</span>
                                            <span
                                                className={styles.adminComment}
                                            >
                                                Админ: {isOpen.adminComment}
                                            </span>
                                        </>
                                    )}
                                </div>
                            </div>
                            <button className={styles.buttonClose} onClick={()=>setIsOpen(undefined)}>Закрыть</button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}