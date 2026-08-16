import { create } from "zustand";
import { immer } from "zustand/middleware/immer";
import type { UsersGet } from "@typings/user";
import { UserService } from "../api/services/userService.ts";
import type { GetUserData } from "@/types/general.ts";
import type { ApiErrorType } from "@api/types.ts";
import { createDataFetcher } from "@pages/shared/setParams.ts";

interface UsersState {
    users: UsersGet[];
    totalElements: number;
    totalPages: number;
    isInitialized: boolean;
    isLoading: boolean;
    error: ApiErrorType | null;
    filters: GetUserData;
    fetchUsers: (newParams: Partial<GetUserData>) => Promise<void>;
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
        fetchUsers: createDataFetcher(
            UserService.getUsers,
            get,
            set,
            "users",
        ),
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
