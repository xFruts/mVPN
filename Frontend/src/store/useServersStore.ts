import { create } from "zustand";
import { immer } from "zustand/middleware/immer";
import type { Servers } from "@typings/server";
import type { GetServerData } from "@/types/general.ts";
import { ServerService } from "@api/services/serverService.ts";
import type { ApiError } from "@api/types.ts";

interface ServersState {
    servers: Servers[];
    totalElements: number;
    totalPages: number;
    isInitialized: boolean;
    isLoading: boolean;
    error: ApiError | null;
    filters: Partial<GetServerData>;
    setParams: (params: Partial<GetServerData>) => void;
    fetchServers: (params?: Partial<GetServerData>) => Promise<void>;
    isChangeOpen: boolean;
    isOpen: number;
    setIsChangeOpen: (value: boolean) => void;
    setIsOpen: (servers: number) => void;
}

const useServersStore = create<ServersState>()(
    immer((set, get) => ({
        servers: [],
        totalElements: 0,
        totalPages: 0,
        isInitialized: false,
        isLoading: false,
        error: null,
        filters: {
            page: 0,
            size: 12,
            sort: "id,asc",
        },
        setParams: (newParams) => {
            set((state) => {
                const updatedFilters = { ...state.filters, ...newParams };

                if (newParams.page === undefined) {
                    updatedFilters.page = 0;
                }

                Object.keys(newParams).forEach((key) => {
                    const k = key as keyof GetServerData;
                    if (
                        updatedFilters[k] === "" ||
                        updatedFilters[k] === null ||
                        updatedFilters[k] === undefined
                    ) {
                        delete updatedFilters[k];
                    }
                });

                state.filters = updatedFilters;
            });

            get().fetchServers();
        },
        fetchServers: async (newParams) => {
            set({ isLoading: true });
            const currentFilters = { ...get().filters, ...newParams };

            try {
                const data = await ServerService.getServers(currentFilters);

                set((state) => {
                    state.servers = data.content;
                    state.totalElements = data.totalElements;
                    state.totalPages = data.totalPages;
                    state.filters = currentFilters;
                    state.isInitialized = true;
                    state.isLoading = false;
                    state.error = null;
                });
            } catch (e: unknown) {
                let status = 500;
                let message = "Произошла ошибка при загрузке серверов";

                if (e && typeof e === "object") {
                    const err = e as Record<string, unknown>;

                    if (typeof err.statusCode === "number") {
                        status = err.statusCode;
                    } else if (
                        err.response &&
                        typeof err.response === "object"
                    ) {
                        const resp = err.response as Record<string, unknown>;
                        if (typeof resp.status === "number")
                            status = resp.status;
                    }

                    if (typeof err.message === "string") {
                        message = err.message;
                    }
                }

                const errorMap: Record<number, string> = {
                    0: "Нет подключения к интернету",
                    400: "Неверный запрос",
                    401: "Нужна авторизация",
                    403: "Доступ ограничен",
                    404: "Серверы не найдены",
                    500: "Внутренняя ошибка сервера",
                };

                const finalError: ApiError = {
                    statusCode: status,
                    message: errorMap[status] || message,
                };

                set((state) => {
                    state.error = finalError;
                    state.isInitialized = true;
                    state.isLoading = false;
                });
            }
        },
        isChangeOpen: false,
        isOpen: -1,
        setIsChangeOpen: (value: boolean) =>
            set((state) => ({
                isChangeOpen:
                    typeof value === "boolean" ? value : !state.isChangeOpen,
            })),
        setIsOpen: (id) => set({ isOpen: id }),
    })),
);

export default useServersStore;
