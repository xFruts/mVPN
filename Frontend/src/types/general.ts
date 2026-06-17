export type SortFields =
    | "id"
    | "fullName"
    | "subEndDate"
    | "role"
    | "subStatus"
    | "tariffName";
export type SortDirection = "asc" | "desc";

export type SortData = `${SortFields},${SortDirection}`;

export interface GetData {
    page: number;
    size: number;
    sort: SortData;
    role?: string;
    subStatus?: string;
    search?: string;
}

export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}