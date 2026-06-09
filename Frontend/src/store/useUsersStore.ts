import { create } from "zustand";
import { immer } from "zustand/middleware/immer";
import type { UsersGet } from "@typings/user";
import { UserService } from "../api/services/userService.ts";
import type {GetData} from "@/types/general.ts";

interface UsersState {
    users: UsersGet[];
    totalElements: number;
    totalPages: number;
    isLoading: boolean;
    filters: Partial<GetData>;
    fetchUsers: (params?: Partial<GetData>) => Promise<void>;
    selectedUser: { id: number } | null;
    selectedIds: number[];
    toggleSelectId: (id: number) => void;
    toggleAll: () => void;
    clearSelection: () => void;
    apiOpen: boolean;
    isOpen: number;
    setApiOpen: (open: boolean) => void;
    toggleMenu: (id: number) => void;
    resetMenu: () => void;
}

const useUsersStore = create<UsersState>()(
    immer((set, get) => ({
        users: [],
        totalElements: 0,
        totalPages: 0,
        isLoading: false,
        filters: {
            page: 0,
            size: 12,
            sort: "id",
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
                    state.isLoading = false;
                });
            } catch (error) {
                set({ isLoading: false });
                console.error(error);
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
        apiOpen: false,
        isOpen: -1,

        setApiOpen: (open) => set({ apiOpen: open }),
        toggleMenu: (id) =>
            set((state) => ({ isOpen: state.isOpen === id ? -1 : id })),
        resetMenu: () =>
            set({ isOpen: -1, selectedUser: null, apiOpen: false }),
    })),
);

export default useUsersStore;
