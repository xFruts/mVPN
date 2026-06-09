import apiClient from "../index";
import type { ServerPut } from "@/types/server.ts";

const url: string = "/subscriptions";

export const SubscriptionService = {
    async extendSubscription(listId: number[]): Promise<void> {
        await apiClient.post<ServerPut>(url + "/extend", {
            userIds: listId,
        });
    },
};