export type SortUserFields =
    | "id"
    | "fullName"
    | "subEndDate"
    | "role"
    | "subStatus"
    | "tariffName";

export type SortServerFields =
    | "id"
    | "name"
    | "status"
    | "load"
    | "maxUsers"
    | "ping"
    | "uptime";

export type SortPromocodeFields =
    | "id"
    | "code"
    | "tariffName"
    | "usage"
    | "status"
    | "expirationDate";
export type SortDirection = "asc" | "desc";

export type SortUserData = `${SortUserFields},${SortDirection}`;
export type SortServerData = `${SortServerFields},${SortDirection}`;
export type SortPromocodeData = `${SortPromocodeFields},${SortDirection}`;

export interface BasePaginationParams {
    page: number;
    size: number;
}

export interface GetUserData {
    page: number;
    size: number;
    sort: SortUserData;
    role?: string;
    subStatus?: string;
    search?: string;
}

export interface GetServerData {
    page: number;
    size: number;
    sort: SortServerData;
    status?: string;
    search?: string;
}

export interface GetPromocodeData {
    page: number;
    size: number;
    sort: SortPromocodeData;
    status?: string;
    search?: string;
}

export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

export interface TitlesUser {
    name: string;
    api: SortUserFields;
    side: "flex-end" | "flex-start" | "center";
}

export interface TitlesServer {
    name: string;
    api: SortServerFields;
    side: "flex-end" | "flex-start" | "center";
}

export interface TitlesPromocode {
    name: string;
    api: SortPromocodeFields;
    side: "flex-end" | "flex-start" | "center";
}