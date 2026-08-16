import apiClient from "../index";
import type { ApiResponse } from "../types";
import type {
    UserCreate,
    UserGet,
    UsersGet,
    UserUpdate,
    UserServerResponse,
    UserRole,
    UserStatus,
    AllUsers,
} from "@/types/user.ts";
import type {GetUserData, PageResponse } from "@/types/general.ts";

const url: string = "/users";

export const UserService = {
    async getUsers(getData: Partial<GetUserData>): Promise<PageResponse<UsersGet>> {
        const response = await apiClient.get<PageResponse<UsersGet>>(url, {
            params: getData,
        });
        return response.data;
    },

    async getUserById(id: number): Promise<UserGet> {
        const response = await apiClient.get<UserServerResponse>(
            url + `/${id}`,
        );
        const raw = response.data;
        return {
            fullName: raw.fullName,
            userTelegramId: raw.userTelegramId,
            role: raw.role as UserRole,
            startDate: raw.subscription?.startDate || "",
            subscriptionEndDate: raw.subscription?.endDate || "",
            subscriptionStatus:
                (raw.subscription?.status as UserStatus) || "CANCELED",
            tariffName: raw.subscription?.tariff?.name || "Нет тарифа",
        };
    },

    async getAllUsers() {
        const response = await apiClient.get<AllUsers[]>(url + "/list")
        return response.data;
    },

    async createUser(userData: Partial<UserCreate>): Promise<UserCreate> {
        const response = await apiClient.post<ApiResponse<UserCreate>>(
            url,
            userData,
        );
        return response.data.content;
    },

    async updateUser(id: number, userData: Partial<UserUpdate>): Promise<void> {
        await apiClient.put<ApiResponse<UserUpdate>>(url + `/${id}`, userData);
    },

    async deleteUser(id: number): Promise<void> {
        await apiClient.delete<ApiResponse<UserGet>>(url + `/${id}`);
    },

    async getCodeUser(id: number): Promise<string> {
        const response = await apiClient.get<string>(
            url + `/${id}/code`,
        );
        return response.data;
    },
};
