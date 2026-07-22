import { create } from "zustand";
import { immer } from "zustand/middleware/immer";
import type { UsersGet } from "@typings/user";
import { UserService } from "../api/services/userService.ts";
import type {GetUserData} from "@/types/general.ts";
import type { ApiError } from "@api/types.ts";

interface UsersState {
    users: UsersGet[];
    totalElements: number;
    totalPages: number;
    isInitialized: boolean;
    isLoading: boolean;
    error: ApiError | null;
    filters: Partial<GetUserData>;
    setParams: (params: Partial<GetUserData>) => void;
    fetchUsers: (params?: Partial<GetUserData>) => Promise<void>;
    selectedUser: { id: number } | null;
    selectedIds: number[];
    toggleSelectId: (id: number) => void;
    toggleAll: () => void;
    clearSelection: () => void;
    isOpen: number;
    isConfiguration: boolean;
    setIsConfiguration: () => void;
    selectedConfigUserId: number | null;
    setConfiguration: (id: number | null) => void;
    toggleMenu: (id: number) => void;
    resetMenu: () => void;
}

const useUsersStore = create<UsersState>()(
    immer((set, get) => ({
        users: [],
        totalElements: 0,
        totalPages: 0,
        isInitialized: false,
        isLoading: false,
        error: null,
        filters: {
            page: 0,
            size: 20,
            sort: "id,asc",
        },

        setParams: (newParams) => {
            set((state) => {
                const updatedFilters = { ...state.filters, ...newParams };

                if (newParams.page === undefined) {
                    updatedFilters.page = 0;
                }

                Object.keys(newParams).forEach((key) => {
                    const k = key as keyof GetUserData;
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

            get().fetchUsers();
        },
        fetchUsers: async (newParams) => {
            set({ isLoading: true });
            const currentFilters = { ...get().filters, ...newParams };
            try {
                const data = await UserService.getUsers(currentFilters);

                set((state) => {
                    state.users = data.content;
                    state.totalElements = data.totalElements;
                    state.totalPages = data.totalPages;
                    state.filters = currentFilters;
                    state.isInitialized = true;
                    state.isLoading = false;
                });
            } catch (e: unknown) {
                let status = 500;
                let message = "Произошла ошибка";

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
                    404: "Пользователи не найдены",
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
        selectedUser: null,
        selectedIds: [],

        toggleSelectId: (id) =>
            set((state) => {
                const index = state.selectedIds.indexOf(id);
                if (index > -1) {
                    state.selectedIds.splice(index, 1);
                } else {
                    state.selectedIds.push(id);
                }
            }),

        toggleAll: () =>
            set((state) => {
                const allIds = state.users.map((u) => u.id);
                const isAllSelected = allIds.every((id) =>
                    state.selectedIds.includes(id),
                );

                if (isAllSelected) {
                    state.selectedIds = state.selectedIds.filter(
                        (id) => !allIds.includes(id),
                    );
                } else {
                    const newIds = allIds.filter(
                        (id) => !state.selectedIds.includes(id),
                    );
                    state.selectedIds.push(...newIds);
                }
            }),

        clearSelection: () => set({ selectedIds: [] }),
        isOpen: -1,
        isConfiguration: false,
        setIsConfiguration: () =>
            set((state) => {
                state.isConfiguration = !state.isConfiguration;
            }),
        selectedConfigUserId: null,
        setConfiguration: (id) => {
            set((state) => {
                state.isConfiguration = id !== null;
                state.selectedConfigUserId = id;
                if (id !== null) state.isOpen = -1;
            });
        },

        toggleMenu: (id) =>
            set((state) => {
                state.isOpen = state.isOpen === id ? -1 : id;
            }),
        resetMenu: () => set({ isOpen: -1, selectedUser: null }),
    })),
);

export default useUsersStore;
