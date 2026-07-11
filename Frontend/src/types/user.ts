export type UserRole = "ADMIN" | "SPECIAL" | "REGULAR"
export type UserStatus = "ACTIVE" | "EXPIRED" | "CANCELLED"

export interface UsersGet {
    id: number;
    fullName: string;
    role: UserRole;
    tariffName: string | null;
    startDate: string | null;
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
    userTelegramId: number | undefined;
    role: UserRole;
    startDate: string | undefined;
    subscriptionEndDate: string | undefined;
    subscriptionStatus: UserStatus | undefined;
    tariffName: string | undefined;
}

export interface UserUpdate {
    fullName: string;
    userTelegramId: number;
    role: UserRole;
    subscriptionStatus: UserStatus | undefined | null;
    subscriptionEndDate: string | undefined | null;
}

export interface UserCreate {
    fullName: string;
    userTelegramId: number | undefined | null;
    role: UserRole;
}