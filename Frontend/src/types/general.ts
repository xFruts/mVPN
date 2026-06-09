export type SortFields =
    | "id"
    | "fullName"
    | "endDate"
    | "role"
    | "subscriptionStatus"
    | "tariff";
export type SortDirection = "asc" | "desc";

export type SortData = `${SortFields}`;

export interface GetData {
    page: number;
    size: number;
    sort: SortData;
}

export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}