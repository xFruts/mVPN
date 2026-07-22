import apiClient from "../index";
import type {Servers, ServerGet, ServerPut} from "@/types/server.ts";
import type {
    GetServerData,
    PageResponse,
} from "@/types/general.ts";

const url: string = "/servers";

export const ServerService = {
    async getServers(
        getData: Partial<GetServerData>,
    ): Promise<PageResponse<Servers>> {
        const response = await apiClient.get<
            PageResponse<Servers>
        >(url, {
            params: getData,
        });
        return response.data;
    },

    async getServerById(id: number): Promise<ServerGet> {
        const response = await apiClient.get<ServerGet>(url + `/${id}`);
        return response.data;
    },

    async createServer(serverData: Partial<ServerPut>): Promise<void> {
        await apiClient.post<ServerPut>(url, serverData);
    },

    async updateServer(
        id: number,
        serverData: Partial<ServerPut>,
    ): Promise<void> {
        await apiClient.put<ServerPut>(url + `/${id}`, serverData);
    },

    async deleteServer(id: number): Promise<void> {
        await apiClient.delete<ServerPut>(url + `/${id}`);
    },

    async updateStatusServer(id: number, status: string): Promise<void> {
        await apiClient.patch<ServerPut>(url + `/${id}/status`, status);
    },

    async loadSshKey(file: File): Promise<string> {
        const formData = new FormData();
        formData.append("file", file);

        const response = await apiClient.post<{ objectKey: string }>(
            url + "/ssh-keys",
            formData,
            {
                headers: {
                    "Content-Type": "multipart/form-data",
                },
            },
        );

        return response.data.objectKey;
    },
};
