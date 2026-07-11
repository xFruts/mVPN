import { type UserStatus } from "@/types/user.ts";

export interface SubscriptionCreate {
    startDate: string;
    status: UserStatus;
    tariff_id: number;
}

export interface SubscriptionResponse {
    startDate: string;
    endDate: string;
    status: UserStatus;
    tariff?: {
        name: string;
    };
}

export interface SubscriptionsGet {
    startDate: string;
    endDate: string;
    status: UserStatus;
    name: string;
}