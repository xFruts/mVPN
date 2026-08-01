import apiClient from "../index";
import type { PromocodePut, PromocodesGet } from "@/types/promocodes.ts";
import type { GetPromocodeData, PageResponse } from "@/types/general.ts";

const url: string = "/promocodes";

export const PromocodeService = {
    async getPromocodes(getData: GetPromocodeData) {
        const response = await apiClient.get<PageResponse<PromocodesGet>>(url, {
            params: getData,
        });
        return response.data;
    },

    async createPromocodes(data: PromocodePut) {
        await apiClient.post(url, data)
    },

    async deletePromocodes(id: number) {
        await apiClient.delete(url + `/${id}`)
    }
}
