import type { BasePaginationParams } from "@/types/general.ts";
import styles from "./Pagination.module.css";
import { ChevronLeft, ChevronRight } from "lucide-react";

interface PaginationProps<T extends BasePaginationParams> {
    filters: T;
    setParams: (params: Partial<T>) => void;
    totalElements: number;
    totalPages: number;
}

export const Pagination = <T extends BasePaginationParams>({
    filters,
    setParams,
    totalElements,
    totalPages
}: PaginationProps<T>) => {
    const { page = 0, size = 20 } = filters;
    return (
        <div className={styles.changePage}>
            <div className={styles.size}>
                <span>
                    <span className={"PC"}>Показывать</span> по:
                </span>
                <select
                    value={filters.size}
                    onChange={(e) => {
                        setParams({
                            size: Number(e.target.value),
                        } as Partial<T>);
                    }}
                >
                    <option value={10}>10</option>
                    <option value={20}>20</option>
                    <option value={50}>50</option>
                    <option value={100}>100</option>
                </select>
                <span>
                    ({page * size + 1} -{" "}
                    {(page + 1) * size > totalElements
                        ? totalElements
                        : (page + 1) * size}{" "}
                    из {totalElements})
                </span>
            </div>
            <div className={styles.page}>
                <button
                    className={`${styles.chevron} ${page === 0 ? styles.disable : ""}`}
                    disabled={page === 0}
                    onClick={() => setParams({ page: page - 1 } as Partial<T>)}
                >
                    <ChevronLeft size={16} />
                </button>
                <div className={styles.viewPage}>
                    {page + 1} / {totalPages}
                </div>
                <button
                    className={`${styles.chevron} ${page + 1 === totalPages ? styles.disable : ""}`}
                    disabled={page + 1 === totalPages}
                    onClick={() => setParams({ page: page + 1 } as Partial<T>)}
                >
                    <ChevronRight size={16} />
                </button>
            </div>
        </div>
    );
};
