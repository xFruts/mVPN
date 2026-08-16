import styles from "./TablePromocodes.module.css"
import { usePromocodesStore } from "@store/usePromocodesStore.ts";
import {
    ChevronDown,
    ChevronsUpDown,
    ChevronUp,
    Copy,
    Trash,
} from "lucide-react";
import { PROMOCODE_STATUS } from "@/constant"
import type {
    GetPromocodeData,
    SortDirection,
    SortPromocodeData,
    SortPromocodeFields,
    TitlesPromocode,
} from "@/types/general.ts";
import { PromocodeService } from "@api/services/promocodeService.ts";
import { Pagination } from "@pages/shared/pagination/Pagination.tsx";

const TitlePromocodes: TitlesPromocode[] = [
    {
        name: "КОД",
        api: "code",
        side: "flex-start",
    },
    {
        name: "ТАРИФ",
        api: "tariffName",
        side: "flex-start",
    },
    {
        name: "ИСПОЛЬЗОВАНИЯ",
        api: "usage",
        side: "center",
    },
    {
        name: "СТАТУС",
        api: "status",
        side: "center",
    },
    {
        name: "ИСТЕКАЕТ",
        api: "expirationDate",
        side: "flex-end",
    }
];

export default function TablePromocodes(){
    const { promocodes, fetchPromocodes, totalPages, totalElements, filters, setIsInitialized } = usePromocodesStore();
    const [currentField, currentDir] = (filters.sort || "id,asc").split(
        ",",
    ) as [SortPromocodeFields, SortDirection];

    const handleCopy = async (text: string) => {
        try {
            await navigator.clipboard.writeText(text);
        }
        catch (e) {
            console.error(e);
        }
    }

    const handleSort = (title: SortPromocodeFields) => {
        let newSort: SortPromocodeData;

        if (currentField !== title) {
            newSort = `${title},asc`;
        } else if (currentDir === "asc") {
            newSort = `${title},desc`;
        } else {
            newSort = "id,asc";
        }

        void fetchPromocodes({ sort: newSort });
    };

    const handleDelete = async (id: number) => {
        try {
            setIsInitialized(false);
            await PromocodeService.deletePromocodes(id)
        }
        catch (e) {
            console.error(e);
        }
        void fetchPromocodes({});
    }

    return (
        <div className={styles.tablePromocodes}>
            <div className={styles.tableFilters}>
                <div className={styles.input}>
                    <input
                        type={"text"}
                        placeholder={"Поиск по коду..."}
                        onChange={(e) =>
                            fetchPromocodes({ search: e.target.value })
                        }
                    />
                </div>
                <div className={styles.input}>
                    <select
                        onChange={(e) =>
                            fetchPromocodes({ status: e.target.value })
                        }
                    >
                        <option value={""}>Все статусы</option>
                        {PROMOCODE_STATUS.map((status, index) => (
                            <option key={index} value={status}>
                                {status}
                            </option>
                        ))}
                    </select>
                </div>
            </div>
            {totalElements > 0 && (
                <>
                    <table className={`${styles.tableContent} PC`}>
                        <thead>
                            <tr className={styles.tableHeader}>
                                {TitlePromocodes.map((title) => (
                                    <td key={title.name}>
                                        <div
                                            className={styles.headerText}
                                            onClick={() =>
                                                handleSort(title.api)
                                            }
                                            style={{
                                                justifyContent: title.side,
                                            }}
                                        >
                                            <span>{title.name}</span>
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
                                        </div>
                                    </td>
                                ))}
                                <td>
                                    <div className={styles.headerText}> </div>
                                </td>
                            </tr>
                        </thead>
                        <tbody>
                            {promocodes.map((promocode) => (
                                <tr key={promocode.id}>
                                    <td className={styles.codeName}>
                                        <span className={styles.name}>
                                            {promocode.code}
                                        </span>
                                        <Copy
                                            size={16}
                                            className={styles.copy}
                                            onClick={() =>
                                                handleCopy(promocode.code)
                                            }
                                        />
                                    </td>
                                    <td className={styles.tariffName}>
                                        {promocode.tariff.name}
                                    </td>
                                    <td className={styles.usage}>
                                        {promocode.usage} /{" "}
                                        {promocode.usageLimit}
                                    </td>
                                    <td style={{ textAlign: "center" }}>
                                        <div
                                            className={`${styles.status} ${styles[promocode.status as keyof typeof styles]}`}
                                        >
                                            <span>{promocode.status}</span>
                                        </div>
                                    </td>
                                    <td className={styles.date}>
                                        {promocode.expirationDate
                                            ?.toString()
                                            .slice(0, 10)}
                                    </td>
                                    <td style={{ textAlign: "right" }}>
                                        <Trash
                                            size={16}
                                            className={styles.trash}
                                            onClick={() =>
                                                handleDelete(promocode.id)
                                            }
                                        />
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                    <div className={`Mobile ${styles.mobileTable}`}>
                        {promocodes.map((promocode) => (
                            <div key={promocode.id} className={styles.mobileTd}>
                                <div className={styles.codeName}>
                                    <span className={styles.name}>
                                        {promocode.code}
                                    </span>
                                    <Copy
                                        size={16}
                                        className={styles.copy}
                                        onClick={() =>
                                            handleCopy(promocode.code)
                                        }
                                    />
                                </div>
                                <div className={styles.statusTrash}>
                                    <div
                                        className={`${styles.status} ${styles[promocode.status as keyof typeof styles]}`}
                                    >
                                        <span>{promocode.status}</span>
                                    </div>
                                    <Trash
                                        size={16}
                                        className={styles.trash}
                                        onClick={() =>
                                            handleDelete(promocode.id)
                                        }
                                    />
                                </div>
                            </div>
                        ))}
                    </div>
                </>
            )}
            {totalElements > 0 && (
                <Pagination
                    filters={filters as GetPromocodeData}
                    fetchData={fetchPromocodes}
                    totalElements={totalElements}
                    totalPages={totalPages}
                />
            )}
            {totalElements === 0 && (
                <div className={styles.trMessage}>
                    <div className={styles.tableMessage}>
                        <span>Промокоды не найдены</span>
                    </div>
                </div>
            )}
        </div>
    );
}