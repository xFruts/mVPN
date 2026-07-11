import apiClient from "@/api";
import type { PageResponse } from "@/types/general.ts";
import type { TariffsGet } from "@/types/tariff.ts";

const url: string = "/tariffs";

export const TariffService = {
    async getTariffs() {
        const response = await apiClient.get<PageResponse<TariffsGet>>(url);
        return response.data;
    }
}