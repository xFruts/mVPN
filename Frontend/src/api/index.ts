import axios from "axios";
import type { AxiosInstance } from "axios";

const BASE_URL = import.meta.env.VITE_API_URL || "https://api.example.com";

const apiClient: AxiosInstance = axios.create({
    baseURL: BASE_URL,
    timeout: 10000,
    headers: {
        "Content-Type": "application/json",
    },
});

apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem("authToken");
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
