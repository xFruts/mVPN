import apiClient from "@/api";
import type { PageResponse } from "@/types/general.ts";
import type { TariffsGet, TariffsPut } from "@/types/tariff.ts";

const url: string = "/tariffs";

export const TariffService = {
    async getTariffs() {
        const response = await apiClient.get<PageResponse<TariffsGet[]>>(url);
        return response.data;
    },

    async getTariffById(id: number) {
        const response = await apiClient.get<TariffsGet>(url + `/${id}`);
        const data = response.data;

        const formattedData: TariffsPut = {
            name: data.name,
            maxDevices: data.maxDevices,
            trafficLimitGb: data.trafficLimitGb,
            durationOfDays: data.durationOfDays,
            serverIds: data.serverLocation.map((loc) => loc.id),
        };

        console.log("Очищенные данные:", formattedData);
        return formattedData;
    },

    async createTariff(data: TariffsPut) {
        await apiClient.post<TariffsPut>(url, data);
    },

    async updateTariff(id: number, data: TariffsPut) {
        await apiClient.put<PageResponse<TariffsPut>>(url + `/${id}`, data);
    },

    async deleteTariff(id: number) {
        await apiClient.delete(url + `/${id}`);
    },
};