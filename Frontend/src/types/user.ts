export type UserRole = "ADMIN" | "SPECIAL" | "REGULAR"
export type UserStatus = "ACTIVE" | "EXPIRED" | "CANCELLED" | "NONE"

export interface UsersGet {
    id: number;
    fullName: string;
    role: UserRole;
    tariffName: string | null;
    subscriptionStatus: UserStatus | null;
    endDate: string | null;
}

export interface UserServerResponse {
    id: number;
    fullName: string;
    userTelegramId: number;
    role: string;
    subscription: {
        startDate: string;
        endDate: string;
        status: string;
        tariff?: {
            name: string;
        } | null;
    } | null;
}

export interface UserGet {
    fullName: string;
    userTelegramId: number;
    role: UserRole;
    startDate: string;
    subscriptionEndDate: string;
    subscriptionStatus: UserStatus;
    tariffName: string;
}

export interface UserUpdate {
    fullName: string;
    userTelegramId: number;
    role: UserRole;
    subscriptionStatus: UserStatus;
    subscriptionEndDate: string;
}

export interface UserCreate {
    fullName: string;
    userTelegramId: number;
    role: UserRole;
}