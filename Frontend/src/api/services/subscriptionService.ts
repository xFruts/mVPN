import apiClient from "../index";
import type { ServerPut } from "@/types/server.ts";
import type {
    SubscriptionCreate,
    SubscriptionResponse,
    SubscriptionsGet,
} from "@/types/subscription.ts";

const url: string = "/subscriptions";

export const SubscriptionService = {
    async extendSubscription(listId: number[]): Promise<void> {
        await apiClient.post<ServerPut>(url + "/extend", {
            userIds: listId,
        });
    },

    async addSubscription(
        sub: SubscriptionCreate,
        userId: number,
    ): Promise<void> {
        await apiClient.post<SubscriptionCreate>(url + `/user/` + userId, sub);
    },

    async getAllSubscriptions(userId: number): Promise<SubscriptionsGet[]> {
        const response = await apiClient.get<SubscriptionResponse[]>(
            url + `/user/` + userId,
        );
        const raw = response.data;
        return raw.map((item) => {
            return {
                startDate: item.startDate,
                endDate: item.endDate,
                status: item.status,
                name: item.tariff?.name || "Нет тарифа",
            };
        });
    },
};