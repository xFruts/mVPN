import type { AxiosInstance } from "axios";
import axios from "axios";
import { API_BASE_URL } from "./../constant.ts";


const apiClient: AxiosInstance = axios.create({
    baseURL: API_BASE_URL,
    timeout: 10000,
    headers: {
        "Content-Type": "application/json",
    },
});

let getToken: () => Promise<string | undefined> | string | undefined = () =>
    undefined;

export const setTokenGetter = (fn: () => Promise<string | undefined>) => {
    getToken = fn;
};

apiClient.interceptors.request.use(
    async (config) => {
        const token = await getToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    },
);

apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        console.error("API Error:", error);

        if (error.response) {
            const { status, data } = error.response;
            const message = data?.message || error.message;

            throw new ApiError(status, message, data);
        } else if (error.request) {
            throw new ApiError(0, "No response from server", null);
        } else {
            throw new ApiError(-1, error.message, null);
        }
    },
);

class ApiError extends Error {
    statusCode: number;
    details?: unknown;

    constructor(statusCode: number, message: string, details?: unknown) {
        super(message);
        this.name = "ApiError";
        this.statusCode = statusCode;
        this.details = details;
    }
}

export default apiClient;
