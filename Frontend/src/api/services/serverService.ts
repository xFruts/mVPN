import apiClient from "../index";
import type { ApiResponse } from "../types";
import type { Server } from "@/types/server.ts";

const url: string = "/servers";

export const ServerService = {
    async getServers(): Promise<Server[]> {
        const response = await apiClient.get<ApiResponse<Server[]>>(url);
        return response.data.data;
    },

    async getServerById(id: number): Promise<Server> {
        const response = await apiClient.get<ApiResponse<Server>>(
            url + `/${id}`,
        );
        return response.data.data;
    },

    async createServer(serverData: Partial<Server>): Promise<Server> {
        const response = await apiClient.post<ApiResponse<Server>>(
            url,
            serverData,
        );
        return response.data.data;
    },
};
