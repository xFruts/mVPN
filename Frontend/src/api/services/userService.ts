import apiClient from "../index";
import type { ApiResponse } from "../types";
import type { User } from "@/types/user.ts";

const url: string = "/users";

export const UserService = {
    async getUsers(): Promise<User[]> {
        const response = await apiClient.get<ApiResponse<User[]>>(url);
        return response.data.data;
    },

    async getUserById(id: number): Promise<User> {
        const response = await apiClient.get<ApiResponse<User>>(url + `/${id}`);
        return response.data.data;
    },

    async createUser(userData: Partial<User>): Promise<User> {
        const response = await apiClient.post<ApiResponse<User>>(url, userData);
        return response.data.data;
    },
};
