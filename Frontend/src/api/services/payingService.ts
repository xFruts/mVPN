import type { GetPayingData, PageResponse } from "@/types/general.ts";
import apiClient from "@/api";
import type { PayingsPost, PayingsSettingsGet, PayingsSettingsPost, PayingStats } from "@/types/paying.ts";

const url = "/payment"
const Verif = "-verifications"
const Sett = "-settings"

export const PayingService = {
    async getPayings(getData: GetPayingData) {
        const response = await apiClient.get<PageResponse<GetPayingData>>(
            url + Verif,
            {
                params: getData,
            },
        );
        return response.data;
    },

    async createPaying(data: PayingsPost) {
        await apiClient.post(url + Verif, data);
    },

    async statsPaying() {
        const response = await apiClient.get<PayingStats>(url + Verif + "/stats", {})
        return response.data;
    },

    async approvePaying(id: number) {
        await apiClient.post(url + Verif + `/${id}/approve`, {});
    },

    async rejectPaying(id: number) {
        await apiClient.post(url + Verif + `/${id}/reject`, {});
    },

    async getSettings(billingMonth: string) {
        const response = await apiClient.get<PayingsSettingsGet>(
            url + Sett + `/${billingMonth}`
        );
        return response.data;
    },

    async createSettings(data: PayingsSettingsPost) {
        await apiClient.post(url + Sett, data);
    },

    async updateSettings(data: PayingsSettingsPost, id: number) {
        await apiClient.put(url + Sett + `/${id}`, data);
    },
};